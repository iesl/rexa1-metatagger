/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Oct 12, 2004
 * authors: saunders, ghuang
 */

package org.rexo.pipeline;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.types.ArraySequence;
import edu.umass.cs.mallet.base.types.Sequence;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.rexo.extraction.CRFOutputFormatter;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.pipeline.components.RxDocument;

import java.util.*;


/**
 * Currently uses regular expressions to tag grant numbers and institutions in a document
 */
public class GrantExtractionFilter extends AbstractFilter {
	private static Logger log = Logger.getLogger( GrantExtractionFilter.class );


	public int accept(RxDocument rdoc) {
		log.info( "GrantExtractionFilter" );
		int errorCode = ReturnCode.OK;
		try {
			errorCode = doExtraction( rdoc ) ? ReturnCode.OK : ReturnCode.ABORT_PAPER;
		}
		catch (Throwable e) {
			log.warn( "grant extraction: " + e.getMessage() );
		}
		return errorCode;
	}


	// Go through the raw text of the entire document, looking for
	// sentences that begin with "supported by" and search until the
	// end of each these sentences to identify grant numbers and
	// institutions.
	private boolean doExtraction(RxDocument rdoc) {
		NewHtmlTokenization docTokenization = rdoc.getTokenization();
		Map segmentations = (Map)rdoc.getScope( "document" ).get( "segmentation" );

		if (docTokenization == null) {
			System.err.println( "Extractor found nothing to extract..." );

			getLogger( rdoc ).error( "Extractor found nothing to extract..." );
			rdoc.docErrorString( "Extractor found nothing to extract..." );
			return false;
		}

		// Ideally we only need to perform grant extraction from the header and body
		// of the document, not the bibliography section.  But due to potential mistakes
		// made by the segmentor we examine the entire document
		ArrayList sentences = segmentSentences( docTokenization );
		Object[] oa = getSupportSentences( sentences );
		ArrayList supportSentences = (ArrayList)oa[0];
		ArrayList startIndices = (ArrayList)oa[1];
		ArrayList grantsList = new ArrayList();

		for (int si = 0; si < supportSentences.size(); si++) {
			NewHtmlTokenization sentence = (NewHtmlTokenization)supportSentences.get( si );
			String formattedText = sentence.getFormattedText();
			String[] tokenStrings = formattedText.split( "\\s+" );
			Sequence grantLabels = tagSentence( tokenStrings );
			CRFOutputFormatter formatter = new CRFOutputFormatter();
			Element element = formatter.toXmlElement( tokenStrings, grantLabels, "grant-sentence" );
			// HSpans grantSpan = ReferenceExtractionFilter.makeHSpansFromTags( sentence, grantLabels, "grant-sentence" );

			grantsList.add( element );
		}

		segmentations.put( "grantList", grantsList );

		// docTokenization.setGrantsList( grantsList );
		return true;
	}


	private static HashSet notSentenceEnders = new HashSet();
	private static HashSet sentenceEndMarkers = new HashSet();

	static {
		notSentenceEnders.add( "dr" );
		notSentenceEnders.add( "mr" );
		notSentenceEnders.add( "mrs" );
		notSentenceEnders.add( "ms" );
		notSentenceEnders.add( "no" );
		notSentenceEnders.add( "nos" );
		sentenceEndMarkers.add( "." );
		sentenceEndMarkers.add( "?" );
		sentenceEndMarkers.add( "!" );
	}


	// More efficient than using a Pattern
	private static boolean canBeSentenceStart(String s) {
		if (s == null || s.length() == 0) {
			return false;
		}

		char c = s.charAt( 0 );

		// The last clause is a hack due to ps2text converting some super/subscripts to those characters
		return Character.isUpperCase( c ) || Character.isDigit( c ) ||
		       (s.equals( "+" ) || s.equals( "?" ) || s.equals( "x" ) || s.equals( "y" ));
	}


