package org.rexo.referencetagging;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.ListIterator;

/* Tries to remove page numbers and any corresponding header/footer text from
 * a document.
*/

public class PaginationArtifactRemover {

	private static Logger log = Logger.getLogger( PaginationArtifactRemover.class );

	final static double MAX_BODY_Y = 8000;

	public PaginationArtifactRemover() {

	}

	private boolean isHeaderFooterText(TextLine line) {
		String s = line.getText();
		// (?s) enables 'DOTALL' mode which lets '.' match line seperators.
		// (see http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html#DOTALL)
		// This used to be important as '\n' is inserted at the beginning of all
		// XML Text elements (perhaps because the XML parser doesn't ignore whitespace).
		// Note: this is no longer needed (since TextLine.getText() trims extra
		// whitespace) but I'm leaving it in for the sake of possible future
		// changes.

		//System.out.println("comparing text:"+s);

		// Line may potentially be something important (e.g. 1 Introduction)
		// (this check should perhaps be restricted to post text only, although I didn't
		// find any 'pre' matches in 500 documents)
		if (s.matches( "(?s)^\\s*1\\.?\\s+\\w.*" )) {
			return false;
		}

		// Line begins with digit
		if (s.matches( "(?s)^\\s*\\(?\\d+\\)?(\\s.+)?$" )) {
			return true;
		}

		// Line ends with digit
		//if (s.matches("(?s)^(.+\\s)?\\(?\\d+\\)?\\s*$")) return true;
		if (s.matches( "(?s)^.*\\d\\)?\\s*$" )) {
			return true;
		}

		// Line is of the form: " - page number - "
		// Note -- Character '-' can be written as character 0 in font 'Fd',
		// as it is defined in several postscript files.  This is later turned
		// into '|' via 2xml using the default character replacement for character 0.
		if (s.matches( "(?s)^\\s*(-|\\|)?\\s*\\d+\\s*(-|\\|)?\\s*$" )) {
			return true;
		}
		return false;
	}

	private void removeLineArtifacts(List lineList) {
		ListIterator lineIterator = lineList.listIterator();

		// don't shorten the header of the first page
		boolean firstpage = true;
		MyTextObject textObj;
		TextLine line;

		// handle text at the beginning/end of pages
		while (lineIterator.hasNext()) {
			textObj = (MyTextObject)lineIterator.next();
			if (textObj instanceof TextPageBoundary) {
				if (firstpage) {
					// don't shorten the header of the first page
					//System.out.println("skipping first page");
					firstpage = false;
					continue;
				}

				// move back to before TextPageBoundary
				lineIterator.previous();

				// if the previous TextObject is a TextLine, inspect it
				textObj = (MyTextObject)lineIterator.previous();
				if (textObj instanceof TextLine) {
					line = (TextLine)textObj;
					//System.out.println("got (pre) textline:"+line.getText());
					if (isHeaderFooterText( line )) {
						log.debug( "removing PRE text: " + line.getText() );
						lineIterator.remove();
					}
					else {
						lineIterator.next();
					}
				}
				// move to after TextPageBoundary
				lineIterator.next();

				// if the next TextObject is a TextLine, inspect it
				if (lineIterator.hasNext()) {
					textObj = (MyTextObject)lineIterator.next();
					if (textObj instanceof TextLine) {
						line = (TextLine)textObj;

						// Anything above 8000 is automatically removed
						if (line.coord.y > MAX_BODY_Y) {
							//System.out.println("removing POST text [off page]: "+line.getText());
							lineIterator.remove();
						}
						else if (isHeaderFooterText( line )) {
							//System.out.println("removing POST text: "+line.getText());
							lineIterator.remove();
						}
					}
				}
			}
		}
		// get the last TextLine of the document
		while (lineIterator.hasPrevious()) {
			textObj = (MyTextObject)lineIterator.previous();
			if (textObj instanceof TextLine) {
				line = (TextLine)textObj;
				if (isHeaderFooterText( line )) {
					//System.out.println("removing PRE text: "+line.getText());
					lineIterator.remove();
				}
				break;
			}
		}
	}

	/* Remove pagination artifacts for element 'contentElement'. */
	public void removePaginationArtifacts(List lineList) {
		//List contentList = contentElement.getContent();
		removeLineArtifacts( lineList );
	}

}
