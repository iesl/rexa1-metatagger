/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jun 10, 2004
 * author: adingle
 */

package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/* For each info string, write the document's relative path to a corresponding
 * logfile.
 */

public class InfoLogFilter extends AbstractFilter {

  HashSet _info = new HashSet();

  public int accept(RxDocument rdoc) {
    Iterator i;

    // Get the document's info string list 
    LinkedList docInfo = (LinkedList) rdoc.getScope("document").get("info.list");

    // No info strings registered for this document
    // (perfectly normal)
    if (docInfo == null) {
      return ReturnCode.OK;
    }

    String docId = rdoc.getId();

    // For each info string, append the document's path to the appropriate
    // logfile.
    for (i = docInfo.iterator(); i.hasNext(); ) {
      String infoString = (String) i.next();
      String logFileName = infoString;
      String logFileExt = ".inf.list";
      //appendLogFile(rdoc, logFileName, logFileExt, relativeFilePath);
      appendLogFile(rdoc, logFileName, logFileExt, docId);
    }

    return ReturnCode.OK;
  }
}