	// Return a list of NewHtmlTokenizations representing sentences
	// Basically looks for periods as sentence endings with just a few exceptions
	private static ArrayList segmentSentences(NewHtmlTokenization tokenization) {
		ArrayList ret = new ArrayList();
		int sentBeginIndex = 0;
		String parent = "";
		String child, grandChild;

		for (int ti = 0; ti < tokenization.size(); ti++) {
			String text = ((StringSpan)tokenization.getToken( ti )).getText();
			child = (ti == tokenization.size() - 1) ? "" : ((StringSpan)tokenization.getToken( ti + 1 )).getText();
			grandChild = (ti >= tokenization.size() - 2) ? "" : ((StringSpan)tokenization.getToken( ti + 2 )).getText();

			// found the end of a sentence
			if (sentenceEndMarkers.contains( text ) && !notSentenceEnders.contains( parent.toLowerCase() )
			    && canBeSentenceStart( child ) && !grandChild.equals( "." )) {

				NewHtmlTokenization sentence = tokenization.getTokenRangeTokenization( sentBeginIndex, ti + 1 );
				ret.add( sentence );
				sentBeginIndex = ti + 1;

				//print(sentence);
			}

			parent = text;
		}

		return ret;
	}


	private static HashSet evidenceForGrantSentence = new HashSet();

	static {
		evidenceForGrantSentence.add( "work" );
		evidenceForGrantSentence.add( "project" );
		evidenceForGrantSentence.add( "research" );
		evidenceForGrantSentence.add( "author" );
		evidenceForGrantSentence.add( "authors" );
		evidenceForGrantSentence.add( "partially" );
		evidenceForGrantSentence.add( "paper" );
		evidenceForGrantSentence.add( "report" );
	}


	// Returns: 
	//  1) a list of lists of tokenizations, where each is a sentence that possibly 
	//     contains acknowledgements of grant numbers and institutions
	//  2) a list of Integers that specifiy where the phrase "supported by" ends in
	//     each sentence
	private static Object[] getSupportSentences(ArrayList sentences) {
		ArrayList retSentList = new ArrayList();
		ArrayList indices = new ArrayList();

		for (int si = 0; si < sentences.size(); si++) {
			NewHtmlTokenization sent = (NewHtmlTokenization)sentences.get( si );
			boolean hasHint = false;

			for (int ti = 0; ti < sent.size(); ti++) {
				String text = ((StringSpan)sent.getToken( ti )).getText().toLowerCase();
				String child = (ti == sent.size() - 1) ? "" : ((StringSpan)sent.getToken( ti + 1 )).getText().toLowerCase();

				if (hasHint && child.equals( "by" ) && (text.equals( "supported" ) || text.equals( "funded" ) || text.equals(
						"sponsored" ))) {
					retSentList.add( sent );
					indices.add( new Integer( ti + 2 ) );
					break;
				}
				else if (evidenceForGrantSentence.contains( text )) {
					hasHint = true;
				}
			}

		}
		return new Object[]{retSentList, indices};
	}


	// Tag the sentence with grant info 
	private static Sequence tagSentence(String[] tokenStrings) {
		// Deal with the text as it appears in the original document,
		// not after converting to a tokenization since this gives 
		// us more accurate information

		// Map each tokenized token to the (space-delimited) 
		// token index as it appears in the original document
		ArrayList origTokenList = new ArrayList();

		List origLabels = tagGrantInfo( Arrays.asList( tokenStrings ), 0 );

		// Postprocess the labels for B/I labels and to remove 
		// extraneous text from institution names
		Sequence cleanedLabels = postprocessLabels( origLabels, origTokenList );

		// Convert tags for original tokens to tags for new tokens
		ArrayList labels = new ArrayList();

		for (int ti = 0; ti < tokenStrings.length; ti++) {
			labels.add( cleanedLabels.get( ti ) );
		}

		return new ArraySequence( labels );
	}


	// Returns whether the token may be a grant number
	private static boolean isGrantNumber(String token) {
		int start = 0;
		int end = token.length() - 1;
		// Skip characters that don't affect the outcome		
		while (start < token.length() && (token.charAt( start ) == '(' || token.charAt( start ) == '#')) {
			start++;
		}
		while (end >= 0 && (token.charAt( end ) == '.' || token.charAt( end ) == ',' || token.charAt( end ) == ')')) {
			end--;
		}

		int numDigits = 0;
		int numCaps = 0;
		int numPuncts = 0;

		for (int si = start; si <= end; si++) {
			char c = token.charAt( si );
			if (Character.isDigit( c )) {
				numDigits++;
			}
			else if (Character.isUpperCase( c )) {
				numCaps++;
			}
			else if (c == '-' || c == '/' || c == '.') {
				numPuncts++;
			}
			else if (!Character.isLowerCase( c )) {
				return false;
			}
		}

		double r = ((double)(numDigits + numCaps + numPuncts)) / token.length();

		return (numDigits >= 5) || (numDigits >= 3 && token.length() >= 5 && r > 0.5);
	}


