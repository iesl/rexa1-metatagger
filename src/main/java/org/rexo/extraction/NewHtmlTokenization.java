/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
    information, see the file `LICENSE' included with this distribution. */

/**
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package org.rexo.extraction;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.extract.Tokenization;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;
import edu.umass.cs.mallet.base.util.PropertyList;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.rexo.referencetagging.PstotextOutputFixer;
import org.rexo.span.CompositeSpan;
import org.rexo.util.EnglishDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

// Derived from the sources to 'StringTokenization'. Created to handle tokenization of
// new-format .html files.  Name is subject to sudden change.  -APD

// Preliminary support for parsing a "span file" has been added.
// activeSpanList holds for each and

public class NewHtmlTokenization extends TokenSequence implements Tokenization {
	private static final long serialVersionUID = 1L;

	// original: private static final Pattern LEXER_PATTERN_2 = Pattern.compile("\\w+[-]\\s*$|``|''|\\n|&amp;|1st|2nd|3rd|[4-9]th|Ph.D.|\\S+@\\S+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}" );
	// original: private static final Pattern LEXER_PATTERN_1 = Pattern.compile("``|''|\\n|&amp;|1st|2nd|3rd|[4-9]th|Ph.D.|\\S+@\\S+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}" );
	// private static final Pattern LEXER_PATTERN = Pattern.compile( "[^\\s,]+[\\s,]+" );

	public static final Pattern LEXER_PATTERN = Pattern.compile( "(``|''|\\n|&amp;|1st|2nd|3rd|[4-9]th|Ph.D.|\\S+@\\S+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}|^)\\s*" );
	// private static final Pattern LEXER_PATTERN = Pattern.compile("(``|''|\\n|1st|2nd|3rd|[4-9]th|Ph.D.|\\S+@\\S+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct})\\s*" );
	private static final Pattern DIGIT_OR_SPACE = Pattern.compile( "\\d+| " );

	private CharSequence _document;
	private List _lineSpans;

	// Used for training.  Move somewhere else?
	private List activeSpanList = new ArrayList();
	private HashMap labelsByName = new HashMap();

	private NewHtmlTokenization(CharSequence document, Span[] tokenSpans, List activeSpanList, List lineSpans) {
		this._document = document;
		// this.addAll( Arrays.asList(  tokenSpans ) );
		this.addAll( tokenSpans );
		this.activeSpanList = activeSpanList;
		this._lineSpans = lineSpans;
	}


	public NewHtmlTokenization(Document xmlDoc, EnglishDictionary dict, BufferedReader spanReader) throws IOException {
		this( xmlDoc, dict );
		//this.spanRegistry = new SpanRegistry(this);
		//spanRegistry.loadSpanFile(spanReader);
		activeSpanList = findActiveLabelList( spanReader );
	}

	/**
	 * set features to null, return true if they were not already null
	 * @return true if features were actually cleared
	 */
	public boolean clearTokenFeatures() {
		for (int i = 0; i < size(); i++) {
			Object o = get( i );
			if (o instanceof PropertyHolder) {
				PropertyHolder token = (PropertyHolder)o;
				if ( null != token.getFeatures() ) {
					token.setFeatures( null );
					return true;
				}
			}
		}
		return false;
	}


	private class ConstructionInfo {
		private String[] _fontNames;
		double _normalFontSize;
		double _largestFontSize;
		HashMap _headerFooterLineCounts;
		ArrayList _wrappedSpans;
		int fontNum = -1;
		int lastFontNum = -1;
		String fontName = "";
		String lastFontName = "";
		double fontSize = -1;
		double lastFontSize = -1;
		int textofs = 0;
		int globalLineNum = -1;
		private StringSpan _hyphToken;
		private StringSpan _preHyphToken;
		StringBuffer _docText = new StringBuffer();
		TreeSet _localDict = new TreeSet();
	}

	private ConstructionInfo _constructionInfo = null;

	/**
	 * Creates a tokenization of .
	 * <p/>
	 * Tokens are added from all the matches of the given lexer in each line, as well as in between the the end and start of two successive lines if de-hyphonization can be applied.
	 */
	private NewHtmlTokenization(Document xmlDoc, EnglishDictionary globalDict) {
		_constructionInfo = new ConstructionInfo();
		_constructionInfo._fontNames = findFontNames( xmlDoc );
		_constructionInfo._normalFontSize = findNormalFontSize( xmlDoc );
		_constructionInfo._largestFontSize = findLargestFontSize( xmlDoc );
		_constructionInfo._headerFooterLineCounts = initHeaderFooterLineCounts( xmlDoc );
		_constructionInfo._wrappedSpans = new ArrayList();
		_lineSpans = new ArrayList();

		_constructionInfo._hyphToken = null;
		_constructionInfo._preHyphToken = null;

		if (globalDict == null) {
			globalDict = EnglishDictionary.createDefault();
		}

		Element root = xmlDoc.getRootElement();
		tokenizePages( root );

		deHyphenateDocument( globalDict );
		this._document = _constructionInfo._docText.toString();
	}

	/**
	 *
	 * @param root
	 */
	private void tokenizePages(Element root) {
		List pageList = root.getChildren( "page" );
		Iterator pageI = pageList.iterator();


		// It appears that the new ps->text converter is often converting
		// '-' into high-ascii character 0xad, so we'll add special code to check for
		// this.  This gets converted into unicode '\u00ad' when loaded)
		while (pageI.hasNext()) {
			Element page = (Element)pageI.next();
			tokenizeLines( page );
		}
	}

	private Pattern trailingWS = Pattern.compile( "\\s+$" );

	/**
	 *
	 * @param page
	 */
	private void tokenizeLines(Element page) {
		int pageNum = Integer.parseInt( page.getAttributeValue( "n" ) );
		List lineList = page.getChildren( "line" );
		Iterator lineI = lineList.iterator();

		boolean firstTokenOnPage = true;

		for (int lineNum = 0; lineNum < lineList.size(); lineNum++) {
			Element line = (Element)lineI.next();
			CompositeSpan lineCompositeSpan = CompositeSpan.createSpan( _constructionInfo._docText );

			List tboxList = line.getChildren( "tbox" );

			// Increase the "lines since the start of the document" count.
			_constructionInfo.globalLineNum++;

			// Check for and skip pagination lines
			int lineStartOfs = _constructionInfo.textofs;

			Element firstTbox = (Element)tboxList.get( 0 );
			int llx = Integer.parseInt( firstTbox.getAttributeValue( "llx" ) );
			int lly = Integer.parseInt( firstTbox.getAttributeValue( "lly" ) );
			int fontNumber = Integer.parseInt(firstTbox.getAttributeValue("f"));
			boolean isTopOrBottomLine = (lineNum == 0 || lineNum == lineList.size() - 1);

			String lineText = lineText( line );

			if (isPaginationText( _constructionInfo._headerFooterLineCounts, lineText.toString(), isTopOrBottomLine, llx, lly, fontNumber )) {
				_constructionInfo._docText.append( lineText );
				_constructionInfo.textofs += lineText.length();
				StringSpan paginationToken = new StringSpan( _constructionInfo._docText, lineStartOfs, _constructionInfo.textofs ) ; 
				
				paginationToken.setNumericProperty("isHeaderFooterLine", 1);
				_lineSpans.add(paginationToken);
				continue;
			}

			// Tokenize this tbox
			Iterator tboxI = tboxList.iterator();
			tboxI = tboxList.iterator();
			boolean firstTokenInLine = true;
			while (tboxI.hasNext()) {
				Element tbox = (Element)tboxI.next();

				llx = Integer.parseInt( tbox.getAttributeValue( "llx" ) );
				lly = Integer.parseInt( tbox.getAttributeValue( "lly" ) );
				int urx = Integer.parseInt( tbox.getAttributeValue( "urx" ) );
				int ury = Integer.parseInt( tbox.getAttributeValue( "ury" ) );

				_constructionInfo.fontNum = Integer.parseInt( tbox.getAttributeValue( "f" ) );
				//fontSize = fontSizes[_constructionInfo.fontNum];
				_constructionInfo.fontSize = ury - lly;
				_constructionInfo.fontName = _constructionInfo._fontNames[_constructionInfo.fontNum];

				// Handle unusual output of totext -- see above
				String boxText = tbox.getText();
				PstotextOutputFixer pstotextOutputFixer = new PstotextOutputFixer();
				boxText = boxText.replace( '\u00ad', '-' );
				boxText = pstotextOutputFixer.cleanPsTotextOutput( boxText );

				_constructionInfo._docText.append( boxText );

				CharSequenceLexer lexer;

				lexer = new CharSequenceLexer( boxText, LEXER_PATTERN );

				boolean firstTokenInBox = true;

				while (lexer.hasNext()) {
					lexer.next();
					int spanStart = lexer.getStartOffset() + _constructionInfo.textofs;
					int spanEnd = lexer.getEndOffset() + _constructionInfo.textofs;
					StringSpan token = new StringSpan( _constructionInfo._docText, spanStart, spanEnd );
					String ttext = token.getText();
					Matcher twsMatcher = trailingWS.matcher( ttext );
					if (twsMatcher.find()) {
						int twsLen = twsMatcher.group().length();
						token.setText( ttext.substring( 0, ttext.length() - twsLen ) );
						if (twsLen > 1) {
							token.setNumericProperty( "trailing-ws-1", twsLen - 1 );
						}
					}
					else {
						token.setNumericProperty( "trailing-ws-1", -1 );
					}

					lineCompositeSpan.appendSpan( token );

					if (firstTokenInBox) {
						token.setNumericProperty( "firstInTextBox", 1 );
						firstTokenInBox = false;
					}

					// Combine with previously hyphenated token
					boolean termCombined = false;
					if (firstTokenInLine && _constructionInfo._hyphToken != null) {
						token.setNumericProperty( "split-rhs", 1 );
						_constructionInfo._wrappedSpans.add( new StringSpan[]{_constructionInfo._preHyphToken, _constructionInfo._hyphToken, token} );
						_constructionInfo._hyphToken = null;
						_constructionInfo._preHyphToken = null;
						termCombined = true;
					}

					// Set line number -- currently used to detect missing text when building HSpans
					token.setNumericProperty( "lineNum", _constructionInfo.globalLineNum );

					// Set font properties
					// Largest font in document property
					if (_constructionInfo.fontSize >= _constructionInfo._largestFontSize - 1 && _constructionInfo.fontSize > _constructionInfo._normalFontSize + 1) {
						token.setNumericProperty( "largestfont", 1 );
					}
					// Larger than body & smaller than largest property
					else if (_constructionInfo.fontSize > _constructionInfo._normalFontSize + 1) {
						token.setNumericProperty( "largefont", 1 );
					}
					// Smaller than body property
					else if (_constructionInfo.fontSize < _constructionInfo._normalFontSize - 1) {
						token.setNumericProperty( "smallfont", 1 );
					}
					// Font size change boundary
					// fixme: add smoothing?
					if (_constructionInfo.fontSize != _constructionInfo.lastFontSize) {
						token.setNumericProperty( "newfontsize", 1 );
					}
					// Font name change boundary
					if (_constructionInfo.fontName != _constructionInfo.lastFontName) {
						token.setNumericProperty( "newfontname", 1 );
					}
					// Font number change boundary
					if (_constructionInfo.fontNum != _constructionInfo.lastFontNum) {
						token.setNumericProperty( "newfontnumber", 1 );
					}

					token.setNumericProperty( "llx", llx );
					token.setNumericProperty( "lly", lly );
					token.setNumericProperty( "urx", urx );
					token.setNumericProperty( "ury", ury );
					token.setNumericProperty( "fontnumber", _constructionInfo.fontNum );
					token.setProperty( "fontname", _constructionInfo.fontName );

					// Handle new page property
					if (firstTokenOnPage) {
						token.setNumericProperty( "newpage", 1 );
						firstTokenOnPage = false;
					}

					// Handle newline property
					if (firstTokenInLine) {
						token.setNumericProperty( "newline", 1 );
						firstTokenInLine = false;
					}

					token.setNumericProperty( "pageNum", pageNum );

					// FIXME: move this somewhere else?
					_constructionInfo.lastFontNum = _constructionInfo.fontNum;
					_constructionInfo.lastFontSize = _constructionInfo.fontSize;
					_constructionInfo.lastFontName = _constructionInfo.fontName;

					// Check if the token is hyphenated and at the end of a line
					String tokenText = token.getText();
					boolean lineEndp = !lexer.hasNext() && !tboxI.hasNext();
					if (lineEndp && tokenText.trim().equals( "-" )) {
						if (size() > 1) {
							StringSpan lastToken = (StringSpan)this.get( this.size() - 1 );
							lastToken.setNumericProperty( "split-lhs", 1 );
							token.setNumericProperty( "split-hyphen", 1 );
							_constructionInfo._preHyphToken = lastToken;
							_constructionInfo._hyphToken = token;
						}
					}
					else {
						this.add( token );

						// Update the dictionary with this token
						if (!termCombined) {
							_constructionInfo._localDict.add( token.getText().toLowerCase() );
						}
					}

					// Check if a token was combined and is at the end of a line
					if (lineEndp && termCombined) {
						StringSpan phantomToken = new StringSpan( _constructionInfo._docText, spanStart, spanEnd );

						if (firstTokenOnPage) {
							phantomToken.setNumericProperty( "newpage", 1 );
							firstTokenOnPage = false;
						}
						phantomToken.setNumericProperty( "newline", 1 );
						phantomToken.setNumericProperty( "llx", llx );
						phantomToken.setNumericProperty( "lly", lly );
						phantomToken.setNumericProperty( "phantom", 1 );
						this.add( phantomToken );
					}
				}
				_constructionInfo.textofs += boxText.length();
			}

			_lineSpans.add( lineCompositeSpan );
		}
	}

	private void deHyphenateDocument(EnglishDictionary globalDict) {
		// De-hyphenate combined terms
		Iterator termI = _constructionInfo._wrappedSpans.iterator();
		while (termI.hasNext()) {
			StringSpan[] terms = (StringSpan[])termI.next();
			StringSpan lhs = terms[0];
			StringSpan hyphen = terms[1];
			StringSpan rhs = terms[2];
			String combinedText = lhs.getText() + rhs.getText();
			String normalText = combinedText.replaceAll( "\\W+", "" );
			normalText = normalText.trim().toLowerCase();
			if (globalDict.contains( normalText ) || _constructionInfo._localDict.contains( normalText )) {
				lhs.setNumericProperty( "invisible", 1 );
				lhs.setProperty( "original-text", lhs.getText() );
				lhs.setText( "" );
				hyphen.setNumericProperty( "invisible", 1 );
				hyphen.setProperty( "original-text", hyphen.getText() );
				hyphen.setText( "" );
				rhs.setProperty( "original-text", rhs.getText() );
				rhs.setText( combinedText );
			}
		}
	}

	private String[] findFontNames(Document xmlDoc) {

		Element root = xmlDoc.getRootElement();

		Element fonts = root.getChild( "fonts" );
		List fontList = fonts.getChildren( "font" );
		Iterator fontI = fontList.iterator();
		String fontNames[] = new String[fontList.size() + 1];
		while (fontI.hasNext()) {
			Element font = (Element)fontI.next();
			int num = Integer.parseInt( font.getAttributeValue( "n" ) );
			String name = font.getAttributeValue( "name" );
			fontNames[num] = name;
		}
		return fontNames;
	}

	// find the smallest, largest, and most common font sizes
	private double findNormalFontSize(Document xmlDoc) {
		HashMap counts = new HashMap();

		Element root = xmlDoc.getRootElement();
		List pageList = root.getChildren( "page" );
		Iterator pageI = pageList.iterator();
		while (pageI.hasNext()) {
			Element page = (Element)pageI.next();
			List lineList = page.getChildren( "line" );
			Iterator lineI = lineList.iterator();
			while (lineI.hasNext()) {
				Element line = (Element)lineI.next();
				List tboxList = line.getChildren( "tbox" );
				Iterator tboxI = tboxList.iterator();
				while (tboxI.hasNext()) {
					Element tbox = (Element)tboxI.next();
					//int _constructionInfo.fontNum = Integer.parseInt(tbox.getAttributeValue("f"));
					//Double _constructionInfo.fontSize = new Double(_constructionInfo.fontSizes[_constructionInfo.fontNum]);
					int lly = Integer.parseInt( tbox.getAttributeValue( "lly" ) );
					int ury = Integer.parseInt( tbox.getAttributeValue( "ury" ) );
					Double fontSize = new Double( ury - lly );

					int count = 1;
					if (counts.containsKey( fontSize )) {
						count += ((Integer)counts.get( fontSize )).intValue();
					}
					counts.put( fontSize, new Integer( count ) );
				}
			}
		}

		Double mostCommonSize = null;
		Integer maxCount = new Integer( -1 );
		Iterator sizeI = counts.keySet().iterator();
		while (sizeI.hasNext()) {
			Double size = (Double)sizeI.next();
			Integer count = (Integer)counts.get( size );
			if (count.compareTo( maxCount ) > 0) {
				maxCount = count;
				mostCommonSize = size;
			}
		}
		//System.out.println( "found most common size: " + mostCommonSize );
		if (mostCommonSize != null) {
			return mostCommonSize.doubleValue();
		}
		else {
			return -1;
		}
	}

	// find the smallest, largest, and most common font sizes
	private double findLargestFontSize(Document xmlDoc) {
		double largestSize = -1;
		Element root = xmlDoc.getRootElement();
		List pageList = root.getChildren( "page" );
		Iterator pageI = pageList.iterator();
		while (pageI.hasNext()) {
			Element page = (Element)pageI.next();
			List lineList = page.getChildren( "line" );
			Iterator lineI = lineList.iterator();
			while (lineI.hasNext()) {
				Element line = (Element)lineI.next();
				List tboxList = line.getChildren( "tbox" );
				Iterator tboxI = tboxList.iterator();
				while (tboxI.hasNext()) {
					Element tbox = (Element)tboxI.next();
					int lly = Integer.parseInt( tbox.getAttributeValue( "lly" ) );
					int ury = Integer.parseInt( tbox.getAttributeValue( "ury" ) );
					double fontSize = ury - lly;
					if (fontSize > largestSize) {
						largestSize = fontSize;
					}
				}
			}
		}
		return largestSize;
	}

	private String lineText(Element line) {
		StringBuffer lineText = new StringBuffer();

		List tboxList = line.getChildren( "tbox" );
		Iterator tboxI = tboxList.iterator();
		while (tboxI.hasNext()) {
			Element tbox = (Element)tboxI.next();
			String boxText = tbox.getText().replace( '\u00ad', '-' );
			lineText.append( boxText );
		}
		return lineText.toString();
	}

	private static String headerFooterNormalize(String string, boolean isTopOrBottomLine, int llx, int lly, int fontNum) {
		// strip all numbers and white space from the string
		// the following is (a more efficient) equivalent to doing string.replaceAll( "\\d+| ", "" );
		String ret = DIGIT_OR_SPACE.matcher( string ).replaceAll( "" );

		// A line needs to have the same position and font to match.
		ret = ret + " " + llx + "," + lly + "," + fontNum;
		
		return ret;
	}


	private HashMap initHeaderFooterLineCounts(Document xmlDoc) {
		HashMap counts = new HashMap();

		Element root = xmlDoc.getRootElement();
		List pageList = root.getChildren( "page" );
		Iterator pageI = pageList.iterator();

		while (pageI.hasNext()) {
			Element page = (Element)pageI.next();
			List lineList = page.getChildren( "line" );
			int lineNum = 0;

			Iterator lineI = lineList.iterator();
			while (lineI.hasNext()) {
				Element line = (Element)lineI.next();
				//if (lineNum == 0 || lineNum == lineList.size() - 1) {
				List tboxList = line.getChildren( "tbox" );
				Element firstTbox = (Element)tboxList.get( 0 );
				int llx = Integer.parseInt( firstTbox.getAttributeValue( "llx" ) );
				int lly = Integer.parseInt( firstTbox.getAttributeValue( "lly" ) );
				int fontNumber = Integer.parseInt(firstTbox.getAttributeValue("f"));
				boolean isTopOrBottomLine = (lineNum == 0 || lineNum == lineList.size() - 1);
				String lineText = lineText( line );

				//System.out.println( "got line text: " + lineText );
				String normalizedLine = headerFooterNormalize( lineText, isTopOrBottomLine, llx, lly, fontNumber );
				//System.out.println( "got normalized line: " + normalizedLine );
				int count = 1;
				if (counts.containsKey( normalizedLine )) {
					count += ((Integer)counts.get( normalizedLine )).intValue();
				}
				counts.put( normalizedLine, new Integer( count ) );
				
				//}
				lineNum++;
			}
		}
		return counts;
	}

	// Check if a line is a suspected header/footer line
	// For now, check if several similar lines have been seen elsewhere at the top/bottom of
	// pages in the document,
	private static boolean isPaginationText(HashMap counts, String s, boolean isTopOrBottomLine, int llx, int lly, int fontnumber) {

		String normalizedLine = headerFooterNormalize( s, isTopOrBottomLine, llx, lly, fontnumber );

		// To try to avoid false positives such as page ranges in
		// bibliographies, require that "middle" header/footer lines to
		// be skipped contain some alphabetical characters.
		// FIXME: is this too strict?
//		if (!isTopOrBottomLine && !normalizedLine.matches( ".*[a-zA-Z].*" ))
//			return false;

		int count = ((Integer)counts.get( normalizedLine )).intValue();
		return count >= 3;
	}


	/* This method initializes token labels for CRF training from an offset file,
	   giving each token a string property which specifies which labels are
	   in effect for it.  The offset file this method reads is created by running
	   the 'org.rexo.ui.TextAligner' program on human-labeled xml data in the old .xml
	   file format.

	   This method in conjunction with 'TextAligner' provides a hacky means of
	   training the CRF based on labeled data in the old XML file format.  As soon as
	   facilities for directly labeling training data in the new pstotext format are
	   put into place, this should no longer be necessary.
	  */
	public ArrayList findActiveLabelList(BufferedReader in) throws IOException {

		ArrayList ret = new ArrayList();
		LinkedList spanList = new LinkedList();
		LinkedList activeSpans = new LinkedList();
		String activeSpanNames = "";

		// Step 1 -- build a list of labels
		// (should come in sorted order in
		// the input file)
		// skip the first line, which holds the source filename
		in.readLine();

		String nextLine;
		while ((nextLine = in.readLine()) != null) {
			//System.out.println("read new line: "+nextLine);
			String[] tokens = nextLine.split( "\\s+" );
			//System.out.println("has "+tokens.length+" tokens");
			//MyTempSpan newSpan = new MyTempSpan();
			String label = tokens[0];
			int start = Integer.parseInt( tokens[1] );
			int end = Integer.parseInt( tokens[2] );
			StringSpan newSpan = new StringSpan( (CharSequence)this.getDocument(), start, end + 1 );
			newSpan.setProperty( "name", label );
			//System.out.println("adding new span, start="+start+" end="+end);
			spanList.add( newSpan );
		}

		// Step 2 -- build a list of strings (one for each token)
		// recording which labels are active.
		for (int i = 0; i < this.size(); i++) {
			StringSpan span = (StringSpan)this.getToken( i );
			int startIdx = span.getStartIdx();
			int endIdx = span.getEndIdx();
			boolean isChanged = false;
			boolean authorBegin = false;

			//System.out.println("read token :"+span.getText()+" start="+startIdx+" end="+endIdx);
			while (activeSpans.size() > 0 && ((StringSpan)activeSpans.getLast()).getEndIdx() <= startIdx) {
				StringSpan removed = (StringSpan)activeSpans.removeLast();
				removed.setNumericProperty( "lastTokenExclusive", i );
				getLabelsByName( (String)removed.getProperty( "name" ) ).add( removed );
				//this.addSpan(removed);
				//System.out.println("removing span '"+(String)removed.getProperty("name")+"'");
				isChanged = true;
			}
			// Consider each unassigned span
			Iterator spanI = spanList.iterator();
			while (spanI.hasNext()) {
				StringSpan next = (StringSpan)spanI.next();
				//System.out.println("considering starting span, start="+next.start+" end="+next.end);

				// Gotcha: it is possible for some spans not to overlap with any token,
				// so we must check for these:
				if (next.getEndIdx() <= startIdx) {
					// No tokens overlap with this span, so remove it
					spanI.remove();
				}
				else if (next.getStartIdx() <= startIdx && next.getEndIdx() > startIdx) {
					//next.firstToken = i;
					next.setNumericProperty( "firstTokenInclusive", i );
					activeSpans.add( next );
					spanI.remove();

					// Hack -- set the "author begin" property for this token
					if (((String)next.getProperty( "name" )).equals( "author" )) {
						span.setNumericProperty( "authorBeginning", 1 );
					}
					//System.out.println("adding span '"+next.label+"'");
					isChanged = true;
				}
				else {
					break;
				}
			}
			if (isChanged) {
				StringBuffer labelText = new StringBuffer();
				Iterator activeI = activeSpans.iterator();
				while (activeI.hasNext()) {
					StringSpan next = (StringSpan)activeI.next();
					if (labelText.length() == 0) {
						labelText.append( (String)next.getProperty( "name" ) );
					}
					else {
						labelText.append( ":" ).append( (String)next.getProperty( "name" ) );
					}
				}
				activeSpanNames = labelText.toString();
				//System.out.println("new label text: "+activeSpanNames);
			}
			ret.add( activeSpanNames );
		}
		// Process any remaining spans
		while (activeSpans.size() > 0) {
			//activeSpans.removeLast();
			StringSpan removed = (StringSpan)activeSpans.removeLast();
			removed.setNumericProperty( "lastTokenExclusive", this.size() );
			getLabelsByName( (String)removed.getProperty( "name" ) ).add( removed );
			//this.addSpan(removed);
			//System.out.println("removing span '"+removed.getLabel()+"'");
		}

		//activeSpanList = ret;
		return ret;
	}

	public List getLabelsByName(String name) {
		if (!labelsByName.containsKey( name )) {
			labelsByName.put( name, new ArrayList() );
		}
		return (List)labelsByName.get( name );
	}

	public List getSubtokenizationsByName(String name) {
		ArrayList ret = new ArrayList();
		Iterator spanI = getLabelsByName( name ).iterator();
		while (spanI.hasNext()) {
			StringSpan span = (StringSpan)spanI.next();
			int firstToken = (int)span.getNumericProperty( "firstTokenInclusive" );
			int lastToken = (int)span.getNumericProperty( "lastTokenExclusive" ) - 1;

			if (firstToken != 0 || lastToken != 0) {
				Span s1 = (Span)getToken( firstToken );
				Span s2 = (Span)getToken( lastToken );

				NewHtmlTokenization subTok = getSubspanTokenization( s1.getStartIdx(), s2.getEndIdx() );
				if (subTok.size() > 0) {
					ret.add( subTok );
				}
			}
		}
		return ret;
	}

	public List getLineSpans() {
		return _lineSpans;
	}

	public String getFormattedText() {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < _lineSpans.size(); i++) {
			Span span = (Span)_lineSpans.get( i );
			stringBuffer.append( span.getText() ).append( "\n" );
		}
		return stringBuffer.toString();
	}


	public NewHtmlTokenization getSubspanTokenization(int startIdx, int endIdx) {
		ArrayList subTokens = new ArrayList();
		ArrayList lines = new ArrayList();
		ArrayList subActiveSpans = new ArrayList();
		// ArrayList subActiveSpans = null;
		boolean foundStart = false;
		boolean foundEnd = false;

		for (int i = 0; i < size(); i++) {
			Span span = (Span)getToken( i );
			if (!foundStart) {
				if (startIdx <= span.getStartIdx()) {
					subTokens.add( span );
					foundStart = true;
					if (activeSpanList != null && activeSpanList.size() > 0) {
						subActiveSpans.add(activeSpanList.get(i));
					}
				}
			}
			else if (!foundEnd) {
				if (span.getStartIdx() >= endIdx) {
					foundEnd = true;
				}
				else {
					subTokens.add( span );
					if (activeSpanList != null && activeSpanList.size() > 0) {
						subActiveSpans.add(activeSpanList.get(i));
					}
				}
			}
			else {
				break;
			}
		}

		foundStart = false;
		foundEnd = false;
		for (int i = 0; i < _lineSpans.size(); i++) {
			Span span = (Span)_lineSpans.get( i );
			if (!foundStart) {
				if (startIdx <= span.getStartIdx()) {
					lines.add( span );
					foundStart = true;
				}
			}
			else if (!foundEnd) {
				if (span.getStartIdx() >= endIdx) {
					foundEnd = true;
				}
				else {
					lines.add( span );
				}
			}
			else {
				break;
			}
		}
		return new NewHtmlTokenization( _document, (Span[])subTokens.toArray( new Span[]{} ), subActiveSpans, lines );
	}

	public NewHtmlTokenization getTokenRangeTokenization(int firstToken, int lastToken) {
		ArrayList subTokens = new ArrayList();
		ArrayList lines = new ArrayList();
		ArrayList subActiveSpans = new ArrayList();

		for (int i = firstToken; i < lastToken; i++) {
			Span span = (Span)getToken( i );

			// Skip phantom tokens
			boolean phantomToken = (((PropertyHolder)span).getNumericProperty( "phantom" ) > 0);
			if (phantomToken) {
				continue;
			}
			subTokens.add( span );
		}

		CompositeSpan lineSpan = null;
		for (int i = 0; i < subTokens.size(); i++) {
			Span span = (Span)subTokens.get( i );
			boolean newLine = (((PropertyHolder)span).getNumericProperty( "newline" ) > 0);
			if (newLine || lines.isEmpty()) {
				lineSpan = CompositeSpan.createSpan( _document );
				lines.add( lineSpan );
			}
			lineSpan.appendSpan( span );
		}
		return new NewHtmlTokenization( _document, (Span[])subTokens.toArray( new Span[]{} ), subActiveSpans, lines );
	}

	// uesd for training
	public List getActiveSpanList() {
		return activeSpanList;
	}


	//xxx Refactor into AbstractTokenization
	public Span subspan(int firstToken, int lastToken) {
		StringSpan firstSpan = (StringSpan)getToken( firstToken );
		int startIdx = firstSpan.getStartIdx();

		int endIdx;
		if (lastToken > size()) {
			endIdx = _document.length();
		}
		else {
			StringSpan lastSpan = (StringSpan)getToken( lastToken - 1 );
			endIdx = lastSpan.getEndIdx();
		}

		return new StringSpan( _document, startIdx, endIdx );
	}


	public Span getSpan(int i) { return (Span)getToken( i ); }

	public Object getDocument() {
		return _document;
	}

	/**
	 * Method that removes all references this object has to the property list of each token. Can be used during HMM/CRF training time to save memory.
	 */
	public void cleanup() {
		labelsByName = null;
		activeSpanList = null;

		for (int i = 0; i < this.size(); i++) {
			StringSpan span = (StringSpan)this.getToken( i );  // one token
			span.setProperties( (PropertyList)null );
		}
	}

	// Test of read/write segmentation Element 
	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println( "Usage: NewHtmlTokenization .pstotext.xml .segmented.xml" );
			System.exit( 0 );
		}
		SAXBuilder builder = new SAXBuilder();
		Document pstoxmlDoc = builder.build( args[0] );
		EnglishDictionary dict = EnglishDictionary.createDefault();
		NewHtmlTokenization test = new NewHtmlTokenization( pstoxmlDoc, dict );
	}

	public static NewHtmlTokenization createNewHtmlTokenization(Document xmlDoc, EnglishDictionary globalDict) {return new NewHtmlTokenization( xmlDoc, globalDict );}

}

