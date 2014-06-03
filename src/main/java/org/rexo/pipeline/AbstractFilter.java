/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Apr 22, 2004
 * author: asaunders
 */

package org.rexo.pipeline;

import org.apache.log4j.Logger;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;
import org.rexo.pipeline.components.RxPipeline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public abstract class AbstractFilter implements RxFilter {
  protected AbstractFilter() {
  }

  protected Logger getLogger(RxDocument rdoc) {
    return Logger.getLogger( this.getClass() );
  }

  //APD
  protected void appendLogFile(RxDocument rdoc, String name, String extension, String text) {
    File logFile = LogfileFactory.getLogfile(rdoc, name, extension);
    writeLogFile(rdoc, logFile, text, true);
  }

  protected void clearLogFile(RxDocument rdoc, String name, String extension) {
    File logFile = LogfileFactory.getLogfile(rdoc, name, extension);
    writeLogFile(rdoc, logFile, null, false);
  }

  private void writeLogFile(RxDocument rdoc, File logFile, String text, boolean append) {
    
    // Create the parent directory if necessary
    if (!logFile.getParentFile().exists()) {
      if (!logFile.getParentFile().mkdirs()) {
	getLogger(rdoc).error("writeLogFile: unable to create directory " + logFile.getParentFile());
	return;
      }
    }

    // Create the file if necessary
    if (!logFile.exists()) { 
      try {
	getLogger(rdoc).info( "writeLogFile: creating file " + logFile );
	if (!logFile.createNewFile()) {
	  getLogger(rdoc).error( "writeLogFile: couldn't create log file " + logFile );
	  return;
	}
      }
      catch (IOException e) {
	getLogger(rdoc).error("writeLogFile: " + e.getMessage());
	return;
      }
    }
    // Append to the logfile
    try {
      // open the logfile for appending
      PrintWriter w = new PrintWriter(new FileWriter(logFile, append));
      // Write the relative file path
      if (text != null) {
        w.println(text);
      }
      // close the file
      w.close();
    }
    catch (IOException e) {
      getLogger( rdoc ).error( e.getMessage() );
    }
  }

  protected void incrementIntAttribute(RxDocument rdoc, String scope, String attribute) {
    setIntAttribute( rdoc, scope, attribute, getIntAttribute( rdoc, scope, attribute ) + 1 );
  }

  protected int getIntAttribute(RxDocument rdoc, String scope, String attribute) {
    Map scopeMap = rdoc.getScope( scope );
    Integer value = (Integer)scopeMap.get( attribute );
    if ( value == null ) {
      scopeMap.put( attribute, new Integer( 0 ) );
      return 0;
    }
    return value.intValue();
  }

  protected void setIntAttribute(RxDocument rdoc, String scope, String attribute, int value) {
    Map scopeMap = rdoc.getScope( scope );
    scopeMap.put( attribute, new Integer( value ) );
  }

  public int init(RxPipeline pipeline) {
    return ReturnCode.OK;
  }

}
