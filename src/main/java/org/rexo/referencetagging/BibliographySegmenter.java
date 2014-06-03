/* A program to Extract the references from Research papers. */
package org.rexo.referencetagging;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BibliographySegmenter {

	private ArrayList reference_start_patterns;

	private static final int maxReferenceLength = 3200;


	public BibliographySegmenter() {
		reference_start_patterns = new ArrayList();
		initRegexPatterns();
	}

	private class ReferenceHint {
		int listIndex;

		ReferenceHint(int index) {
			listIndex = index;
		}
	}

	private class ReferenceSectionEndHint extends ReferenceHint {
		ReferenceSectionEndHint(int index) {
			super( index );
		}
	}

	private class ReferenceHintList {
		private LinkedList list = new LinkedList();

		public void add(ReferenceHint r) {
			list.add( r );
		}
		public ReferenceHint remove(int i) {
			return (ReferenceHint)list.remove(i);
		}

		public ReferenceHint get(int i) {
			return (ReferenceHint)list.get( i );
		}

		public Iterator iterator() {
			return list.iterator();
		}

		public int size() {
			return list.size();
		}
	}

	private class RegexReferenceHintList extends ReferenceHintList {
		static final int invalidRegexNum = -1;
		int regexNum = invalidRegexNum;
	}

	private class HorizReferenceHintList extends ReferenceHintList {
	}

	private class VerticalReferenceHintList extends ReferenceHintList {
	}

	private class AlphabetReferenceHintList extends ReferenceHintList {
	}

	private void initRegexPatterns() {
		// 1. References that start with "[....]"
		String reString = "^\\s*\\[[^\\]]+\\]";
		this.reference_start_patterns.add( Pattern.compile( reString ) );

		// References that start with <author> <date> patterns. Various ways of doing this.
		// 2. "Corrada, A., 1984" or "Corrada, A.E., 1984" or "Corrada, A., Emmanuel, A., 1984"
		reString = "^\\s*                   # Possible whitespace at beginning\n" +
		           "(                              # Grouping for authors\n" +
		           " [A-Z]\\w+\\s+([A-Z]\\.?\\s*)+ # Author name pattern: 'Corrada, A.' or 'Corrada, A.E.'\n" +
		           " ,\\s+                         # authors separated by comma\n" +
		           ")+                             # possibly multiple authors\n" +
		           "\\d{4}                         # publication year";

		this.reference_start_patterns.add( Pattern.compile( reString, Pattern.COMMENTS ) );
		// 3. "A. Corrada. 1984." or "Andres Corrada. 1984."
		// example: s/www.dcs.shef.ac.uk/http:\\www.dcs.shef.ac.uk\spandh\projects\m4\pdf\jovanovic04_SIGDial.pdf.xml
		/* Has problems with: l/www.comp.leeds.ac.uk/http:\\www.comp.leeds.ac.uk\eric\icamejournal2000.ps.xml
		*/

		reString = "^\\s*                # Start with possible whitespace\n" +
		           "([A-Z](\\.|\\w+)\\s*){1,6}  # Pattern for a name: 'A. Corrada' or 'Andres Corrada'\n" +
		           "(                           # grouping for authors\n" +
		           " (\\,\\s+|\\,\\s+and\\s+)+  # separator between succesive names: ', ' or ', and '\n" +
		           " ([A-Z](\\.|\\w+)\\s*){1,6} # additional authors\n" +
		           ")*                          # end of grouping for authors\n" +
		           "\\.                         # Authors end with a period\n" +
		           "\\s+\\d{4}\\.               # publication year: '1984.'\n";

		this.reference_start_patterns.add( Pattern.compile( reString, Pattern.COMMENTS ) );
		// 4. Another "<author> <date>" style: m/www.cs.mcgill.ca/http:\\\\www.cs.mcgill.ca\\~denis\\smallmajdepth.ps.xml
		// Basically the year is wrapped in parentheses and can end with a period or comma.
		// Ash, C., Farrow, J. A. E., Wallbanks, S. & Collins, M. D. (1991).
		/* I've made a couple tweaks to this regex to help it match more cases.  Notably,
		I added '&' as a substitute for 'and' and made the regex more lenient in that
		it no longer cares about what follows the year.
		*/
		/* Has problems with: m/calli.matem.unam.mx/http:\\calli.matem.unam.mx\EMIS\journals\DMJDMV\xvol-icm\18\Niss.MAN.ps.gz.xml
		Has problems with: b/www.compsci.bristol.ac.uk/http:\\www.compsci.bristol.ac.uk\%7Eflach\papers\UKCI03-concavities.pdf.xml
		Has problems with: g/www.cc.gatech.edu/http:\\www.cc.gatech.edu\elc\palaver\papers\pt-cscl02-long.pdf.xml (O'Neill)
		Has problems with: s/www-flash.stanford.edu/ftp:\\www-flash.stanford.edu\pub\hive\TOMACS96-simos.ps.xml
		Has problems with: m/math.muni.cz/http:\\math.muni.cz\EMIS\journals\EJP-ECP\EjpVol8\paper5.ps.xml
		*/
		reString = "^\\s*                              # Start with possible whitespace\n" +
		           "(                                         # Grouping for authors\n" +
		           " [A-Z][\\w\\-]+,?\\s+[A-Z]\\.(\\s*-?[A-Z]\\.)*  # Names are of the form: 'Corrada, A.' or 'Corrada, A.-E.'\n" +
		           " (\\,\\s+|\\,?\\s+(and|&)\\s+)*                # Names are separated by: ', ' or ', and '\n" +
		           ")+                                        # Multiple authors possible\n" +
		           "\\s+\\(?\\d{4}                 # Year appears as '(1984),' or '(1984).'";
		this.reference_start_patterns.add( Pattern.compile( reString, Pattern.COMMENTS ) );

		// 5. Another pattern: '1)', '2)', etc.
		reString = "^\\s*\\d+\\)\\s+[A-Z]";
		this.reference_start_patterns.add( Pattern.compile( reString ) );

		// 6. References that start with "1.", etc.
		// Parens are important (used for capturing the lead number)

		// Previously we required a space after numbers;  it appears that occasionally spaces get lost in 
		// 2xml conversion and so this has been made more lenient.  (Since we require ascending numbers
		// we shouldn't have to worry too much about invalid matches) 
		////reString = "^\\s*(\\d{1,3})\\.\\s+";
		reString = "^\\s*(\\d{1,3})\\..+";
		this.reference_start_patterns.add( Pattern.compile( reString ) );

		// Pack this.reference_start_patterns to be memory efficient
		this.reference_start_patterns.trimToSize();

	}


	/* Auxillary method used to advance the list iterator of content
	 *  past the biblioprologue to the first line of reference text.
	 *  This has been assigned its own method since it appears that a few
	 *  papers (see for example, http://www.auto.tuwien.ac.at/Projects/SynUTC/documents/ptti99.ps)
	 *  have multiple lines in the biblioPrologue. 
	 */
	// TODO: change segmentation so the bibliography is searched for from the end;  this should make 
	// this method obsolete.
	private boolean advancePastPrologue(ListIterator lineIterator) {

		final String bodyEnd = SegmentationFinder.bodyEnd;

    		Pattern bodyEndPattern = Pattern.compile( bodyEnd, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS );

		while (lineIterator.hasNext()) {
			MyTextObject textObject = (MyTextObject)lineIterator.next();
			if (textObject instanceof TextLine) {
				String lineText = ((TextLine) textObject).getText();
				Matcher m = bodyEndPattern.matcher(lineText);
				// Check for non-prologue text
				if (!m.find()) {
					// Back up one so the non-prologue text will be next returned.
					lineIterator.previous();
					return true;
				}
			}
		}

		return false;
	}

	/*  Uses regular expressions to mark references in lineList in the
	 *  same way as before.
	 */
	private RegexReferenceHintList regexReferenceHintList(List lineList) {
		RegexReferenceHintList ret = new RegexReferenceHintList();
		ListIterator lineIterator = lineList.listIterator();
		int lineIndex;

		// Skip the 'References' marker
		//TextLine refMarker = (TextLine)lineIterator.next();
		////System.out.println("refMarker: "+refMarker.getText());
		if (!advancePastPrologue(lineIterator)) {
			return null;
		}
		TextLine firstLine = (TextLine) lineIterator.next();

		Pattern referencePatternUsed = null;
		int regexNum = 1;

		/*
		while (lineIterator.hasNext()) {
			MyTextObject textObject = (MyTextObject)lineIterator.next();
			lineIndex++;
			if (textObject instanceof TextLine) {
				firstLine = (TextLine)textObject;
				break;
			}
		}
		if (firstLine == null) {
			return null;
		}
		*/

		////System.out.println("got first line "+firstLine.getText());

		/* Find the first reference pattern matching the first line */
		Iterator patternIterator = reference_start_patterns.iterator();
		boolean foundRegex = false;
		while (patternIterator.hasNext()) {
			referencePatternUsed = (Pattern)patternIterator.next();
			Matcher currentMatcher = referencePatternUsed.matcher( firstLine.getText() );

			boolean isMatch = false;
			if (currentMatcher.find()) {
				// Require the first match to start with '1' to count as a valid match
				// for regex #6
				if (regexNum == 6) {
					int lineNum = Integer.parseInt( currentMatcher.group( 1 ) );
					if (lineNum == 1) {
						isMatch = true;
					}
				}
				else {
					isMatch = true;
				}
			}

			if (isMatch) {
				////System.out.println("line" + firstLine.getText() + "matches regex"+regexNum);
				ret.regexNum = regexNum;
				foundRegex = true;
				break;
			}
			regexNum++;
		}
		// Did we get a match? If not, return false
		if (!foundRegex) {
			////System.out.println("no matching regex found");
			return null;
		}

		// Start from the beginning again
		lineIterator = lineList.listIterator();
		// skip the 'References' marker
		/*
		lineIndex = -1;
		refMarker = (TextLine)lineIterator.next();
		lineIndex++;
		*/
		if (!advancePastPrologue(lineIterator)) {
			return null;
		}
		lineIndex = lineIterator.previousIndex();

		// add hints
		int refSize = 0;

		// Keeps track of the previously seen reference number;  Used by regex #6
		// to help determine whether a match should be counted as the start of a new reference.
		int prevRefNum = 0;

		while (lineIterator.hasNext()) {
			MyTextObject textObject = (MyTextObject)lineIterator.next();
			lineIndex++;
			if (textObject instanceof TextLine) {
				TextLine nextLine = (TextLine)textObject;
				////System.out.println("[R] Got line:"+nextLine.getText());
				// If this line of text matches the reference pattern, insert a
				// reference hint.
				Matcher currentMatcher = referencePatternUsed.matcher( nextLine.getText() );

				boolean isMatch = false;
				if (currentMatcher.find()) {
					// To guard against invalid matches, require sequentially increasing numbers
					// for regex #6
					if (regexNum == 6) {
						int lineNum = Integer.parseInt( currentMatcher.group( 1 ) );
						// Allow up to two skipped references  (is this too lenient?)
						if (lineNum == prevRefNum+1 || lineNum == prevRefNum+2 || lineNum == prevRefNum+3) {
							isMatch = true;
							prevRefNum = lineNum;
						}
					}
					else {
						isMatch = true;
					}
				}

				if (isMatch) {
					////System.out.println("Adding regex hint@"+lineIndex+" regexNum="+regexNum+" nextLine="+nextLine.getText());
					ret.add( new ReferenceHint( lineIndex ) );

					// reset length of current reference
					refSize = 0;
				}
				// Otherwise update the length of the current reference and stop
				// if the current reference is too long, assuming that the
				// end of the reference section has been reached.
				else {
					refSize += nextLine.getText().length();
					if (refSize > maxReferenceLength) {

						////System.out.println("reference too big! stopping..."+nextLine.getText());
						return ret;
					}
				}
			}
		}
		return ret;
	}

	/* Uses horizontal indentation to segment the references. This is still
	   in development. */

	/* TODO: check for negative dy use c/rattler.cameron.edu/http:\\rattler.cameron.edu\EMIS\journals\AG\1-4\4_323.pdf.xml as test*/
	/* outstanding issue -- detecting horizontal indent when only one reference is on
	   the first page of the references section (see
	   u/www.thphys.uni-heidelberg.de/http:\\www.thphys.uni-heidelberg.de\arcel\hep-lat\0105\0105013.ps.gz.xml
	 */
	private HorizReferenceHintList horizReferenceHintList(List lineList) {
		HorizReferenceHintList ret = new HorizReferenceHintList();

		final double MIN_HORIZ_INDENT = 5;
		final double MAX_HORIZ_INDENT = 90;
		final double MIN_NEWCOLUMN_AMOUNT = 150;
		double indentScale = 1;
		final int INIT_REFERENCE_LINES = 14;

		ListIterator lineIterator = lineList.listIterator();

		Point2D.Double firstLinePosition = new Point2D.Double();
		Point2D.Double prevLinePosition = new Point2D.Double();
		Point2D.Double curLinePosition = new Point2D.Double();
		Point2D.Double scannedLinePosition = new Point2D.Double();
		double rightMarginX = 0;
		double dy = 0;

		boolean newcolumn = true;
		boolean firstColumn = true;
		int refSize = 0;
		boolean hasIndent = false;

		boolean reverse = false;

		int lineIndex;
		// Skip 'References' Line.
		if (!advancePastPrologue(lineIterator)) {
			return null;
		}
		lineIndex = lineIterator.previousIndex();

		// Examine each line.
		while (lineIterator.hasNext()) {
			MyTextObject item = (MyTextObject)lineIterator.next();
			lineIndex++;
			if (item instanceof TextLine) {
				TextLine line = (TextLine)item;

				prevLinePosition.x = curLinePosition.x;
				prevLinePosition.y = curLinePosition.y;
				curLinePosition.x = reverse ? -line.coord.x : line.coord.x;
				curLinePosition.y = line.coord.y;

				dy = prevLinePosition.y - curLinePosition.y;

				// Amount the line is indented to the left of the right-indent margin
				double leftIndentAmount = (rightMarginX - curLinePosition.x) / indentScale;

				if (dy < 0) {
					newcolumn = true;
				}
				if (Math.abs( leftIndentAmount ) > MIN_NEWCOLUMN_AMOUNT) {
					newcolumn = true;
				}

				// *************
				/* If a new column is detected, scan ahead looking for an indent (left or right)
				*  to try to determine if the references continue */
				// *************
				if (newcolumn) {
					newcolumn = false;

					////System.out.println("New Column detected:"+line.getText()+"leftIndentAmount="+leftIndentAmount);

					ListIterator scanAheadIterator = lineList.listIterator( lineIterator.nextIndex() );
					int scannedLines = 0;
					boolean foundIndent = false;
					int columnBeginLineIndex = lineIndex;
					while (scanAheadIterator.hasNext() && scannedLines < 10) {
						MyTextObject nextObj = (MyTextObject)scanAheadIterator.next();
						lineIndex++;
						if (nextObj instanceof TextLine) {
							TextLine scannedLine = (TextLine)nextObj;

							////System.out.println("scanned line :"+scannedLine.getText());
							prevLinePosition.x = scannedLinePosition.x;
							prevLinePosition.y = scannedLinePosition.y;
							scannedLinePosition.x = reverse ? -scannedLine.coord.x : scannedLine.coord.x;
							scannedLinePosition.y = scannedLine.coord.y;
							double rightIndentAmount = scannedLinePosition.x - curLinePosition.x;
							leftIndentAmount = -rightIndentAmount;

							// Check for negative dy
							if (scannedLines > 0) {
								if (scannedLinePosition.y > prevLinePosition.y) {
									////System.out.println("negative dy detected");
									break;
								}
							}

							// Hack -- check for "reverse indentation"
							// If left indentation was found and this is the first column, then it 
							// appears that "reverse indentation" is being used.
							if (leftIndentAmount >= MIN_HORIZ_INDENT && leftIndentAmount <= MAX_HORIZ_INDENT && firstColumn) {
								reverse = true;

								scannedLinePosition.x *= -1;
								prevLinePosition.x *= -1;
								curLinePosition.x *= -1;
								rightIndentAmount *= -1;
								leftIndentAmount *= -1;
							}

							// Check for a positive indent
							if (rightIndentAmount >= MIN_HORIZ_INDENT && rightIndentAmount <= MAX_HORIZ_INDENT) {
								foundIndent = true;
								////System.out.println("positive indent detected. indent="+rightIndentAmount);
								// Since a positive indent was detected, the first line of
								// the new column marked the beginning of a new reference.
								// Reset rightMarginX and continue from the first line
								rightMarginX = scannedLinePosition.x;

								// reset lineIndex to the beginning of the new column
								lineIndex = columnBeginLineIndex;

								break;
							}

							// Check for a negative indent
							if (leftIndentAmount >= MIN_HORIZ_INDENT && leftIndentAmount <= MAX_HORIZ_INDENT) {
								foundIndent = true;
								////System.out.println("negative indent detected");
								// Since a negative (left) indent was detected, the first line of
								// the first reference was a continuation of the previous
								// reference.  Reset rightMarginX and continue from the
								// scanned line

								// align the right indent margin with the first
								// line in this column
								rightMarginX = curLinePosition.x;

								// continue from the new position
								curLinePosition.x = scannedLinePosition.x;
								line = scannedLine;
								lineIterator = scanAheadIterator;
								break;
							}

							// Check for an out of range indent
							if (Math.abs( leftIndentAmount ) > MAX_HORIZ_INDENT * indentScale) {
								////System.out.println("out of range indent detected:"+leftIndentAmount);
								break;
							}

							scannedLines++;
						}
						else if (nextObj instanceof TextPageBoundary) {
							////System.out.println("new page detected");
							break;
						}
					}
					// No indent was found during the scan.
					if (!foundIndent) {
						if (firstColumn) {
							////System.out.println("No indent found.");
							return null;
						}
						else {
							// I'm not sure what the most correct thing to do here is.
							// For now, mark the end of the references section.
							////System.out.println("Couldn't find (valid) indent in new column");
							////System.out.println("[no (valid) indent found] adding section end hint@"+columnBeginLineIndex+":"+line.getText());
							ReferenceSectionEndHint endHint = new ReferenceSectionEndHint( columnBeginLineIndex );
							ret.add( endHint );
							return ret;
						}
					}

					// Recalculate indent amount for new line
					leftIndentAmount = (rightMarginX - curLinePosition.x) / indentScale;
				}

				// Check for a valid left indent
				if (leftIndentAmount >= MIN_HORIZ_INDENT && leftIndentAmount <= MAX_HORIZ_INDENT) {
					// If a valid left-indent is detected add a reference end hint
					ret.add( new ReferenceHint( lineIndex ) );
					////System.out.println("Adding horiz reference hint@"+lineIndex+" Text="+line.getText());
					refSize = 0;
				}
				// Check for proximity to rightMarginX
				//else if (Math.abs( leftIndentAmount ) < MIN_HORIZ_INDENT) {
				else if (leftIndentAmount < MIN_HORIZ_INDENT) {

					refSize += line.getText().length();
					// Stop if the reference has grown too large
					// (unlikely)
					if (refSize > maxReferenceLength) {
						////System.out.println("Reference too long, stopping");
						return ret;
					}
				}
/*				Temporarily disabled since I think this may help with some cases e.g.
				http://www.c3.lanl.gov/napc/pdf/mortgage.pdf
				else {
					// Stop if indented outside the valid indent range
					// but smaller than necessary for a new column
					////System.out.println("Invalid indent size:"+Math.abs(leftIndentAmount)+", stopping");
					return ret;
				}
*/

				firstColumn = false;
			}
			else if (item instanceof TextPageBoundary) {
				newcolumn = true;
			}
		}

		return ret;
	}

	/* Reference finding using vertical seperation.  Currently in development.
	 * Not all documents seperate their references using vertical seperation, but
	 * nearly all documents seem to mark the end of the references section with
	 * vertical white space (or EOF).  Therefore, this method will always return
	 * something -- for documents which use vertical seperation to mark reference
	 * boundaries, it returns a list of reference hints appended with a
	 * reference section end hint.  (marking its guess as to the the end of
	 * references) For other documents, it returns only the reference
	 * section end hint.
	 */

	/* Todo -- potentially change the definition of "close" to a DY from
	 * relative to absolute.  This would allow smaller fonts to have more DY
	 * variation.  I think this might help in some cases.
	 */


	private VerticalReferenceHintList verticalReferenceHintList(List lineList) {
		VerticalReferenceHintList retRefList = new VerticalReferenceHintList();

		final double MIN_INDENT_DIFFERENCE = .15;
		// max amount dy can vary from 'maxDY' to be counted as the "same
		// seperation"
		final double SAME_INDENT_THRESHOLD = .09;
		// lines to check to observe large dy seperation occur twice
		//final int INIT_REFERENCE_LINES = 14;
		final int INIT_REFERENCE_LINES = 22;

		/* This defines the maximum amount of whitespace at the end of a page
		* which will be allowed in the middle of a multi-page reference
		* section.
		*/
		//final double MAX_BOTTOM_MARGIN = 2500;
		////final double MAX_BOTTOM_MARGIN = 1800;
		// Reduced by factor of 10 due to new ps2text format.
		final double MAX_BOTTOM_MARGIN = 250;

		ListIterator lineIterator = lineList.listIterator();
		int lineIndex;
		int textLineNum = -1;
		int refSize = 0;

		// Value used to seperate rows in the same reference
		double minDY = Double.MAX_VALUE;
		// Value used to seperate rows in different references
		double maxDY = 0;
		double curDY;
		boolean newcolumn = true;
		boolean first = false;

		/* Skip the initial 'References' marker. */
		//TextLine refMarker = (TextLine)lineIterator.next();
		if (!advancePastPrologue(lineIterator)) {
			return null;
		}

		/* Search the first ten lines of text for varied y indentation */
		Point2D.Double prevLinePosition = new Point2D.Double();
		Point2D.Double curLinePosition = new Point2D.Double();

		//*****************************************************
		/* Step 1: Analyze the reference section to determine A: if varied
		* vertical seperation has been used to seperate references and B:
		* how much vertical seperation should be required to seperate
		* references and/or infer the end of the references section.
		*/
		//*****************************************************
		boolean hasVariedSeperation = false;

		/* Try to find the same large seperation twice within the first INIT_REFERENCES_LINES. */

		/*boolean foundBigIndent = false;
		double bigDY = -1;
		double seperationDY;*/
		while (lineIterator.hasNext() && textLineNum < INIT_REFERENCE_LINES) {
			MyTextObject item = (MyTextObject)lineIterator.next();

			if (item instanceof TextLine) {
				TextLine line = (TextLine)item;
				////System.out.println("VI Read line "+line.getText());
				textLineNum++;

				prevLinePosition.x = curLinePosition.x;
				prevLinePosition.y = curLinePosition.y;
				curLinePosition.x = line.coord.x;
				curLinePosition.y = line.coord.y;
				curDY = prevLinePosition.y - curLinePosition.y;

				if (curDY < 0) {
					newcolumn = true;
				}

				// skip the first line in a column
				if (newcolumn) {
					//System.out.println("VI New Column minDY="+minDY+" maxDY="+maxDY);
					newcolumn = false;
					continue;
				}

				//************
				// Consider DY
				//************

				/* Update minDY */
				minDY = Math.min( curDY, minDY );

				/* Update maxDY */

				/* Check if we've encountered y seperation "close" to the old maxDY */
				double ratio = (maxDY > 0 ? Math.abs( curDY - maxDY ) / maxDY : Double.MAX_VALUE);
				if (ratio <= SAME_INDENT_THRESHOLD) {
					// update maxDY if appropriate (to be safe, since we want the "upper end"
					// on what the indent can be)
					if (curDY > maxDY) {
						maxDY = curDY;
					}

					//System.out.println("found duplicate large indent");
					//System.out.println("minDY="+minDY+"maxDY="+maxDY);

					/* If the old maxDY was sufficiently greater than minDY
					   then use it for reference partitioning.  Waiting until we've encountered
					   this DY a second time ensures that we won't misintepret whitespace at the end
					   of a short references section (without vertical seperation between
					   references) as a seperation distance between references.
					*/
					if ((maxDY - minDY) / minDY >= MIN_INDENT_DIFFERENCE) {
						//System.out.println("Found vertical seperation: minDY="+minDY+" maxDY="+maxDY+" ratio="+(maxDY-minDY)/maxDY);
						hasVariedSeperation = true;
						break;
					}
				}

				/* Otherwise, check if maxDY should be updated */
				else if (curDY > maxDY) {

					if ((maxDY - minDY) / minDY >= MIN_INDENT_DIFFERENCE) {
						/* This is an unusual situation -- we've encountered *two* distinct DY
						* values sufficiently larger than minDY to count as a seperation distance
						* between references.  This (unlikely) scenario might occur if vertical
						* seperation is used in a references section with only two references.  (or
						* if several dy values are used after a short references section with no
						* varied vertical seperation -- but let's not go there)
						*
						* Assume that the first DY encountered is the spacing between references
						* and that the second DY (just encountered) occurs at the end of the
						* references section.  This should correctly handle the "two references,
						* vertical seperation" case.
						*/
						//System.out.println("Unexpected second 'large' vertical seperation: "+curDY);
						//System.out.println("Assuming prior 'maxDY' is the reference seperation distance:"+maxDY);
						hasVariedSeperation = true;
						break;
					}
					maxDY = curDY;
				}
			}

			else if (item instanceof TextPageBoundary) {
				newcolumn = true;
			}
		}

		//*****************************************************
		/* Step 2:  Use vertical seperation to try to locate
	     * the end of the references section (and individual
	     * references if hasVariedSeperation is set to true)
		//*****************************************************
		/* Find references using vertical seperation variance */

		lineIterator = lineList.listIterator();
		newcolumn = false;
		first = true;

		/* Skip the 'References' marker */
		if (!advancePastPrologue(lineIterator)) {
			return null;
		}
		lineIndex = lineIterator.previousIndex();

		while (lineIterator.hasNext()) {
			MyTextObject item = (MyTextObject)lineIterator.next();
			lineIndex++;

			if (item instanceof TextLine) {

				TextLine line = (TextLine)item;
				//System.out.println("VI2 Read line "+line.getText());

				prevLinePosition.x = curLinePosition.x;
				prevLinePosition.y = curLinePosition.y;
				curLinePosition.x = line.coord.x;
				curLinePosition.y = line.coord.y;
				curDY = prevLinePosition.y - curLinePosition.y;

				// Special processing of the first reference
				if (first) {
					first = false;
					// add first reference hint
					if (hasVariedSeperation) {
						ReferenceHint hint = new ReferenceHint( lineIndex );
						//System.out.println("[first] adding reference hint@"+lineIndex+":"+line.getText());
						retRefList.add( hint );
						refSize = line.getText().length();
					}
					continue;
				}

				if (curDY < 0) {
					newcolumn = true;
				}

				if (newcolumn) {
					newcolumn = false;
					/*   Try to determine whether this column contains more
					 * references by checking the next 'dy' value.  If it is in
					 * the valid range then we (assuming the column may contain
					 * more references) add a reference hint and continue.
					 * Otherwise, we add a reference section end hint and stop.
					 *   Note that if the previous reference continues into this
					 * new column then this behavior will be incorrect; perhaps
					 * a new, "weak" reference hint should be introduced for this
					 * case.
					 */
					//System.out.println("new column detected");

					ListIterator scanAheadIterator = lineList.listIterator( lineIterator.nextIndex() );
					double nextY = 0;
					double nextDY = 0;
					boolean foundValidDY = false;
					if (scanAheadIterator.hasNext()) {
						MyTextObject nextObj = (MyTextObject)scanAheadIterator.next();
						if (nextObj instanceof TextLine) {
							TextLine scannedLine = (TextLine)nextObj;
							//System.out.println("VI2 Scanned line "+scannedLine.getText());
							// get the next y position
							nextY = scannedLine.coord.y;
							nextDY = curLinePosition.y - nextY;
							//System.out.println("VI2 nextDY="+nextDY+" minDY="+minDY+" maxDY="+maxDY);

							// check for a valid dy
							if (((Math.abs( nextDY - minDY )-2) / minDY < SAME_INDENT_THRESHOLD) ||
							    ((Math.abs( nextDY - maxDY )-2) / maxDY < SAME_INDENT_THRESHOLD) ||
							    (nextDY >= minDY && nextDY <= maxDY)) {
								foundValidDY = true;
							}
						}
					}
					if (foundValidDY) {
						/* Add reference hint */
						if (hasVariedSeperation) {
							//System.out.println("adding newcolumn reference hint");
							curDY = maxDY;
						}
						else {
							//System.out.println("found valid column indent");
							continue;
						}
					}
					else {
						/* Add section end hint */
						ReferenceSectionEndHint hint = new ReferenceSectionEndHint( lineIndex );
						////System.out.println("[newcolumn] adding section end hint@"+lineIndex+":"+line.getText()+"nextDY="+nextDY+" minDY="+minDY+" maxDY="+maxDY);
						retRefList.add( hint );
						return retRefList;
					}
				}

				/* checks on DX? */

				/* Check 'curDY' for end of reference */
				if (hasVariedSeperation) {
					// outside range but close
					if ((curDY > maxDY && (curDY - (maxDY+2)) / maxDY <= SAME_INDENT_THRESHOLD) ||
					    // inside range but closer to maxDY
					    (curDY <= maxDY && maxDY - curDY < curDY - minDY)) {
						/* Add reference hint */
						ReferenceHint hint = new ReferenceHint( lineIndex );
						////System.out.println("adding reference hint@"+lineIndex+":"+line.getText());
						retRefList.add( hint );
						refSize = line.getText().length();
						continue;
					}
				}
				/* Check 'curDY' for end of reference section */
				if ((hasVariedSeperation && curDY > maxDY) ||
				    (!hasVariedSeperation && (curDY - (minDY+2)) / minDY >= MIN_INDENT_DIFFERENCE)) {
					/* Add section end */
					//System.out.println("[big dy] adding section end hint@"+lineIndex+":"+line.getText()+"hasVariedSeperation="+hasVariedSeperation+"curDY="+curDY+"minDY="+minDY);
					ReferenceSectionEndHint hint = new ReferenceSectionEndHint( lineIndex );
					retRefList.add( hint );
					return retRefList;
				}
				/* Check 'curDY' for unusually small indent (should almost never happen) */
				/* (probably this should just be counted as a "normal" indent.  I'm
				adding this for testing purposes)
				*/
/*
				if ((curDY < minDY) && (minDY - curDY) / minDY > SAME_INDENT_THRESHOLD) {
					System.out.println( "unusually small vertical space!" );
					return null;
				}
*/
				/* Otherwise a "normal" indent */
/*				else { */
				/*
					if (hasVariedSeperation) {
						refSize += line.getText().length();
						if (refSize > maxReferenceLength) {
							 // I'm not sure what the correct thing to do here is.
							 //   Perhaps the best thing to do is to replace the last ReferenceHint
							 //   with a SectionEndHint under the assumption that
							 //   _some_ sort of indentation change (whether bigDY or
							 //   a new column) must have preceded the end of the
							 //   references.
							// TODO: change last hint into sectionendhint
							System.out.println("VI Reference too long:"+refSize+", stopping");
							return retRefList;
						}
					}
				} */
			}
			else if (item instanceof TextPageBoundary) {
				/* Check the y value of the last line on the previous page.  If
				* it is sufficiently high, we assume the references section
				* must end.  This won't work all the time but should catch many
				* of the cases where the only whitespace after the references
				* section is at the bottom of the page.
				*/
				/* For consistency with other methods (currently
				* horizReferenceHints), add the reference end hint _after_ the
				new page.
				*/
				if (curLinePosition.y >= MAX_BOTTOM_MARGIN) {
					//System.out.println("Bottom whitespace: lastY="+curLinePosition.y);
					//System.out.println("[bottom whitespace] adding section end hint@"+(lineIndex+1));
					ReferenceSectionEndHint hint = new ReferenceSectionEndHint( lineIndex + 1 );
					retRefList.add( hint );
					return retRefList;
				}
				newcolumn = true;
			}
		}
		////System.out.println("[end of lines] adding section end hint@"+lineIndex);
		ReferenceSectionEndHint hint = new ReferenceSectionEndHint( lineIndex + 1 );
		retRefList.add( hint );
		return retRefList;
	}


	// Input: list of lines to be searched for the greatest monotonically increasing sequence
	// Output: indices of the greatest monotonically increasing sequence
	// (i.e., the alphabetized reference lines)
	//
	private static ArrayList getMonotonicLines(List inLinesArg) {

		// convert to arrayList of strings
		ArrayList inLines = new ArrayList();
		inLines.addAll(inLinesArg);

		int[] table = new int[inLines.size()];
		int[] previous = new int[inLines.size()];
		/* dynamic programming algorithm:
		table[y] = the longest sequence ending at 'y'.
		table[y] = 1 if y is the beginning
		tabel[y] = MAX(table[x] s.t. x<y and value[x] <= value[y])+1
		previous[y] = previous 'x' that was found.
		*/

		for (int j = 0; j < inLines.size(); j++) {
			MyTextObject obj = (MyTextObject)inLines.get(j);
			if (!(obj instanceof TextLine)) 
				continue;
			String str = ((TextLine)obj).getText();
			String[] tokens = ((String[])str.trim().split("\\s+"));

			// Skip lines with fewer than 5 words
			if (tokens.length < 5)
				continue;

			String word = tokens[0];
			// Hack -- automatically capitalize surnames starting with "van" or "von".
			// I'm not sure if this is the correct thing
			// in general but it seems to help for one paper.
			if (word.equals("van"))
				word = "Van";

			if (word.equals("von"))
				word = "Von";

			if (word.equals("de"))
				word = "De";

			// Only count lines starting with a capital word
			if (!Character.isUpperCase(word.charAt(0)))
				continue;

			// Only count words followed by a comma OR followed by
			// "et" or "and" OR on a line containing a year.
			boolean isValid = false;
			// Count first words followed by a comma
			if (word.charAt(word.length()-1) == ',')
				isValid = true;
			if (tokens[1].equals(","))
				isValid = true;

			// Count lines with "et" or "and" as the second word
			if (tokens[1].matches("(et|ET)\\.?") || tokens[1].matches("and|AND"))
				isValid = true;

			// Count lines containing a year
			if (str.matches(".*(19|20)\\d\\d.*"))
				isValid = true;

			// Sometimes last names consist of two words, so check again for the second token
			/*
			if (Character.isUpperCase(tokens[1].charAt(0))) {
				if (tokens[1].charAt(tokens[1].length()-1) == ',')
					isValid = true;
				if (tokens[2].equals(","))
					isValid = true;
				// Hack -- skip checking for "And" after the second
				// word since this would be too common.
				if (tokens[2].matches("(et|ET)\\.?")) //|| tokens[2].matches("and|AND"))
					isValid = true;

				// Count lines with a number as the second word
				if (tokens[2].matches("\\d+"))
					isValid = true;
			}
			*/

			if (!isValid)
				continue;

			int bestScore = 0;
			int bestIndex = -1;
			for (int i = Math.max(0, j-20); i < j; i++) {
				MyTextObject obj2 = (MyTextObject)inLines.get(i);
				if (!(obj2 instanceof TextLine)) 
					continue;
				String str2 = ((TextLine)obj2).getText();
				String word2 = ((String[])str2.trim().split("\\s+"))[0];

				// Hack -- automatically capitalize surnames starting with "van" or "von".
				// I'm not sure if this is the correct thing
				// in general but it seems to help for one paper.
				if (word2.equals("van"))
					word2 = "Van";

				if (word2.equals("von"))
					word2 = "Von";

				if (word2.equals("de"))
					word2 = "De";

				// Only compare to first two letters of the previous word (under theory this is less prone
				// to human ordering errors)
				if (word2.length() >= 3) {
					word2 = word2.substring(0, 2);
				}

				// if word2 < word and this is the best score yet
				if (word2.compareTo(word) <= 0 && table[i] > bestScore) {
					bestScore = table[i];
					bestIndex = i;
				}
			}
			table[j] = bestScore+1;
			previous[j] = bestIndex;
		}

		int maxScore = 0;
		int maxIndex = 0;
		for (int j = 0; j < table.length; j++) {
			if (table[j] > maxScore) {
				maxScore = table[j];
				maxIndex = j;
			}
		}

		// If no valid hints were found, stop.
		if (maxScore == 0)
			return new ArrayList();

		/*
		at end, find max table entry, walk previous entries back to the beginning pushing onto stack.
		pop off stack and print.
		*/
		LinkedList reverseStack = new LinkedList();
		int curIndex = maxIndex;
		while (curIndex != -1) {
			reverseStack.addFirst(new Integer(curIndex));
			curIndex = previous[curIndex];
		}
		ArrayList ret = new ArrayList();
		ret.addAll(reverseStack);
		return ret;
		/*
		ArrayList ret = new ArrayList();
		Iterator lineI = reverseStack.iterator();

		while (lineI.hasNext()) {
			ret.add( inLines.get(((Integer)lineI.next()).intValue()) );
		}
		return ret;
		*/
	}

	private AlphabetReferenceHintList alphabetReferenceHintList(List lineList) {
		AlphabetReferenceHintList ret = new AlphabetReferenceHintList();
		ListIterator lineIterator = lineList.listIterator();

		// Skip the 'References' marker
		if (!advancePastPrologue(lineIterator)) {
			return null;
		}
		TextLine firstLine = (TextLine) lineIterator.next();

		// Require the first letter to be capitalized and the first word to not be an initial.
		if (firstLine.getText().length() < 3)
			return null;
		if (!Character.isUpperCase(firstLine.getText().charAt(0)))
			return null;

		// Check the second and third characters since some valid last
		// names (O'Neil) have punctuation in the second character.
		if (! (Character.isLetter(firstLine.getText().charAt(1)) || Character.isLetter(firstLine.getText().charAt(2)) ) )
			return null;

		//System.out.println("finding monotonic line...");
		ArrayList sequenceLines = getMonotonicLines(lineList);
		//System.out.println("done.");

		// Check for failure for some reason
		if (sequenceLines.size() == 0)
			return null;

		Iterator lineI = sequenceLines.iterator();
		while (lineI.hasNext()) {
			int lineIndex = ((Integer)lineI.next()).intValue();
			////System.out.println( "alphabet hint: "+((TextLine)lineList.get(lineIndex)).getText() );
			ret.add( new ReferenceHint( lineIndex ) );
		}

		return ret;
	}


	private class HintInfo {
		boolean hasRegexHints = false;
		boolean hasHorizHints = false;
		boolean hasVerticalHints = false;
		boolean hasAlphabetHints = false;

		RegexReferenceHintList regexHintList;
		HorizReferenceHintList horizHintList;
		VerticalReferenceHintList verticalHintList;
		AlphabetReferenceHintList alphabetHintList;
	}

	/* Applies the different methods for marking references in the given list of lines. */
	private HintInfo addReferenceHints(List lineList) throws ReferenceParsingException {
		HintInfo ret = new HintInfo();

		ret.regexHintList = regexReferenceHintList( lineList );
		ret.horizHintList = horizReferenceHintList( lineList );
		ret.verticalHintList = verticalReferenceHintList( lineList );
		ret.alphabetHintList = alphabetReferenceHintList( lineList );

		ret.hasRegexHints = (ret.regexHintList != null);
		ret.hasHorizHints = (ret.horizHintList != null);
		ret.hasVerticalHints = (ret.verticalHintList != null);
		ret.hasAlphabetHints = (ret.alphabetHintList != null);

		return ret;
	}

	/* Currently unused */
	private boolean referenceListsAreSame(ReferenceHintList a, ReferenceHintList b) {
		if (a.size() != b.size()) {
			return false;
		}
		Iterator aIterator = a.iterator();
		Iterator bIterator = b.iterator();
		for (Iterator i = a.iterator(); i.hasNext();) {
			ReferenceHint hintA = (ReferenceHint)aIterator.next();
			ReferenceHint hintB = (ReferenceHint)bIterator.next();
			if (hintA.listIndex != hintB.listIndex) {
				return false;
			}
		}
		return true;
	}

	/* Combines the reference hints produced from multiple reference marking
	 * methods into one definitive list.
	 */

	/*  Vertical seperation:

		- Can't determine of previous reference wraps into new column

		Horizontal Indentation:

		- Potential for invalid matches past end

		Regular Expressions:

		- May ocasionally fail or match incorrectly due to wrapped lines for regex 4
		- Potential for invalid matches past the end of the references section

		*/
	private ReferenceHintList combineReferenceHints(List lineList, HintInfo info) throws ReferenceParsingException {

		ReferenceHintList ret = new ReferenceHintList();
		ReferenceSectionEndHint endHint;
		int regexNum;
		int eofIndex = lineList.size();

		/*
		if (!info.hasAlphabetHints) {
			throw new ReferenceParsingException( "no alphabet hints found" );
		}
		*/

		/* Currently, the vertical end hint finder always adds a ReferenceSectionEndHint to
		its returned list to mark where it thinks the reference section ends
		-- even in cases where vertical spacing is not used to seperate
		individual references.  This section end hint is currently used in
		almost all cases (all except a few involving regex hints of type 1
		-- described below) to mark the end of the references section.
		*/

		// *********************************
		// Get the vertical section end hint
		// *********************************

		if (!info.hasVerticalHints) {
			throw new ReferenceParsingException( "no vertical hints found" );
		}

		ReferenceHint lastVerticalHint = info.verticalHintList.get( info.verticalHintList.size() - 1 );
		if (lastVerticalHint instanceof ReferenceSectionEndHint) {
			endHint = (ReferenceSectionEndHint)lastVerticalHint;
		}
		else {
			throw new ReferenceParsingException( "no vertical section end hint" );
		}

		if (info.verticalHintList.size() == 1 && !info.hasRegexHints && !info.hasHorizHints && !info.hasAlphabetHints) {
			throw new ReferenceParsingException( "no reference hints founds" );
		}

		////System.out.println("hasRegexHints="+info.hasRegexHints);
		////System.out.println("hasHorizHints="+info.hasHorizHints);
		////System.out.println("hasVerticalHints="+info.hasVerticalHints);

		// *********************************************
		// Check for premature vertical section end hint
		// *********************************************

		/*     Unfortunately, (due to header/footer issues, the references section being drawn
		* "out of order", TextLine construction problems, etc) ocasionally the
		* vertical section end hint occurs prematurely.
		*     Experimental evidence shows that most of the time, if the last regex hint of type #1
		* (brackets) occurs after the vertical section end hint, then this corresponds to a valid
		* reference.  Therefore, in these circumstances we discard the vertical section end hint,
		* under the assumptions that the vertical section end hint is probably premature, and that
		* preserving later references is worth a potentialy malformed reference or two in the middle.
		*     This should *not* be done with horizontal indenting since horizontal indenting frequently
		* incorrectly matches "bogus references" after the end of the references
		* section, and so more often than not any horizontal reference hints occuring
		* past the vertical section end hint are incorrect and should be discarded.
		*/
		boolean hasReliableRegexHints = (info.hasRegexHints && (info.regexHintList.regexNum == 1 || info.regexHintList.regexNum == 6));
		if (hasReliableRegexHints || info.hasAlphabetHints) {
			ReferenceHintList sourceList = hasReliableRegexHints ? (ReferenceHintList) info.regexHintList : (ReferenceHintList) info.alphabetHintList;
			ReferenceHint finalHint = sourceList.get( sourceList.size() - 1 );
			if (finalHint.listIndex > endHint.listIndex) {
				//System.out.println(
				   // "detected reliable hint after vertical section end hint;  assuming vertical section end hint is premature" );
				// XXX skip the last reference since we have no way of knowing where it ends
				sourceList.remove(sourceList.size()-1);
				//sourceList.add( new ReferenceSectionEndHint( lineList.size() ) );
				sourceList.add( new ReferenceSectionEndHint( finalHint.listIndex ) );
				return sourceList;
			}
		}

		// *****************************************************************
		// If only vertical hints are available, use the vertical hint list
		// *****************************************************************
		if (info.verticalHintList.size() != 1 && !info.hasRegexHints && !info.hasHorizHints && !info.hasAlphabetHints) {
			return info.verticalHintList;
		}

		// *****************************************************************
		// Attempt to choose a single "main" source of hints and combine it with
		// the the vertical section end hint
		// *****************************************************************

		Iterator hintSourceIterator = null;

		// Hack -- Sometimes the horizontal hint list is truncated prematurely
		// Check for this by comparing the horizontal hint list to other hint lists to
		// make sure it is not too small (< 3/4 of another hint list's length)
		boolean prematureHorizEnd = false;
		if (info.hasHorizHints) {
			//System.out.println("Checking for too short horiz hint list:");
			//System.out.println("numHorizHints = "+info.horizHintList.size()+", numAlphabetHints="+info.alphabetHintList.size()+", numRegexHints="+info.regexHintList.size());
			// Require a hint list at least 3/4 of the other hints
			if (info.hasAlphabetHints && info.horizHintList.size()*4 < info.alphabetHintList.size()*3)
				prematureHorizEnd = true;
			if (info.hasRegexHints && info.horizHintList.size()*4 < info.regexHintList.size()*3)
				prematureHorizEnd = true;
			if (prematureHorizEnd) {
				////System.out.println("Premature end of horizontal reference hints detected; using another hint source");
			}
		}

		// Try using 'bracketed' or 'numbered' regex hints since they are
		// very reliable

		if (info.hasRegexHints && (info.regexHintList.regexNum == 1 || info.regexHintList.regexNum == 6)) {
			//throw new ReferenceParsingException("D");
			hintSourceIterator = info.regexHintList.iterator();
		}

		// Try using horiz hints, since these are usually reliable
		/* I examined 15 random documents which were not 'bracketed' or 'numbered' but had
		regex matches and horizontal indentation.
		sets.  In 11/15 cases, the horizontal indentation hints were preferable.  (In
		3/15 cases, the horizontal references were truncated prematurely.)  Hence, if
	        horizontal indentation is available, we should use it.
		*/
		else if (info.hasHorizHints && !prematureHorizEnd) {
			hintSourceIterator = info.horizHintList.iterator();
		}
		// I have not yet compared alphabet hints versus the other methods, so this
		// is just a guess as to their correct preference level!
		else if (info.hasAlphabetHints) {
			hintSourceIterator = info.alphabetHintList.iterator();
		}
		
		// If only regex hints are available use them as the main source
		else if (info.hasRegexHints && info.verticalHintList.size() == 1) {
			//throw new ReferenceParsingException("C");
			hintSourceIterator = info.regexHintList.iterator();
		}

		/* The only remaining case is where both vertical hints and
		* regex hints (other than reference pattern 1), are available, but
		* horizontal hints are not.  We could potentially overcome problems in
		* regex matching (e.g. wrapped lines that match pattern #4 one line
		* too late) and vertical indentation (the inability to tell if a
		* reference continues into a new column) by comparing the two hint
		* lists.  This is a topic for further investigation.
		* For now, we just use the regex list since this case seems to be
		* fairly uncommon.
		*/
		else {
			//throw new ReferenceParsingException("B");
			hintSourceIterator = info.regexHintList.iterator();
		}

		assert(hintSourceIterator != null);

		// combine references with vertical hint
		while (hintSourceIterator.hasNext()) {
			ReferenceHint hint = (ReferenceHint)hintSourceIterator.next();
			if (hint.listIndex >= endHint.listIndex) {
				break;
			}
			////System.out.println("Adding hint@"+hint.listIndex);
			ret.add( hint );
			/* If the hint source (currently only occurs with horizontal hints)
			   contains a reference section end hint before endHint, use it.
			   I have not yet investigated whether this improves the results.
			   (i.e. whether in practice horizontal section end hints occuring
			   before the vertical section end hint are accurate indicators of
			   the section end)
			*/

			if (hint instanceof ReferenceSectionEndHint) {
				return ret;
			}
		}
		/* Add the vertical section end hint */
		////System.out.println("[hintlist end] Adding hint@"+endHint.listIndex);
		ret.add( endHint );
		return ret;
	}

	public ReferenceData buildReferences(List inLineList, ReferenceHintList hintList) {

		ArrayList lineList = new ArrayList( inLineList );  // Avoids theta(n^2) building time on linked lists.  Probably unnecessary for inputs of reasonable size.
		ReferenceData refData = new ReferenceData();

		LinkedList prologue, referenceList, epilogue;
		LinkedList reference;
		ReferenceHint hint;
		int startIndex = 0;
		int endIndex = 0;

		Iterator hintIterator = hintList.iterator();

		// handle prologue list
		hint = (ReferenceHint)hintIterator.next();
		endIndex = hint.listIndex;
		prologue = new LinkedList( lineList.subList( 0, endIndex ) );
		refData.prologueList = prologue;
		//System.out.println("Prologue is"+prologue);

		// handle reference lists
		referenceList = new LinkedList();
		while (hintIterator.hasNext()) {
			hint = (ReferenceHint)hintIterator.next();
			startIndex = endIndex;
			endIndex = hint.listIndex;
			assert(startIndex < endIndex);
			reference = new LinkedList( lineList.subList( startIndex, endIndex ) );
			////System.out.println("[buildReferences] reference is"+reference);
			referenceList.add( reference );
		}
		refData.referenceLineList = referenceList;

		// handle epilogue list
		epilogue = new LinkedList();
		if (endIndex < lineList.size()) {
			epilogue.addAll( lineList.subList( endIndex, lineList.size() ) );
		}
		refData.epilogueList = epilogue;
		//System.out.println("epilogue is"+epilogue);

		return refData;
	}

	/**
	 * @param bibliographyContentList
	 * @return
	 */

	protected ReferenceData extractReferences(List lineList) throws ReferenceParsingException {

		HintInfo hintInfo = addReferenceHints( lineList );
		ReferenceHintList hintList = combineReferenceHints( lineList, hintInfo );
		ReferenceData refdata = buildReferences( lineList, hintList );

		return refdata;
	}

	public static class ReferenceData {
		LinkedList prologueList;
		LinkedList referenceLineList;
		LinkedList epilogueList;
		int numReferences;
	}
}

