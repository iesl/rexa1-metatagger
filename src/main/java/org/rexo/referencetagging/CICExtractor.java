package org.rexo.referencetagging;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.extract.StringSpan;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.rexo.extraction.NewHtmlTokenization;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Author: saunders Created Nov 17, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */

/* Class responsible for identifying occurences of citations within
 * the document body.
 */

/* Examples:

   Tag Style:
   [1, 3, 15]

   Date Style:
   (Grossberg and Kuperstein, 1986), (Kawato and Gomi, 1991; Miall, Malkus and Robertson, 1996),
   (e.g., Robinson, Houk, and Gibson, 1987), and Grossberg, (1993a, 1993b)
   (cf. Jessen 1994).

   Unsupported examples:
   "in Refs. 1 and 3"
   first noticed by Citron and Rudolph. [[superscript]]24
*/


public class CICExtractor {

	private static Logger log = Logger.getLogger( CitationContextFilter.class );

	/* Important: these are ordered in increasing order of preference */
	private final static int NUMBER_STYLE = 0;
	private final static int WEAK_TAG_STYLE = 1;
	private final static int TAG_STYLE = 2;
	private final static int DATE_STYLE = 3;
	private final static int MAX_STYLE = DATE_STYLE + 1;
	private final static String validTagRegex = "(.*\\[[\\w\\s,;\\.]+\\].*)|" +
	                                            "(\\d+\\.)";

	private List referenceList;
	private HashMap[] refData;

	CICExtractor(List referenceList) {
		assert (referenceList.size() > 0);
		this.referenceList = referenceList;
		refData = new HashMap[referenceList.size()];
		findRefData();
	}


	/**
	 *
	 * @param p
	 * @param region
	 */
	private ArrayList findRegexMatchesInRegion(Pattern p, NewHtmlTokenization region) {
		ArrayList ret = new ArrayList();

		List lineSpans = region.getLineSpans();
		for (int i = 0; i < lineSpans.size(); i++) {
			Span span = (Span)lineSpans.get( i );
			String sourceText = span.getText();

			Matcher matcher = p.matcher( sourceText );

			while (matcher.find()) {
				ret.add( new StringSpan( (CharSequence)span.getDocument(), span.getStartIdx() + matcher.start( 1 ), span.getStartIdx() + matcher.end( 1 ) ) );
			}
		}
		return ret;
	}

	/* Strips a reference tag of it's leading/trailing characters (currently [] or '.').
	   Additionally removes any whitespace directly inside brackets. */
	private String stripTag(String tag) {
		if (tag.charAt( 0 ) == '[') {
			assert(tag.charAt( tag.length() - 1 ) == ']');
			// Trim off []s and remove any internal spacing
			return tag.substring( 1, tag.length() - 1 ).trim();
		}
		else {
			if (!tag.matches( "\\d+\\." )) {
				log.error( "Erroneous ref-marker: " + tag );
			}
			// Strip off the '.' after the number.
			return tag.substring( 0, tag.length() - 1 );
		}
	}


	/* Searches body for a match of 'refTag' in brackets */
	// Old comments follow (I don't know if they still apply or not, probably safe to delete):
	// 1. TODO: recognize [1, pp. 405-410]? (see http:##www-cgi.cs.cmu.edu#afs#cs.cmu.edu#user#droh#mosaic#papers#csapp.sigcse01.pdf.pp.tagged.xml)
	// 2. String regex = "("+"(\\[(?:[\\w\\.]+\\s*,\\s*)*("+strippedTag+")(?:\\s*,\\s*[\\w\\.]+)*\\])"+"|"+

	private String tagRegex(String refTag, int refNum) {
		//log.debug("in findTagContext");
		assert(refTag.matches( validTagRegex ));
		String strippedTag = stripTag( refTag );
		//log.debug("strippedTag is "+strippedTag);

		// Bracket followed by some number of letters,numbers,spaces,punctuation, followed by space followed by target followed by space
		// followed by some number of leters,numbers,spaces,punctuation, followed by closing bracket.
		// TODO: make more readable
		String regex = "(?:\\[(?:[\\w\\s,;\\.\\-]+[\\s,;])?(" + strippedTag + ")(?:[\\s,;][\\w\\s,;\\.\\-]*)?\\])";

		return regex;
	}

	/* Hack -- Sometimes '[' and ']' are incorrectly converted to '#' (the default for "unknown character"
	   in the pstotext process), so we'll allow '#' in place of '[',']' to handle papers with this defective conversion.
	   Since it is conceivable that # shows up in other contexts, we have to be careful to guard against invalid matches.
	   Therefore, this regex is slightly more strict than the regular "tagRegex" about what characters it allows inside its boundary '#' characters,
	   to hopefully avoid invalid matches.  */
	private String weakTagRegex(String refTag, int refNum) {
		//log.debug("in findTagContext");
		assert(refTag.matches( validTagRegex ));
		String strippedTag = stripTag( refTag );
		//log.debug("strippedTag is "+strippedTag);

		/* Searches body for a match of 'refTag' in '#'*/
		String regex = "(?:\\#(?:[\\w\\.]+\\s*,\\s*)*(" + strippedTag + ")(?:\\s*,\\s*[\\w\\.]+)*\\#)";

		return regex;
	}

