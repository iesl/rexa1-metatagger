/**
 * Created on Jan 20, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */

package org.rexo.extraction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.rexo.referencetagging.NewHtmlTokenization;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;

/**
 * This pipe takes care of B/I tags for author segmentation and handles tokens inside a <note> tag differently.
 * <p/>
 * Also adds layout features useful for header/reference extraction
 */
public class NewHtmlTokenization2TokenSequence extends Pipe implements Serializable {
	private static String[] PS2TEXT_PROPERTY_NAMES = new String[]{"newfontsize", "newfontname", "newfontnumber",
	                                                              "newline", "newpage",
	                                                              "leftQuote", "rightQuote"};
	private static String AUTHOR_BEGIN_PROPERTY_NAME = "authorBeginning";

	boolean m_doAuthorSegmentation;
	boolean m_doNotePrefix;
	boolean m_addLayoutFeatures;
	boolean m_inProduction;

	public NewHtmlTokenization2TokenSequence() {
		this( true, true, true, true );
	}

	public NewHtmlTokenization2TokenSequence(boolean doAuthorSegmentation, boolean doNotePrefix,
	                                         boolean addLayoutFeatures, boolean inProduction) {
		m_doAuthorSegmentation = doAuthorSegmentation;
		m_doNotePrefix = doNotePrefix;
		m_addLayoutFeatures = addLayoutFeatures;
		m_inProduction = inProduction;
	}

	public Instance pipe(Instance carrier) {
		NewHtmlTokenization htmlTokenization = (NewHtmlTokenization)carrier.getData();

		// Check if the input instance has already been put through this pipe.
		// If so, return it immediately
		if (! m_inProduction && htmlTokenization.getActiveSpanList() == null) {
			return carrier;
		}

		TokenSequence targetTokens = new TokenSequence();

		addPs2TextLayoutFeatures( htmlTokenization );

		if (m_addLayoutFeatures) {
			addLineBasedLayoutFeatures( htmlTokenization );
			addPageBasedLayoutFeatures( htmlTokenization );
			addPrevTokenBasedLayoutFeatures( htmlTokenization );
			addLayoutConjunctionFeatures( htmlTokenization );
			addInstanceBasedFeatures( htmlTokenization );
		}

		List activeSpanList = htmlTokenization.getActiveSpanList();

		// Convert the tokenization to a token sequence
		for (int si = 0; si < htmlTokenization.size(); si++) {
			StringSpan span = (StringSpan)htmlTokenization.getToken( si );  // one token

			if (isTargetProcessing() && activeSpanList.size() > si) {
				String nestedTags = (String)activeSpanList.get( si );
				String innerMostTag = nestedTags.substring( nestedTags.lastIndexOf( ":" ) + 1 );
				String target = innerMostTag;

				// Set target label
				if (m_doAuthorSegmentation && ! innerMostTag.equals( "author" ) && nestedTags.indexOf( "author:" ) >= 0) {
					if (span.getNumericProperty( AUTHOR_BEGIN_PROPERTY_NAME ) > 0)
						target = "author-begin|" + target;
					else
						target = "author-inside|" + target;
				}

				if (m_doNotePrefix && ! innerMostTag.equals( "note" ) && nestedTags.indexOf( "note:" ) >= 0)
					target = "note|" + target;
				targetTokens.add( new Token( target ) );
			}
			else
				targetTokens.add( new Token( "unspecified" ) );
		}

		if (! m_inProduction)
		 	htmlTokenization.cleanup();

		carrier.setData( htmlTokenization );
		carrier.setTarget( targetTokens );

		// if (isTargetProcessing()) {
		//     	System.out.println("~~~N:" + carrier.getName());
		//     	for (int i = 0; i < htmlTokenization.size(); i++) {
		// 		System.out.println("~~~X:" + ((Token)htmlTokenization.get(i)).getText());
		// 		System.out.println("~~~T:" + targetTokens.get(i));
		// 		System.out.println("~~~D:" + htmlTokenization.get(i));
		//     	}
		// }


		return carrier;
	}

	// The code from different methods below that add layout features could be streamlined
	// to a single method to be more efficient, at the expense of ease of coding and modularity

	// Add features depending on whether the sequence contains certain properties
	private static void addInstanceBasedFeatures(NewHtmlTokenization tokenSeq) {
		boolean containsDissertation = false;

		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan currentToken = (StringSpan)tokenSeq.getToken( ti );
			String text = currentToken.getText();

			if (text.equalsIgnoreCase( "dissertation" ) || text.equalsIgnoreCase( "thesis" )) {
				containsDissertation = true;
				break;
			}
		}