	// Returns the number of capital letters that begin the string,
	// not including the initial ( or [
	private static int numStartCaps(String token) {
		int num = 0;
		if (token.charAt( 0 ) == '(' || token.charAt( 0 ) == '[') {
		}
		else if (!Character.isUpperCase( token.charAt( 0 ) )) {
			return 0;
		}
		else {
			num = 1;
		}

		for (int ci = 1; ci < token.length(); ci++) {
			if (Character.isUpperCase( token.charAt( ci ) )) {
				num++;
			}
			else {
				return num;
			}
		}
		return num;
	}


	// These may be part of an institution name
	private static boolean isInstProposition(String token) {
		return token.equalsIgnoreCase( "and" ) || token.equalsIgnoreCase( "of" )
		       || token.equalsIgnoreCase( "for" ) || token.equalsIgnoreCase( "the" )
		       || token.equalsIgnoreCase( "in" ) || token.equalsIgnoreCase( "at" )
		       || token.equalsIgnoreCase( "on" )
		       || token.equalsIgnoreCase( "und" ) || token.equalsIgnoreCase( "fur" )
		       || token.equalsIgnoreCase( "de" ) || token.equalsIgnoreCase( "la" )
		       || token.equalsIgnoreCase( "di" ) || token.equalsIgnoreCase( "y" );
	}


	private static boolean endsWithLetter(String token) {
		return Character.isLetterOrDigit( token.charAt( token.length() - 1 ) );
	}


	private static boolean hasNonInstFeature(String token) {
		for (int ci = 0; ci < token.length(); ci++) {
			if (Character.isDigit( token.charAt( ci ) )) {
				return true;
			}
		}
		return false;
	}


	// Main pattern matching method
	private static ArrayList tagGrantInfo(List sentence, int beginIndex) {
		ArrayList labels = new ArrayList();

		// tag tokens up to "supported by" as background
		for (int ti = 0; ti < beginIndex; ti++) {
			labels.add( "grant-background" );
		}

		boolean prevIsAnd = false;
		boolean inInst = false;
		StringBuffer inst = new StringBuffer();
		String prevInst = null;

		for (int ti = beginIndex; ti < sentence.size(); ti++) {
			String token = (String)sentence.get( ti );
			int startCapCount = numStartCaps( token );
			boolean letterEnding = endsWithLetter( token );
			boolean instProposition = isInstProposition( token );
			boolean nonInstFeature = hasNonInstFeature( token );

			// token is a grant number.  Currently assumes a grant number can only consist of one token
			if (isGrantNumber( token )) {
				labels.add( "B-grant-number" );
				inInst = false;
			}
			// first token of an institution name
			else if (!inInst && startCapCount > 0 && !nonInstFeature && !instProposition && token.length() > 1) {
				labels.add( "B-grant-institution" );

				// Not things like eg, NSF.
				if (letterEnding) {
					inInst = true;
				}
			}
			// within-token of an institution
			else if (inInst && (instProposition || startCapCount > 0) && !nonInstFeature) {
				// assume "and the ..." denotes start of a new institution name
				if (prevIsAnd && token.equalsIgnoreCase( "the" )) {
					prevIsAnd = false;
					labels.remove( labels.size() - 1 );
					labels.add( "grant-backgound" );
					labels.add( "grant-background" );
					continue;
				}

				if (token.equalsIgnoreCase( "and" )) {
					prevIsAnd = true;
				}

				labels.add( "I-grant-institution" );

				// last token of an institution
				if (!letterEnding && token.length() > 2 && token.charAt( token.length() - 1 ) != ')') {
					inInst = false;
				}
			}
			// not grant number or institution name
			else {
				inInst = false;
				labels.add( "grant-background" );
			}

			if (!token.equalsIgnoreCase( "and" )) {
				prevIsAnd = false;
			}
		}

		return labels;
	}


	// These should not be part of an institution name
	private static HashSet ignorableUpperCases = new HashSet();

	static {
		ignorableUpperCases.add( "grant" );
		ignorableUpperCases.add( "grants" );
		ignorableUpperCases.add( "no" );
		ignorableUpperCases.add( "nos" );
		ignorableUpperCases.add( "number" );
		ignorableUpperCases.add( "numbers" );
		ignorableUpperCases.add( "agreement" );
		ignorableUpperCases.add( "agreements" );
		ignorableUpperCases.add( "contract" );
		ignorableUpperCases.add( "contracts" );
		//ignorableUpperCases.add( "fellowship" ); 
		//ignorableUpperCases.add( "fellowships" );
		ignorableUpperCases.add( "under" );
	}


