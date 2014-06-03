/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jun 7, 2004
 * author: adingle
 */

package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/* For each document error, write the document's relative path to a '.list'
 * logfile corresponding to the error name.
 */

public class ErrorLogFilter extends AbstractFilter {

  HashSet _errorTypes = new HashSet();

  public int accept(RxDocument rdoc) {
    Iterator i;

    // Get the document's error list 
    LinkedList docErrors = (LinkedList) rdoc.getScope("document").get("error.list");

    // No error strings registered for this document
    if (docErrors == null) {
      /*
      docErrors = new LinkedList();
      docErrors.add("unreported error");
      */
      return ReturnCode.OK;
    }

    // Get the document's relative path
/*
    URI relURI = (URI) rdoc.getScope("document").get("corpus.relative.uri");
    String relativeFilePath = relURI.getPath();
*/
    String docId = rdoc.getId();

    // For each error, append the document's path to the appropriate logfile.
    for (i = docErrors.iterator(); i.hasNext(); ) {
      String errorType = (String) i.next();
      String logFileName = errorType;
      String logFileExt = ".err.list";

/*
      // If the first time we've seen this error type
      if (!_errorTypes.contains(errorType)) {
	_errorTypes.add(errorType);
	clearLogFile(rdoc, logFileName);
      }
*/

      //appendLogFile(rdoc, logFileName, logFileExt, relativeFilePath );
      appendLogFile(rdoc, logFileName, logFileExt, docId );
    }

    return ReturnCode.OK;
  }
}