	/* This is an old regex type from my early code.  The basic idea is
           that documents are identified by their reference number instead
	   of by ref-markers.  I don't know whether this is useful or not; it is conceivable
	   that it should be removed.
 	  */
	/* Searches body for exact match of [num] */
	/* Note: does not handle [1-3] yet */
	private String numberRegex(int refNum) {
		//log.debug("in findNumContext");

		// Build regex for matching numbers
		// (numbering starts at one)
		String regex = "\\[(?:\\d+\\s*,\\s*)*(" + (refNum + 1) + ")(?:\\s*,\\s*\\d+)*\\]";

		return regex;
	}

	/* Searches body for a citation of the author last name/year style.
           e.g. Chen (2000), Chen and Hovy, (2000), Chen et. al, 2000, Chen 2000;2004 */
	private String dateRegex(LinkedList names, String year, int refNum) {
		//log.debug("in findDateContext");

		StringBuffer regex = new StringBuffer();
		Iterator nameI = names.iterator();
		String name;

		if (!nameI.hasNext())
			return null;

		// Beginning paren for the expression
		regex.append( "(" );

		// Start with first author name
		name = (String)nameI.next();
		regex.append( name );

		// Follow by other authors or 'et al' if more authors are present
		// FIXME: maybe more parens than necesary are used here
		if (nameI.hasNext()) {
			regex.append( "(?:(?:(?:\\s|,)*et al\\.?)|(?:" );
			while (nameI.hasNext()) {
				regex.append( "(?:\\s|,)*(?:(?:and|&)(?:\\s|,)*)?" );
				// It is assumed that 'name' is alphanumeric
				regex.append( nameI.next() );
			}
			regex.append( "))" );
		}
		// After the author portion of the regex comes the date matching portion.
		// It is slightly convoluted to allow proper matching of authors followed
		// by multiple years, e.g. "Jones 1993, 1995, 1997"

		// spaces + optional '(' + (year + space)*
		regex.append( "(?:\\s|,)*\\(?(?:\\d+(?:\\s|,)*)*" );

		// Year of the particular reference we are searching for
		regex.append( year );

		// Closing parenthesis around the year (included for purely aesthetic reasons)
		regex.append( "\\)?" );

		// Finish enclosing parens for entire expression
		regex.append( ")" );

		return regex.toString();
	}

	/* This method scans a list of references marked up by the CRF and
	   finds the relevant information (reference marker text, list of author last names, year)
 	   that will be used by later methods to produce regular expressions that match citations
	   in context for each reference.
	   This is an initialization step that should be run before any of the regex generating methods are run.
 	*/
	public void findRefData() {
		// For each reference
		for (int refNum = 0; refNum < referenceList.size(); refNum++) {
			this.refData[refNum] = new HashMap();
			HashMap data = this.refData[refNum];

			if (refNum >= referenceList.size())
				throw new IllegalArgumentException( "Ref num out of range: '" + refNum + "'" );

			Element refSpans = (Element)referenceList.get( refNum );

			// Extract the reference marker if one is present
			String marker;
			Element markerSpans = refSpans.getChild( "ref-marker" );
			if (markerSpans != null) {
				String markerText = markerSpans.getText().trim();
				if (markerText.matches( validTagRegex )) {
					data.put( "marker", markerText );
				}
			}

			// Extract the list of authors for this reference
			List authorList = new LinkedList();

			Element authSpan = refSpans.getChild( "authors" );
			if (authSpan != null) {
				Iterator lastNameIter = authSpan.getDescendants( new Filter() {
					public boolean matches(Object object) {
						return object instanceof Element
						       && ((Element)object).getName().equals( "author-last" );
					}
				} );
				// List lastNames = (authSpan == null) ? null :

				//log.info("got "+(lastNames == null ? "0" : Integer.toString(lastNames.size()))+" elements.");
				while (lastNameIter.hasNext()) {
					Element nameS = (Element)lastNameIter.next();
					String lastName = nameS.getText()
							.replaceAll( "[.,:]", "" )
							.trim();
					if (lastName.matches( "[\\w\\-\\s']+" )) {
						authorList.add( lastName );
					}
					else {
						//log.info("invalid last name: "+lastName);
					}
				}
			}

			data.put( "authors", authorList );

			// Extract the year for this reference
			Element dateSpan = (Element)refSpans.getChild( "date" );
			if (dateSpan != null) {
				/* Get the date text */
				String date = dateSpan.getText();
				String year = null;

				/* Fix this in 9999. :) */
				Pattern pattern = Pattern.compile( "\\d{4}[a-f]?" );
				Matcher matcher = pattern.matcher( date );

				if (matcher.find()) {
					year = matcher.group();
				}
				// Hack -- search for a two digit year.  I'm not sure if these
				// actually exist or not.  Maybe this should be removed.
				else {
					pattern = Pattern.compile( "\\d{2}" );
					matcher = pattern.matcher( date );
					if (matcher.find()) {
						year = matcher.group();
					}
				}
				if (year != null) {
					data.put( "year", matcher.group() );
					//log.debug("extracted year: "+matcher.group());
				}
				else {
					//log.debug("did not find year, date="+date);
				}
			}
		}
	}

