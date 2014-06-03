package org.rexo.referencetagging;

import edu.umass.cs.mallet.base.extract.StringSpan;
import org.apache.log4j.Logger;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.util.UnicodeUtil;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SegmentationFinder {
	private static Logger log = Logger.getLogger( SegmentationFinder.class );

	private Pattern headerEndPattern;
	private Pattern headerEndPatternLessStrict;
	private Pattern bodyEndPattern;

	/* Regexes used to find section boundaries */

	/* Public since it is used by 'BibliographySegmenter' in one place */
	public static String bodyEnd = "^\\s*             # Possible whitespace at beginning.\n" +
	                               "(\\d+(\\.\\d+)*\\.?\\s+)*                # It may have a section number.\n" +
	                               "(References|REFERENCES|Bibliography|BIBLIOGRAPHY)      # different ways of naming the reference section.\n" +
	                               "\\:?                           # There might be a colon.\n" +
	                               "\\.?                           # There might be a period.\n" +
	                               "\\s*$                          # We want nothing else on the line except whitespace\n";

	public SegmentationFinder() {
		String headerEnd = "^\\s*      # Possible whitespace at the beginning.\n" +
		                   "(                         # Grouping for section numbering/marking.\n" +
		                   " (                        # Section numbering has different choices\n" +
		                   "  \\d\\.*|I+\\.*|\\d\\.0  # A number or roman numerals can be used, sometimes they are followed by a period\n" +
		                   " )                        # ends 'or' group\n" +
		                   " \\s+                     # space after section numbering\n" +
		                   ")?                        # There should be no numbering or just one instance\n" +
		                   "(introduction|background)            # Not all papers name their first section introduction, but this is the most frequent choice.\n";
		this.headerEndPattern = Pattern.compile( headerEnd, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS );

		// TODO: change the final requirement to [A-Z][a-zA-Z]?
		String headerEndLessStrict = "^\\s*      # Possible whitespace at the beginning.\n" +
		                             "(                         # Grouping for section numbering/marking.\n" +
		                             " (                        # Section numbering has different choices\n" +
		                             "  \\d\\.*|I+\\.*|\\d\\.0  # A number or roman numerals can be used, sometimes they are followed by a period\n" +
		                             " )                        # ends 'or' group\n" +
		                             " \\s+                     # space after section numbering\n" +
		                             ")                        # There should be no numbering or just one instance\n" +
		                             "[A-Z][a-z]            # Not all papers name their first section introduction, but this is the most frequent choice.\n";
		this.headerEndPatternLessStrict = Pattern.compile( headerEndLessStrict, Pattern.COMMENTS );

		this.bodyEndPattern = Pattern.compile( bodyEnd, Pattern.COMMENTS );
	}


	private String lineText(NewHtmlTokenization tokenization, int startToken, int endToken) {
		// Skip phantom tokens when constructing line text
		StringSpan firstToken = (StringSpan)tokenization.getToken( startToken );
		double phantomProperty = firstToken.getNumericProperty( "phantom" );
		String lineText = "";
		if (phantomProperty > 0) {
			startToken++;
		}
		if (startToken < endToken) {
			int startIdx = tokenization.getSpan( startToken ).getStartIdx();
			int endIdx = tokenization.getSpan( endToken - 1 ).getEndIdx();
			try {
				lineText = ((CharSequence)tokenization.getDocument()).subSequence( startIdx, endIdx ).toString();
				lineText = cleanPsTotextOutput( lineText );
			}
			catch (StringIndexOutOfBoundsException e) {
				log.error( "SegmentationFinder.lineText(): " + e.getMessage() );
			}
			catch (IndexOutOfBoundsException e) {
				log.error( "SegmentationFinder.lineText(): " + e.getMessage() );
			}
		}
		return lineText;
	}

	/**
	 * Combining Diacritical marks: Combining grave accent = U+0300 Combining acute accent = U+0301 Modifier letter acute accent = U+02CA Modifier letter grave accent = U+02CB Acute Accent = U+00B4 grave
	 * = 0060 cedilla = 00C8 masculine indicator (circle above) 00BA diuresis 00A8
	 * <p/>
	 * special cases:
	 * <p/>
	 * url ../~smith/... ^TM
	 */

	private String cleanPsTotextOutput(String lineText1) {
		try {
			byte[] bytes = lineText1.getBytes( "ISO-8859-1" );
			String lineText = new String( bytes, "ISO-8859-1" );
			ArrayList replacements = new ArrayList();
			int ltLen = lineText.length();
			Pattern loneCharModifierP = Pattern.compile( "([\\^\\`\\¨\\¯\\´\\¸\\~\\º\\'])" );
			Matcher matcher = loneCharModifierP.matcher( lineText );

			boolean foundMatch = false;
			// get a 3-char window of text around the unicode character (excluding whitespace)
			while (matcher.find()) {
				foundMatch = true;
				int s = matcher.start();
				int e = matcher.end();
				String found = matcher.group();
				// some special cases:
				if ("~".equals( found ) && '/' == lineText.charAt( s - 1 )) {
					// probably a url like "../~smith/...
				}
				else if ("^".equals( found ) && s + 2 < ltLen && 'T' == lineText.charAt( s + 1 ) && 'M' == lineText.charAt( s + 2 )) {
					// U+2122 Trade Mark Sign:
					// replacements.add( new Object[]{new int[]{s, s + 3}, "\u2122"} );
				}
				else if ("`".equals( found ) && s + 1 < ltLen && '`' == lineText.charAt( s + 1 )) {
					// dquote
					replacements.add( new Object[]{new int[]{s, s + 2}, "\""} );
				}
				else if ("'".equals( found )) {
					if (s + 1 < ltLen && '\'' == lineText.charAt( s + 1 )) {
						// dquote
						replacements.add( new Object[]{new int[]{s, s + 2}, "\""} );
					}
				}
				else {
					// 'back up' over matched text to find last non-space character
					// while (--s > 0 && ' ' == lineText.charAt( s )) ;
					// 'back up' over matched text to find last character
					s = s > 0 ? s - 1 : s;
					// look forward at most 2 chars over matched text to find next non-space character
					e = e < ltLen ? e + 1 : e;
					e = e < ltLen && ' ' == lineText.charAt( e ) ? e + 1 : e;
					String r = getReplacement( lineText.substring( s, e ) );
					if (r != null) {
						replacements.add( new Object[]{new int[]{s, e}, r} );
					}
				}
			}

			int last = 0;
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < replacements.size(); i++) {
				Object[] repl = (Object[])replacements.get( i );
				int[] se = (int[])repl[0];
				String r = (String)repl[1];
				stringBuffer.append( lineText.substring( last, se[0] ) );
				stringBuffer.append( r );
				last = se[1];
			}
			stringBuffer.append( lineText.substring( last, ltLen ) );

			if (foundMatch) {
				log.debug( "Cleaning pstotext (" + replacements.size() + " replacements): " + lineText + " ---> " + stringBuffer.toString() );
			}

			return stringBuffer.toString();
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException( e );
		}
	}


	private Map charSubstitutionTable = null;

	/**
	 *
	 * @param inputText
	 */
	private String getReplacement(String inputText) {
		if (charSubstitutionTable == null) {
			charSubstitutionTable = UnicodeUtil.create2CharSubstitutionTable();
		}
		// strip whitespace
		String replText = inputText.replaceAll( "\\s", "" );
		if (replText.length() == 3) {
			String s = (String)charSubstitutionTable.get( replText.substring( 1, 3 ) );
			if (s == null) {
				s = (String)charSubstitutionTable.get( replText.substring( 0, 2 ) );
				if (s != null) {
					s = s + replText.substring( 2, 3 );
				}
			}
			else {
				s = replText.substring( 0, 1 ) + s;
			}
			return s;
		}
		return null;
	}

	/**
	 * Converts a tokenization representation into the old 'TextLine'/'PageBoundary' format used by the segemtation code. FIXME: switch to spans
	 */
	private List makeLines(NewHtmlTokenization tokenization) {
		List lineList = new ArrayList();
		int pageNum = 0;
		int prevLineStartTokNum = -1;
		//int prevLinePageNum = -1;
		boolean firstLine = true;
		int llx = -1;
		int lly = -1;

		for (int tokenNum = 0; tokenNum < tokenization.size(); tokenNum++) {
			StringSpan s = (StringSpan)tokenization.getSpan( tokenNum );
			double pageProperty = s.getNumericProperty( "newpage" );

			// Insert page boundary objects as necessary
			if (pageProperty > 0) {
				//pageNum = (int) s.getNumericProperty("pagenum");
				TextPageBoundary pageBoundary = new TextPageBoundary();
				lineList.add( pageBoundary );
				pageNum++;
			}

			double newlineProperty = s.getNumericProperty( "newline" );
			if (newlineProperty > 0) {
				if (prevLineStartTokNum > -1) {

					// Finish the previously started line
					int startToken = prevLineStartTokNum;
					//Increased since 'subtokenization' now takes high-exclusive token ranges
					//int endToken = tokenNum-1;
					int endToken = tokenNum;
					String lineText = lineText( tokenization, startToken, endToken );
					TextLine t = new TextLine( startToken, endToken, llx, lly, pageNum, lineText );
					lineList.add( t );
				}
				// Start new line
				prevLineStartTokNum = tokenNum;
				//prevLinePageNum = pageNum;
				llx = (int)s.getNumericProperty( "llx" );
				lly = (int)s.getNumericProperty( "lly" );

			}
		}

		// Finish final line
		if (prevLineStartTokNum > -1) {
			int startToken = prevLineStartTokNum;
			//int endToken = tokenization.size()-1;
			int endToken = tokenization.size();
			String lineText = lineText( tokenization, startToken, endToken );
			TextLine t = new TextLine( startToken, endToken, llx, lly, pageNum, lineText );
			lineList.add( t );
		}

		return lineList;
	}

	// Converts a list of text objects into an initial/final
	// token location.  Used for converting the results of the
	// old segmentation into the new format.
	private long[] lineListBoundaries(List lineList) {

		long startToken = -1;
		long endToken = -1;

		Iterator textI = lineList.iterator();
		while (textI.hasNext()) {
			Object o = textI.next();
			if (o instanceof TextLine) {
				// debug
				//System.out.println(((TextLine)o).getText());

				if (startToken < 0) {
					startToken = ((TextLine)o).startTokenNum;
					assert startToken >= 0;
				}
				endToken = ((TextLine)o).endTokenNum;
				assert endToken >= 0;
			}
		}
		return new long[]{startToken, endToken};
	}


	/* Sets 'targetList' to the elements ocurring before the first match of
	 * 'pattern', and 'sourceList' to the remaining elements */
	private void findLineType(LinkedList sourceList, LinkedList targetList, Pattern pattern) {

		while (!sourceList.isEmpty()) {

			Object o = sourceList.getFirst();
			if (o instanceof TextLine) {
				String text = ((TextLine)o).getText();
				Matcher matchObject = pattern.matcher( text );
				if (matchObject.find()) {
					break;
				}
			}

			targetList.add( sourceList.removeFirst() );
		}
	}

	/* Sets 'targetList' to the elements ocurring before the last match of
	   'pattern', and 'sourceList' to the remaining elements.
	*/
	private void findLineTypeReverseOrder(LinkedList sourceList, LinkedList targetList, Pattern pattern) {
		// find the first match from the end, in reverse order
		ListIterator sourceIterator = sourceList.listIterator( sourceList.size() );
		while (sourceIterator.hasPrevious()) {
			Object o = sourceIterator.previous();
			if (o instanceof TextLine) {
				String text = ((TextLine)o).getText();
				Matcher matchObject = pattern.matcher( text );
				if (matchObject.find()) {
					// split the list
					int splitIndex = sourceIterator.nextIndex();
					sourceIterator = null;
					// targetList gets everything < match
					targetList.addAll( sourceList.subList( 0, splitIndex ) );
					// sourceLIst keeps everything >= match
					sourceList.subList( 0, splitIndex ).clear();
					return;
				}
			}
		}
		// If no match was found, move everything to targetList
		targetList.addAll( sourceList );
		sourceList.clear();
	}

	// TODO: switch to this version
	// If 'onlyReferencs' is true, segmentation finding is skipped and
	// 'onlyReferences' is treated as containing just the bibliography
	// section.  
	// (At the moment this is used to assist in in retagging documents when creating an
	// annotated test set)
	public SegmentationData findSegments(List inLineList, boolean onlyReferences) throws HeaderNotFoundException,
	                                                                                     ReferencesNotFoundException,
	                                                                                     ReferenceParsingException {
		SegmentationData segData = new SegmentationData();
		LinkedList lineList = new LinkedList();
		lineList.addAll( inLineList );
		LinkedList headerLineList = new LinkedList();
		LinkedList bodyLineList = new LinkedList();

		if (!onlyReferences) {
			//System.out.println("finding header lines");

			//**** Process header ****
			// find end of header
			findLineType( lineList, headerLineList, this.headerEndPattern );

			if (lineList.isEmpty()) {
				// If the end of the header couldn't be located, try again using a less
				// strict regex
				/* In a study of 28 cases where using the "less strict header regex" caused
					the document to succeed in the pipeline, it was found that in 13/28 times,
					this header end was found in the "right" place, in 7/28 times in the "wrong"
					place, in 6/28 cases in the "right" place but the mathced line was a mangled
					'Introduction' caused by pdfbox errors, and in 2/28 times it was unclear
					whether the was the "desired.  The original 28 cases were taken from 500 documents
					so this represents (roughly) a boost of 2% to succesfully processed documents and
					1% to documents which were succesfully processed but possibly with invalid header
					information.  This is a topic for future investigation.
					*/
				lineList = headerLineList;
				headerLineList = new LinkedList();
				findLineType( lineList, headerLineList, this.headerEndPatternLessStrict );
				if (!lineList.isEmpty()) {
					//// TESTING
					//throw new HeaderNotFoundException("less strict header regex");
					//logAuxInfo("INFO", "less strict header regex");
				}
			}

			if (lineList.isEmpty()) {
				//finalizeXML(ret);
				throw new HeaderNotFoundException( "Introduction not found error" );
			}

			//int[] headerTokenBoundaries = lineListBoundaries( headerLineList );
			//NewHtmlTokenization header = tokenization.subTokenization( headerTokenBoundaries[0], headerTokenBoundaries[1] );
			//tokenization.setProperty( "headerTokenization", header );
			segData.headerBoundaries = lineListBoundaries( headerLineList );

			// Find end of body
			// The References section is searched for in reverse, from the end of the document. This may help with the 
			// problem of prematurely matching 'References' in a document's table of contents
			// Actually, I've disabled 'Reverse search' until I have time to test it more 
			//System.out.println("finding bibliography");
			findLineType( lineList, bodyLineList, this.bodyEndPattern );

			if (lineList.isEmpty()) {
				// DEBUG: Print out the last line of the introduction (maybe we marked it incorrectly)
				throw new ReferencesNotFoundException( "References not found error" );
			}

			// Extract the body
			//int[] bodyTokenBoundaries = lineListBoundaries( bodyLineList );
			//NewHtmlTokenization body = tokenization.subTokenization( bodyTokenBoundaries[0], bodyTokenBoundaries[1] );
			//tokenization.setProperty( "bodyTokenization", body );
			segData.bodyBoundaries = lineListBoundaries( bodyLineList );
		}

		// Extract References
		//System.out.println("extracting references");
		BibliographySegmenter refExtractor = new BibliographySegmenter();
		BibliographySegmenter.ReferenceData referenceData = refExtractor.extractReferences( lineList );
		//System.out.println("done... adding extra spans");

		// Extract biblioPrologue
		//int[] prologueTokenBoundaries = lineListBoundaries( referenceData.prologueList );
		//NewHtmlTokenization prologue = tokenization.subTokenization( prologueTokenBoundaries[0],
		//                                                            prologueTokenBoundaries[1] );
		//tokenization.setProperty( "prologueTokenization", prologue );
		segData.biblioPrologueBoundaries = lineListBoundaries( referenceData.prologueList );

		// Create reference elements
		LinkedList referencesList = referenceData.referenceLineList;

		ArrayList refBounds = new ArrayList();

		while (!referencesList.isEmpty()) {
			List referenceLine = (List)referencesList.removeFirst();

			//int[] referenceTokenBoundaries = lineListBoundaries( referenceLine );
			//NewHtmlTokenization reference = tokenization.subTokenization( referenceTokenBoundaries[0],
			//			                                                              referenceTokenBoundaries[1] );
			//refTokenizationList.add( reference );
			refBounds.add( lineListBoundaries( referenceLine ) );
		}
		//tokenization.setProperty( "referenceList", refTokenizationList );
		segData.referenceList = refBounds;

		//int[] epilogueTokenBoundaries = lineListBoundaries( referenceData.epilogueList );
		//NewHtmlTokenization epilogue = tokenization.subTokenization( prologueTokenBoundaries[0],
		//                                                            prologueTokenBoundaries[1] );
		//tokenization.setProperty( "epilogueTokenization", epilogue );
		//segData.biblioEpilogueBoundaries = lineListBoundaries( referenceData.epilogueList );
		segData.biblioEpilogueBoundaries = (onlyReferences || referenceData.epilogueList.size() == 0) ? null
		                                   : lineListBoundaries( referenceData.epilogueList );

		//System.out.println("finished.");
		return segData;
	}

	// protected void markSections(NewHtmlTokenization tokenization, List inLineList) throws HeaderNotFoundException,
	//                                                                                       ReferencesNotFoundException,
	//                                                                                       ReferenceParsingException {
	//
	// 	LinkedList lineList = new LinkedList();
	// 	lineList.addAll( inLineList );
	// 	LinkedList headerLineList = new LinkedList();
	// 	LinkedList bodyLineList = new LinkedList();
	// 	Element retElement = new Element( "content.element" );
  //
	// 	//**** Process header ****
	// 	// find end of header
	// 	findLineType( lineList, headerLineList, this.headerEndPattern );
  //
	// 	if (lineList.isEmpty()) {
	// 		// If the end of the header couldn't be located, try again using a less
	// 		// strict regex
	// 		/* In a study of 28 cases where using the "less strict header regex" caused
	// 			the document to succeed in the pipeline, it was found that in 13/28 times,
	// 			this header end was found in the "right" place, in 7/28 times in the "wrong"
	// 			place, in 6/28 cases in the "right" place but the mathced line was a mangled
	// 			'Introduction' caused by pdfbox errors, and in 2/28 times it was unclear
	// 			whether the was the "desired.  The original 28 cases were taken from 500 documents
	// 			so this represents (roughly) a boost of 2% to succesfully processed documents and
	// 			1% to documents which were succesfully processed but possibly with invalid header
	// 			information.  This is a topic for future investigation.
	// 			*/
	// 		lineList = headerLineList;
	// 		headerLineList = new LinkedList();
	// 		findLineType( lineList, headerLineList, this.headerEndPatternLessStrict );
	// 	}
  //
	// 	if (lineList.isEmpty()) {
	// 		throw new HeaderNotFoundException( "Introduction not found error" );
	// 	}
  //
	// 	// Create header element
	// 	long[] headerTokenBoundaries = lineListBoundaries( headerLineList );
	// 	NewHtmlTokenization header = tokenization.subTokenization( (int)headerTokenBoundaries[0], (int)headerTokenBoundaries[1] );
	// 	tokenization.setProperty( "headerTokenization", header );
  //
	// 	// Find end of body
	// 	// The References section is searched for in reverse, from the end of the document. This may help with the
	// 	// problem of prematurely matching 'References' in a document's table of contents
	// 	// Actually, I've disabled 'Reverse search' until I have time to test it more
	// 	//findLineTypeReverseOrder( lineList, bodyLineList, this.bodyEndPattern );
	// 	//System.out.println("finding body lines");
	// 	findLineType( lineList, bodyLineList, this.bodyEndPattern );
  //
  //
	// 	if (lineList.isEmpty()) {
	// 		throw new ReferencesNotFoundException( "References not found error" );
	// 	}
  //
	// 	// Create body element
	// 	long[] bodyTokenBoundaries = lineListBoundaries( bodyLineList );
	// 	NewHtmlTokenization body = tokenization.subTokenization( (int)bodyTokenBoundaries[0], (int)bodyTokenBoundaries[1] );
	// 	tokenization.setProperty( "bodyTokenization", body );
	// 	//System.out.println("extracting reference");
  //
	// 	// Extract References
	// 	BibliographySegmenter refExtractor = new BibliographySegmenter();
	// 	BibliographySegmenter.ReferenceData referenceData = refExtractor.extractReferences( lineList );
  //
	// 	// Create biblioPrologue element
	// 	long[] prologueTokenBoundaries = lineListBoundaries( referenceData.prologueList );
	// 	NewHtmlTokenization prologue = tokenization.subTokenization( (int)prologueTokenBoundaries[0],
	// 	                                                             (int)prologueTokenBoundaries[1] );
  //
	// 	tokenization.setProperty( "prologueTokenization", prologue );
  //
	// 	// Create reference elements
	// 	LinkedList referencesList = referenceData.referenceLineList;
  //
	// 	LinkedList refTokenizationList = new LinkedList();
	// 	while (!referencesList.isEmpty()) {
	// 		List referenceLine = (List)referencesList.removeFirst();
	// 		////System.out.println("[findSections] reference is:"+lineListContent(referenceLine));
	// 		//Element referenceElement = new Element( "reference" );
	// 		//referenceElement.setContent( lineListContent( referenceLine ) );
	// 		//biblioElement.addContent( referenceElement );
	// 		//System.out.println("!!!reference lines");
	// 		long[] referenceTokenBoundaries = lineListBoundaries( referenceLine );
	// 		NewHtmlTokenization reference = tokenization.subTokenization( (int)referenceTokenBoundaries[0],
	// 		                                                              (int)referenceTokenBoundaries[1] );
	// 		refTokenizationList.add( reference );
  //
	// 	}
	// 	tokenization.setProperty( "referenceList", refTokenizationList );
  //
	// 	long[] epilogueTokenBoundaries = lineListBoundaries( referenceData.epilogueList );
	// 	NewHtmlTokenization epilogue = tokenization.subTokenization( (int)epilogueTokenBoundaries[0],
	// 	                                                             (int)epilogueTokenBoundaries[1] );
	// 	tokenization.setProperty( "epilogueTokenization", epilogue );
  //
	// 	//System.out.println("finished segmentation");
	// 	//return retElement;
	// }


	/* Partition element 'contentElement' into segments. */
	public void markup(NewHtmlTokenization tokenization) throws HeaderNotFoundException,
	                                                            ReferencesNotFoundException,
	                                                            ReferenceParsingException {
		// Group related elements into lines of text
		List contentLines = makeLines( tokenization );

		// Build marked up document, with marked sections and extracted
		// references.
		// markSections( tokenization, contentLines );
	}

	public static class SegmentationData {
		public long[] headerBoundaries;
		public long[] bodyBoundaries;
		public long[] biblioPrologueBoundaries;
		public long[] biblioEpilogueBoundaries;
		public ArrayList referenceList;
	}

}

