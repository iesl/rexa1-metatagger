/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jun 9, 2004
 * author: adingle
 */


package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;
import org.rexo.util.URLMangler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/* For each document error, link to the document's .ps/.pdf
 * file and .xml file in an '.html' logfile corresponding
 * to the error name.
 */

public class HtmlErrorLogFilter extends AbstractFilter {

  HashSet _errorTypes = new HashSet();

  private void htmlHeader(RxDocument rdoc, String filename, String extension) {
    String header_text = "<html>";

    appendLogFile(rdoc, filename, extension, header_text);
  }

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

    URI fileURI = (URI) rdoc.getScope("document").get("corpus.absolute.uri");
    URI relURI = (URI) rdoc.getScope("document").get("corpus.relative.uri");

    // Get the relative path
    String tmp = relURI.getPath();

    // Chop off the final '.xml'
    tmp = tmp.substring(0, tmp.length()-4);

    // Extract the original URL
    URL u;
    File f;
    try {
      f = new File(tmp);
      u = URLMangler.demangle(f);
    }
    catch (MalformedURLException e) {
      getLogger(rdoc).error( "malformedURL: " + e );
      return ReturnCode.OK;
    }

    String xmllink = "<A href=\"" + fileURI.toString() + "\">" + fileURI.getPath() + "</A>";
    String pslink;

    // Hack -- add a hint about the file type for .ps.gz files.  This
    // is useful for getting mozilla to fire up ghostview properly.  Mileage
    // with other browsers may vary.
    
    if (tmp.endsWith(".ps.gz")) {
      pslink = "<A href=\"" + u.toString() + "\" type=application/postscript>" + u.toString() + "</A>";
    }
    else {
      pslink = "<A href=\"" + u.toString() + "\">" + u.toString() + "</A>";
    }

    for (i = docErrors.iterator(); i.hasNext(); ) {
      String errorType = (String) i.next();
      String logFileName = errorType;
      String logFileExt = ".err.html";

      // if the first error of this type we've seen...
      if (!_errorTypes.contains(errorType)) {
	_errorTypes.add(errorType);
	clearLogFile(rdoc, logFileName, logFileExt);
	htmlHeader(rdoc, logFileName, logFileExt);
      }

      appendLogFile(rdoc, logFileName, logFileExt, pslink + "<br>");
      appendLogFile(rdoc, logFileName, logFileExt, xmllink + "<br><br>");
    }

    return ReturnCode.OK;
  }
}
