/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on June 17, 2004
 * author: adingle
 */

package org.rexo.pipeline;

import org.jdom.Element;
import org.jdom.Text;
import org.jdom.filter.Filter;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;

import java.util.Iterator;
import java.util.List;


public class PartitioningValidationFilter extends AbstractFilter implements RxFilter {

  public final int maxHeaderContentLength = 20000;
  public final int maxReferenceContentLength = 3200;

  public int accept(RxDocument rdoc) {
    Element contentElement = (Element)rdoc.getScope( "document" ).get( "content.element" );

    if (contentElement == null) {
      getLogger( rdoc ).error( "nothing to verify");
      rdoc.docErrorString("nothing to verify");
      return ReturnCode.ABORT_PAPER;
    }

    Element headersElement = contentElement.getChild( "headers" );
    if (headersElement == null) {
      getLogger( rdoc ).error( "no headers to verify" );
      rdoc.docErrorString("no headers to verify");
      return ReturnCode.ABORT_PAPER;
    }

    String contentString = getDescendantText( headersElement );
    // Verify that the header is not too big
    if (contentString.length() > maxHeaderContentLength) {
        getLogger( rdoc ).error( "header too long @"+maxHeaderContentLength);
	rdoc.docErrorString("header too long @"+maxHeaderContentLength);
	return ReturnCode.ABORT_PAPER;
    }

    // Verify each reference Element
    List referenceList = contentElement.getChildren( "reference" );
    Iterator referenceIterator = referenceList.iterator();
    int numLongRef = 0;
    while (referenceIterator.hasNext()) {
      Element referenceElement = (Element)referenceIterator.next();

      // Verify reference size
      contentString = getDescendantText(referenceElement);
      if (contentString.length() > maxReferenceContentLength) {
	/* [obsolete -- see below]
	 * If this is the last reference of the reference list, and it is the
	 * first reference to be too long, it is probably too long due to some
	 * extra content after the references section.  I have observed that in
	 * many cases the extra content begins on a new page.  Therefore, we
	 * attempt to truncate the final reference at the first page boundary.
	 */

	/*
	Temporarily disabled.  Since the reference extraction code now is capable
	of determining the end of the reference section on its own, if this problem
	occurs it probably indicates a failure in the reference extraction stage (and so an
	error is more appropriate than this workaround which may not be applicable.)

	It's possible in a few cases the end of the references section is
	still being missed by the reference extractor and so this workaround
	might still be applicable.
	*/
/*
	boolean truncated = false;

	if (!referenceIterator.hasNext() && numLongRef == 0) {

	  List contentList = referenceElement.getContent();
	  Iterator contentIterator = contentList.iterator();
	  int index = 0;
	  while (contentIterator.hasNext()) {
	    Content c = (Content) contentIterator.next();
	    if (c instanceof Element) {
	      if (((Element)c).getName().equals("page")) {
		// truncate the reference's content list here
		System.out.println("Truncating reference at page boundary:");
		System.out.println(contentList);
		contentList.subList(index, contentList.size()).clear();
		System.out.println("Truncated list:");
		System.out.println(contentList);
		truncated = true;
		break;
	      }
	    }
	    index++;
	  }
	}
	// Check if the last reference was succesfully truncated to a
	// reasonable length
	if (truncated) { 
          contentString = getDescendantText(referenceElement);
	  if ((contentString.length() <= maxReferenceContentLength)) {
		System.out.println("Succesfully shortened list!");
	    rdoc.docInfoString("last reference truncated@page");
	    continue;
	  }
	}
*/

        getLogger( rdoc ).error( "reference too long @"+maxReferenceContentLength);
        numLongRef++;
	if (numLongRef > 1) break;
      }

    } 
    // Hack -- log these out to seperate files since I believe that these
    // are symptomatic of different problems
    if (numLongRef == 1) {
    	rdoc.docErrorString("reference too long @"+maxReferenceContentLength);
	return ReturnCode.ABORT_PAPER;
    }
    if (numLongRef > 1) {
    	rdoc.docErrorString("reference too long multi @"+maxReferenceContentLength);
	return ReturnCode.ABORT_PAPER;
    }
    return ReturnCode.OK;
  }

  /**
   * @param contentElement
   * @return
   */
  private String getDescendantText(Element contentElement) {
    Iterator iterator = contentElement.getDescendants( new Filter() {
      public boolean matches(Object o) {
        return (o instanceof Text);
      }
    } );

    StringBuffer contextBuffer = new StringBuffer();
    while (iterator.hasNext()) {
      Text text = (Text)iterator.next();
      contextBuffer.append( text.getText() );
    }

    String contextString = contextBuffer.toString();
    return contextString;
  }
}