	// Remove non-alphanumeric characters from both ends of the given string
	private static String cleanup(String input) {
		int begin = 0;
		int end = input.length() - 1;

		while (begin < input.length() && !Character.isLetterOrDigit( input.charAt( begin ) )) {
			begin++;
		}
		while (end >= 0 && !Character.isLetterOrDigit( input.charAt( end ) )) {
			end--;
		}

		return (begin == end) ? "" : input.substring( begin, end + 1 );
	}


	// Convert the labels to properly handle B/I and remove extraneous text from institution names.
	// Assumes that grant numbers are associated with the institution last mentioned and that
	// grant numbers that don't follow any institution names form their own record
	private static Sequence postprocessLabels(List inputLabels, ArrayList sentence) {
		assert(inputLabels.size() == sentence.size());

		Object[] labels = inputLabels.toArray();

		// Relabel tokens improperly tagged as institution names to background
		for (int ti = 0; ti < sentence.size(); ti++) {
			String token = (String)sentence.get( ti );
			String label = (String)labels[ti];
			String newLabel = label;

			if (label.indexOf( "grant-institution" ) >= 0 && ignorableUpperCases.contains( cleanup( token ).toLowerCase() )) {
				newLabel = "grant-background";

				// relabel the next token if needed
				if (ti != sentence.size() - 1 && ((String)inputLabels.get( ti + 1 )).equals( "I-grant-institution" )) {
					labels[ti + 1] = "B-grant-institution";
				}
			}

			labels[ti] = newLabel;
		}

		// Leave all initial background labels and convert each initial grant number to a record
		int ti = 0;
		String label = null;
		for (; ti < inputLabels.size(); ti++) {
			label = (String)labels[ti];

			if (label.indexOf( "grant-institution" ) >= 0) {
				break;
			}
			else if (label.equals( "B-grant-number" )) {
				labels[ti] = "B-grant-record|grant-number";
			}
			else if (label.equals( "I-grant-number" )) {
				labels[ti] = "I-grant-record|grant-number";
			}
		}

		// Convert the rest of the labels to grant records (associating numbers to institutions)
		ArrayList stack = new ArrayList();

		assert(ti == inputLabels.size() || label.indexOf( "B-" ) == 0);

		for (; ti < inputLabels.size(); ti++) {
			label = (String)labels[ti];

			// Create accumulated record and begin a new record
			if (label.equals( "B-grant-institution" )) {
				addGrantRecord( labels, ti, stack );
			}

			stack.add( 0, labels[ti] );
		}
		// Add the last record
		addGrantRecord( labels, ti, stack );

		return new ArraySequence( labels );
	}


	// Helper method to postprocessLabels.  Modifies the labels and stack lists
	private static void addGrantRecord(Object[] labels, int ti, ArrayList stack) {
		boolean trailingBackground = true;
		int labelIndex = ti - 1;

		while (stack.size() > 0) {
			String rl = (String)stack.remove( 0 );

			if (trailingBackground && rl.equals( "grant-background" )) {
				// leave as is
			}
			else {
				trailingBackground = false;

				if (rl.equals( "B-grant-institution" )) {
					labels[labelIndex] = "B-grant-record|grant-institution";
				}
				else if (rl.equals( "grant-background" )) {
					labels[labelIndex] = "I-grant-record|" + rl;
				}
				else {
					String newLabel = rl.substring( 2 );
					labels[labelIndex] = "I-grant-record|" + newLabel;
				}
			}

			labelIndex--;
		}
	}


	private static void print(NewHtmlTokenization t) {
		System.out.print( "## " );
		for (int i = 0; i < t.size(); i++) {
			System.out.print( ((StringSpan)t.getToken( i )).getText() + " " );
		}
		System.out.println();
	}

	private static void print(Sequence t) {
		System.out.print( "## " );
		for (int i = 0; i < t.size(); i++) {
			System.out.print( t.get( i ) + " " );
		}
		System.out.println();
	}

	private static void print(Object[] o) {
		System.out.println( "\t[" );
		for (int i = 0; i < o.length; i++) {
			System.out.println( "\t" + i + " " + o[i] );
		}
		System.out.println( "\t]" );
	}
}