	/* Generates a regular expression that will search for a citation-in-context of
	the given reference in the specified citation style.
	 */
	private String styleRegex(int citationStyle, int refNum) {

		HashMap data = refData[refNum];
		// Search for all matching strings in the document body

		// FIXME: this could probably be refactored
		String refTag;
		switch (citationStyle) {
			case TAG_STYLE:
				refTag = (String)data.get( "marker" );
				if (refTag != null)
					return tagRegex( refTag, refNum );
				else
					return null;

			case WEAK_TAG_STYLE:
				refTag = (String)data.get( "marker" );
				if (refTag != null)
					return weakTagRegex( refTag, refNum );
				else
					return null;

			case NUMBER_STYLE:
				return numberRegex( refNum );

			case DATE_STYLE:
				LinkedList authors = (LinkedList)data.get( "authors" );
				String year = (String)data.get( "year" );
				if (authors != null && year != null)
					return dateRegex( authors, year, refNum );
				else
					return null;
			default:
				throw new IllegalArgumentException( "unknown citation style: '" + citationStyle + "'" );
		}
	}


	/* This method finds all citations in context occuring in 'sourceRegion'.
	   For each citation in context found, it adds an 'HSpans' element to a result list.
	   The 'HSpans' element has three children, corresponding to the text before, during,
	   and after the citation in context that was found.
	   Currently, this method throws an exception if citations for at least 2/3 of the references are
	   not found and returns the result list otherwise.
	 */
	public ArrayList findCitationsInContext(NewHtmlTokenization tokenization /*, StringMultiSpan sourceRegion */) throws CitationFinderException {
		ArrayList ret = new ArrayList();
		int citationStyle;

		// Try each citation style in decreasing order of preference,
		// starting with 'DATE_STYLE', and ending with 'NUMBER_STYLE'
		for (citationStyle = MAX_STYLE - 1; citationStyle > -1; citationStyle--) {
			int numFailures = 0;
			int refNum;

			// For this citation style, try to find matches for each reference
			for (refNum = 0; refNum < referenceList.size(); refNum++) {
				// Build a regex to detect citations in context for this particular reference
				String regex = styleRegex( citationStyle, refNum );
				if (regex != null) try {
					Pattern cicPattern = Pattern.compile( regex );
					// Attempt to find matches in context
					ArrayList matches = null;
					if (regex != null) {
						matches = findRegexMatchesInRegion( cicPattern, tokenization );
					}
					else {
						matches = null;
					}
					// Check for a matching failure
					if (matches == null || matches.size() == 0) {
						numFailures++;
						// If 2/3 of the references have failed to produce any matches, move on to the next
						// citation style.
						if ((numFailures * 3 > referenceList.size() * 2)) {
							ret.clear();
							break;
						}
						continue;
					}

					// Add an 'HSpans' object to the result list for each match found
					Element refSpans = (Element)referenceList.get( refNum );
					for (int i = 0; i < matches.size(); i++) {
						Span matchSpan = (StringSpan)matches.get( i );
						Span contextWindow = new StringSpan( (CharSequence)tokenization.getDocument(), Math.max( matchSpan.getStartIdx() - 200, 0 ),
						                                     Math.min( matchSpan.getEndIdx() + 200, ((CharSequence)tokenization.getDocument()).length() ) );
						Span preMatchSpan = new StringSpan( (CharSequence)tokenization.getDocument(), contextWindow.getStartIdx(), matchSpan.getStartIdx() );
						Span postMatchSpan = new StringSpan( (CharSequence)tokenization.getDocument(), matchSpan.getEndIdx(), contextWindow.getEndIdx() );
						Element cicElement = new Element( "CitationContext" );
						Element pre = new Element( "PreMatch" );
						Element match = new Element( "Match" );
						Element post = new Element( "PostMatch" );
						pre.addContent( preMatchSpan.getText() );
						match.addContent( matchSpan.getText() );
						post.addContent( postMatchSpan.getText() );
						cicElement.addContent( pre )
								.addContent( match )
								.addContent( post );
						cicElement.setAttribute( "refersToRefID", refSpans.getAttributeValue( "refID" ) );
						ret.add( cicElement );
					}
				}
				catch (PatternSyntaxException e) {
					log.info( "could not derive ref-marker regex" );
				}
			}
		}
		return ret;
	}
}
