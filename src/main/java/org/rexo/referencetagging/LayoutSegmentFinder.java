package org.rexo.referencetagging;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.util.EnglishDictionary;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: saunders Created Nov 9, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class LayoutSegmentFinder {
	private static Logger log = Logger.getLogger( LayoutSegmentFinder.class );

	static final private Pattern NULL_PATTERN = null;
	static final private Pattern INTRODUCTION_PATTERN;
	static final private Pattern ABSTRACT_PATTERN;
	static final private Pattern BIBLIOGRAPHY_PATTERN;

	static {
		INTRODUCTION_PATTERN = Pattern.compile( "I(?i:ntroduction)" );
		ABSTRACT_PATTERN = Pattern.compile( "A(?i:bstract)" );
		BIBLIOGRAPHY_PATTERN = Pattern.compile( "^[iIvVxX\\d\\.\\s]{0,5}(R(?i:eferences)|B(?i:ibliography))\\s*$" );
	}

	/* Partition element 'contentElement' into segments. */
	public Map markup(NewHtmlTokenization tokenization) throws HeaderNotFoundException, ReferencesNotFoundException, ReferenceParsingException {
		return getSubsections( tokenization );
	}


	/**
	 *
	 * @param tokenization
	 * @throws HeaderNotFoundException
	 * @throws ReferencesNotFoundException
	 * @throws ReferenceParsingException
	 */
	protected Map getSubsections(NewHtmlTokenization tokenization) throws HeaderNotFoundException, ReferencesNotFoundException, ReferenceParsingException {
		Map subsections = new HashMap();
		ArrayList lineSpans = new ArrayList();
		lineSpans.addAll( tokenization.getLineSpans() );

		//**** Find header ****
		LinkedList headerLineList = new LinkedList();
		// look for 'abstract'
		List subList = findMatchingLines( lineSpans, NULL_PATTERN, ABSTRACT_PATTERN, /*lineCountMax=*/Integer.MAX_VALUE, /*pageCountMax=*/Integer.MAX_VALUE );
		if (! subList.isEmpty()) {
			// add everything before 'abstract' to header list
			headerLineList.addAll( subList );
			subList.clear();
			// found 'abstract', now look for 'introduction' or 1-page limit, whichever comes first
			subList = findMatchingLines( lineSpans, ABSTRACT_PATTERN, INTRODUCTION_PATTERN, /*lineCountMax=*/Integer.MAX_VALUE, /*pageCountMax=*/1 );
			headerLineList.addAll( subList );
			subList.clear();
		}
		else {
			// no 'abstract': check for 'introduction' (if this fails, this paper should never have passed the 'is a paper test' from pstotext)
			subList = findMatchingLines( lineSpans, NULL_PATTERN, INTRODUCTION_PATTERN, /*lineCountMax=*/Integer.MAX_VALUE, /*pageCountMax=*/Integer.MAX_VALUE );
			if (! subList.isEmpty()) {
				headerLineList.addAll( subList );
				subList.clear();
			}
			else {
				throw new HeaderNotFoundException( "did not find 'abstract' or 'introduction'" );
			}
		}

		// Create header element
		long[] headerTokenBoundaries = lineListBoundaries( headerLineList );
		NewHtmlTokenization header = tokenization.getSubspanTokenization( (int)headerTokenBoundaries[0], (int)headerTokenBoundaries[1] );

		subsections.put( "headerTokenization", header );


		//***** Find body ****
		ArrayList bodyLines = new ArrayList();
		subList = findMatchingLines( lineSpans, NULL_PATTERN, BIBLIOGRAPHY_PATTERN, /*lineCountMax=*/Integer.MAX_VALUE, /*pageCountMax=*/Integer.MAX_VALUE );
		while (!subList.isEmpty()) {
			// this will find the *last* occurrance of the references/bibliography marker in the paper
			bodyLines.addAll( subList );
			subList.clear();
			subList = findMatchingLines( lineSpans, BIBLIOGRAPHY_PATTERN, BIBLIOGRAPHY_PATTERN, /*lineCountMax=*/Integer.MAX_VALUE, /*pageCountMax=*/Integer.MAX_VALUE );
		}

		if (!bodyLines.isEmpty()) {
			// Create body element
			long[] bodyTokenBoundaries = lineListBoundaries( bodyLines );
			NewHtmlTokenization body = tokenization.getSubspanTokenization( (int)bodyTokenBoundaries[0], (int)bodyTokenBoundaries[1] );
			subsections.put( "bodyTokenization", body );
		}
		else {
			throw new ReferencesNotFoundException( "did not find reference section" );
		}


		// Extract References
		// skip the first line ('References' or 'Bibliography')
		if (!lineSpans.isEmpty()) {
			lineSpans.remove( 0 );
		}
		BibliographyLayoutSegmenter refExtractor = new BibliographyLayoutSegmenter( tokenization );
		BibliographyLayoutSegmenter.ReferenceData referenceData = refExtractor.extractReferences( lineSpans );

		// Create biblioPrologue element
		if (! referenceData.prologueList.isEmpty()) {
			long[] prologueTokenBoundaries = lineListBoundaries( referenceData.prologueList );
			NewHtmlTokenization prologue = tokenization.getSubspanTokenization( (int)prologueTokenBoundaries[0],
			                                                                    (int)prologueTokenBoundaries[1] );

			subsections.put( "prologueTokenization", prologue );
		}

		// Create reference elements
		LinkedList referencesList = referenceData.referenceLineList;

		LinkedList refTokenizationList = new LinkedList();
		while (!referencesList.isEmpty()) {
			List referenceLine = (List)referencesList.removeFirst();
			long[] referenceTokenBoundaries = lineListBoundaries( referenceLine );
			NewHtmlTokenization reference = tokenization.getSubspanTokenization( (int)referenceTokenBoundaries[0],
			                                                                     (int)referenceTokenBoundaries[1] );
			refTokenizationList.add( reference );

		}
		subsections.put( "referenceList", refTokenizationList );

		if (! referenceData.epilogueList .isEmpty()) {
			long[] epilogueTokenBoundaries = lineListBoundaries( referenceData.epilogueList );
			NewHtmlTokenization epilogue = tokenization.getSubspanTokenization( (int)epilogueTokenBoundaries[0],
			                                                                    (int)epilogueTokenBoundaries[1] );
			subsections.put( "epilogueTokenization", epilogue );
		}
		return subsections;
	}

	private List findMatchingLines(List lineSpans, Pattern beginPattern, Pattern endPattern, int lineCountMax, int pageCountMax) {
		int lineCount = 0;
		int pageCount = 1;
		boolean foundBegin = beginPattern == null;
		boolean foundEnd = endPattern == null;
		int docLineCount = lineSpans.size();
		int subListStart = foundBegin ? 0 : docLineCount;
		int subListEnd = foundEnd ? docLineCount : 0;

		for (int i = 0; i < lineSpans.size(); i++) {
			Span lineSpan = (Span)lineSpans.get( i );
			String text = lineSpan.getText();
			lineCount++;
			if (i > 0 && isNewPage( (PropertyHolder)lineSpan )) {
				pageCount++;
			}

			if (! foundBegin) {
				Matcher matchObject = beginPattern.matcher( text );
				if (matchObject.find()) {
					foundBegin = true;
					subListStart = i;
					lineCount = 0;
					pageCount = 0;
				}
			}
			else if (! foundEnd) {
				Matcher matchObject = endPattern.matcher( text );
				if (matchObject.find() || lineCount > lineCountMax || pageCount > pageCountMax) {
					foundEnd = true;
					subListEnd = i;
				}
			}
			else {
				break;
			}
		}
		return lineSpans.subList( subListStart, subListEnd );
	}

	private boolean isNewPage(PropertyHolder span) {
		return span.getNumericProperty( "newpage" ) > 0.0;
	}


	private long[] lineListBoundaries(List lineList) {
		Span start = (Span)lineList.get( 0 );
		Span end = (Span)lineList.get( lineList.size() - 1 );
		long startToken = start.getStartIdx();
		long endToken = end.getEndIdx();
		return new long[]{startToken, endToken};
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println( "Usage: NewHtmlTokenization *.pstotext.xml" );
			System.exit( 0 );
		}
		SAXBuilder builder = new SAXBuilder();
		Document pstoxmlDoc = builder.build( args[0] );
		LayoutSegmentFinder layoutSegmentFinder = new LayoutSegmentFinder();

		EnglishDictionary dict = EnglishDictionary.createDefault();

		NewHtmlTokenization test = NewHtmlTokenization.createNewHtmlTokenization( pstoxmlDoc, dict );
		String formattedText = test.getFormattedText();

		Map map = layoutSegmentFinder.markup( test );
		Iterator iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			Object o = iterator.next();

		}
	}
}