		if (containsDissertation) {
			for (int ti = 0; ti < tokenSeq.size(); ti++) {
				StringSpan currentToken = (StringSpan)tokenSeq.getToken( ti );
				currentToken.setFeatureValue( "SEQCONTAINSDISSERTATION", 1.0 );
			}
		}
	}

	// Add features depending on whether a page contains certain properties
	private static void addPageBasedLayoutFeatures(NewHtmlTokenization tokenSeq) {
		int pageNum = 0;
		int pageStart = 0;
		boolean pageContainsLargestFont = false;
		boolean pageContainsAbstract = false;

		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan currentToken = (StringSpan)tokenSeq.getToken( ti );
			String text = currentToken.getText();

			// Ignore the initial newpage given by the first token
			// since we haven't actually scanned through the page yet
			if (ti != 0 && currentToken.getNumericProperty( "newpage" ) > 0) {

				for (int j = pageStart; j < ti; j++) {
					StringSpan t = (StringSpan)tokenSeq.getToken( j );
					if (pageContainsLargestFont)
						t.setFeatureValue( "PAGECONTAINSLARGESTFONT", 1.0 );
					if (pageContainsAbstract)
						t.setFeatureValue( "PAGECONTAINSWORDABSTRACT", 1.0 );
				}

				pageNum++;
				pageStart = ti;
				pageContainsLargestFont = false;
				pageContainsAbstract = false;
			}

			if (pageNum < 4)
				currentToken.setFeatureValue( "ONPAGE" + pageNum, 1.0 );

			// Note the font size and text of the token
			if (text.equalsIgnoreCase( "abstract" ))
				pageContainsAbstract = true;
			if (currentToken.getNumericProperty( "largestfont" ) > 0)
				pageContainsLargestFont = true;
		}

		// Add features for last page
		for (int j = pageStart; j < tokenSeq.size(); j++) {
			StringSpan t = (StringSpan)tokenSeq.getToken( j );
			if (pageContainsLargestFont)
				t.setFeatureValue( "PAGECONTAINSLARGESTFONT", 1.0 );
			if (pageContainsAbstract)
				t.setFeatureValue( "PAGECONTAINSWORDABSTRACT", 1.0 );
		}
	}

	// Add features depending on whether a line contains certain properties,
	// such as the initial token and the regularity of distance to previous
	// and following lines
	private static void addLineBasedLayoutFeatures(NewHtmlTokenization tokenSeq) {
		// For each line, store its distance to the previous line
		double prevLLY = 0;
		ArrayList dist2prevLine = new ArrayList();
		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan currentToken = (StringSpan)tokenSeq.getToken( ti );

			if (ti == 0 || currentToken.getNumericProperty( "newline" ) > 0) {
				double currLLY = currentToken.getNumericProperty( "lly" );
				double dist = (ti == 0) ? 0 : Math.abs( currLLY - prevLLY );
				prevLLY = currLLY;
				dist2prevLine.add( new Double( dist ) );
			}
		}

		//System.out.println("DDD dist2prev=" + dist2prevLine);

		// Diff b/w two vertical distances has to be greater than this to be considered actually different 
		final int tolerance = 2;
		// Two lines can belong to the same group if their distance is no greater than this
		final int groupDistLimit = 15;

		// One bit per line; change of parity denotes notable change of dist b/w lines
		int numLines = dist2prevLine.size();
		BitSet lineRegions = new BitSet();
		BitSet lineBigJumpAfter = new BitSet();
		BitSet lineBigJumpBefore = new BitSet();
		boolean parity = false;
		int lineNum = -1;

		// Mark line groupings
		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan currentToken = (StringSpan)tokenSeq.getToken( ti );

			if (ti == 0 || currentToken.getNumericProperty( "newline" ) > 0) {
				lineNum++;
				double prevDist = ((Double)dist2prevLine.get( lineNum )).doubleValue();
				double nextDist = (lineNum == numLines - 1) ? Integer.MAX_VALUE : ((Double)dist2prevLine.get( lineNum + 1 )).doubleValue();

				// Check which group this line belongs to
				if (Math.abs( prevDist - nextDist ) <= tolerance) {
					// same group
				}
				else if (currentToken.getNumericProperty( "newpage" ) > 0) {
					// first line on a page starts a new group
					parity = ! parity;
					if (nextDist > groupDistLimit)
						lineBigJumpAfter.set( lineNum );
				}
				else if (nextDist < prevDist && nextDist <= groupDistLimit) {
					// line belongs to the next group
					parity = ! parity;
					lineBigJumpBefore.set( lineNum );
				}
				else if (prevDist < nextDist && prevDist <= groupDistLimit) {
					// line belongs to the previous group
					lineBigJumpAfter.set( lineNum );
				}
				else {
					assert (prevDist > groupDistLimit && nextDist > groupDistLimit);
					// line belongs to its own group
					parity = ! parity;
					lineBigJumpBefore.set( lineNum );
					lineBigJumpAfter.set( lineNum );
				}

				if (parity)
					lineRegions.set( lineNum );
			}
		}

		//System.out.println("BBB " + lineBigJumpBefore + "\nAAA " + lineBigJumpAfter + "\nCCC " + lineRegions);


		// For each line, count the number of lines that are in the same text region as it
		int[] numLinesInRegion = new int[numLines];
		int regionStart = 0;
		parity = lineRegions.get( 0 );
		while (regionStart <= lineNum) {
			int nextRegionStart = parity ? lineRegions.nextClearBit( regionStart ) : lineRegions.nextSetBit( regionStart );
			// This happens for the last line group
			if (nextRegionStart < 0)
				nextRegionStart = lineNum + 1;

			int n = nextRegionStart - regionStart;
			for (int i = regionStart; i < nextRegionStart && i <= lineNum; i++)
				numLinesInRegion[i] = n;

			regionStart = nextRegionStart;
			parity = ! parity;

			assert(regionStart >= 0);
		}

		/*System.out.print("BBB");
		for (int i = 0; i < numLinesInRegion.length; i++)
			System.out.print(" " + numLinesInRegion[i]);
			System.out.println();*/

		// Finally add the features
		lineNum = -1;
		StringSpan firstTokenInLine = null;
		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan currentToken = (StringSpan)tokenSeq.getToken( ti );

			if (ti == 0 || currentToken.getNumericProperty( "newline" ) > 0) {
				lineNum++;
				firstTokenInLine = currentToken;
			}
			else {
				if (! sameFontNumber( currentToken, firstTokenInLine ))
					currentToken.setFeatureValue( "INITLINETOKENDIFFFONTNUMBER", 1.0 );
				if (! sameFontName( currentToken, firstTokenInLine ))
					currentToken.setFeatureValue( "INITLINETOKENDIFFFONTNAME", 1.0 );
				if (! sameFontSize( currentToken, firstTokenInLine ))
					currentToken.setFeatureValue( "INITLINETOKENDIFFFONTSIZE", 1.0 );
			}

			if (ti > 0 && currentToken.getNumericProperty( "newline" ) > 0) {
				StringSpan prevToken = (StringSpan)tokenSeq.getToken( ti - 1 );
				prevToken.setFeatureValue( "LASTTOKENONLINE", 1.0 );
			}

			assert(lineNum >= 0);

			if (lineBigJumpBefore.get( lineNum ))
				currentToken.setFeatureValue( "BIGLINEJUMPBEFORE", 1.0 );
			if (lineBigJumpAfter.get( lineNum ))
				currentToken.setFeatureValue( "BIGLINEJUMPAFTER", 1.0 );

			if (numLinesInRegion[lineNum] == 1)
				currentToken.setFeatureValue( "IN1LINETEXTREGION", 1.0 );
			else if (numLinesInRegion[lineNum] == 2)
				currentToken.setFeatureValue( "IN2LINETEXTREGION", 1.0 );

			if (numLinesInRegion[lineNum] >= 3)
				currentToken.setFeatureValue( "IN3LINETEXTREGION", 1.0 );
			if (numLinesInRegion[lineNum] >= 4)
				currentToken.setFeatureValue( "IN4LINETEXTREGION", 1.0 );
			if (numLinesInRegion[lineNum] >= 5)
				currentToken.setFeatureValue( "IN5LINETEXTREGION", 1.0 );
		}
	}

	// Add features depending on the previous token with a different font property
	private static void addPrevTokenBasedLayoutFeatures(NewHtmlTokenization tokenSeq) {
		StringSpan  prevTokenWithDifferentFont = null;
		String prevTokenWithDifferentFontText = null;

		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan  currentToken = (StringSpan )tokenSeq.getToken( ti );

			if (prevTokenWithDifferentFont == null) {
				prevTokenWithDifferentFont = currentToken;
				prevTokenWithDifferentFontText = prevTokenWithDifferentFont.getText();
			}
			else if (! sameFontNumber( currentToken, prevTokenWithDifferentFont )) {

				if (prevTokenWithDifferentFontText.matches( "\\d+" ))
					currentToken.setFeatureValue( "PREVFORMATTOKENISNUMBER", 1.0 );

				if (! prevTokenWithDifferentFontText.matches( "\\p{Alpha}+" ))
					currentToken.setFeatureValue( "PREVFORMATTOKENISNOTLETTER", 1.0 );

				if (prevTokenWithDifferentFontText.equalsIgnoreCase( "abstract" ))
					currentToken.setFeatureValue( "PREVFORMATTOKENISABSTRACT", 1.0 );

				if (prevTokenWithDifferentFontText.equalsIgnoreCase( "keyword" )
				    || prevTokenWithDifferentFontText.equalsIgnoreCase( "keywords" ))
					currentToken.setFeatureValue( "PREVFORMATTOKENISKEYWORDS", 1.0 );

				prevTokenWithDifferentFont = currentToken;
				prevTokenWithDifferentFontText = currentToken.getText();
			}
		}
	}

	// Add conjunctions of certain layout features 
	private static void addLayoutConjunctionFeatures(NewHtmlTokenization tokenSeq) {
		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan  currentToken = (StringSpan )tokenSeq.getToken( ti );

			if (currentToken.getFeatureValue( "LARGESTFONT" ) > 0 && currentToken.getFeatureValue( "IN1LINETEXTREGION" ) > 0)
				currentToken.setFeatureValue( "LARGESTFONT-AND-IN1LINETEXTREGION", 1.0 );
			if (currentToken.getFeatureValue( "LARGESTFONT" ) > 0 && currentToken.getFeatureValue( "IN2LINETEXTREGION" ) > 0)
				currentToken.setFeatureValue( "LARGESTFONT-AND-IN2LINETEXTREGION", 1.0 );
			if (currentToken.getFeatureValue( "LARGESTFONT" ) > 0 && currentToken.getFeatureValue( "IN3LINETEXTREGION" ) > 0)
				currentToken.setFeatureValue( "LARGESTFONT-AND-IN3LINETEXTREGION", 1.0 );
		}
	}

	private static boolean sameFontSize(StringSpan t1, StringSpan  t2) {
		return t1.getNumericProperty( "largestfont" ) == t2.getNumericProperty( "largestfont" )
		       && t1.getNumericProperty( "largefont" ) == t2.getNumericProperty( "largefont" )
		       && t1.getNumericProperty( "smallfont" ) == t2.getNumericProperty( "smallfont" );
	}

	private static boolean sameFontNumber(StringSpan t1, StringSpan t2) {
		return t1.getNumericProperty( "fontnumber" ) == t2.getNumericProperty( "fontnumber" );
	}

	private static boolean sameFontName(StringSpan t1, StringSpan t2) {
		return t1.getProperty( "fontname" ).equals( t2.getProperty( "fontname" ) );
	}

	private static void addPs2TextLayoutFeatures(NewHtmlTokenization tokenSeq) {
		String propertyName;

		// Figure whether the largest font size appears in the given token sequence
		boolean hasLargestFont = false;
		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan token = (StringSpan)tokenSeq.getToken( ti );
			if (token.getNumericProperty( "largestfont" ) > 0) {
				hasLargestFont = true;
				break;
			}
		}

		for (int ti = 0; ti < tokenSeq.size(); ti++) {
			StringSpan token = (StringSpan)tokenSeq.getToken( ti );

			propertyName = "firstInTextBox";
			if (token.getNumericProperty( propertyName ) > 0)
				token.setFeatureValue( "FIRSTINTEXTBOX", 1.0 );

			propertyName = "smallfont";
			if (token.getNumericProperty( propertyName ) > 0)
				token.setFeatureValue( propertyName.toUpperCase(), 1.0 );

			propertyName = "largefont";
			if (token.getNumericProperty( propertyName ) > 0) {
				if (hasLargestFont)
					token.setFeatureValue( propertyName.toUpperCase(), 1.0 );
				else
					token.setFeatureValue( "LARGESTFONT", 1.0 );
			}

			propertyName = "largestfont";
			if (token.getNumericProperty( propertyName ) > 0)
				token.setFeatureValue( propertyName.toUpperCase(), 1.0 );

			// Add features for the rest of the properties
			for (int i = 0; i < PS2TEXT_PROPERTY_NAMES.length; i++) {
				propertyName = PS2TEXT_PROPERTY_NAMES[i];
				if (token.getNumericProperty( propertyName ) > 0)
					token.setFeatureValue( propertyName.toUpperCase(), 1.0 );
			}
		}
	}

	public boolean isInProduction() {
		return m_inProduction;
	}

	public void setInProduction(boolean production) {
		m_inProduction = production;
	}

	public static void setInProduction(Pipe p, boolean inProduction) {
		if (p instanceof NewHtmlTokenization2TokenSequence)
			((NewHtmlTokenization2TokenSequence)p).setInProduction( inProduction );
		else if (p instanceof SerialPipes) {
			SerialPipes sp = (SerialPipes)p;

			for (int i = 0; i < sp.size(); i++)
				setInProduction( sp.getPipe( i ), inProduction );
		}
	}

	// Serialization 

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt( CURRENT_SERIAL_VERSION );
		out.writeBoolean( m_doAuthorSegmentation );
		out.writeBoolean( m_doNotePrefix );
		out.writeBoolean( m_addLayoutFeatures );
		out.writeBoolean( m_inProduction );
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		m_doAuthorSegmentation = in.readBoolean();
		m_doNotePrefix = in.readBoolean();
		m_addLayoutFeatures = in.readBoolean();
		m_inProduction = in.readBoolean();
	}

}
