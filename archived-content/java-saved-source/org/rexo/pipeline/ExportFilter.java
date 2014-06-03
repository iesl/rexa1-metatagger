/*
 * Created on Feb 10, 2004
 *
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * 
 */
package org.rexo.pipeline;

import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rexo.pipeline.components.RxDocument;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 */
public class ExportFilter extends AbstractFilter {
  /**
   *
   * @param rdoc
   * @return
   */
  public int accept(RxDocument rdoc) {
    Document targetDocument = (Document)rdoc.getScope( "document" ).get( "collated.document" );
    if (targetDocument != null) {
      File exportFile = (File)rdoc.getScope( "document" ).get( "target.canonical.file" );
      FileWriter fileWriter = null;
      try {
        fileWriter = new FileWriter( exportFile );
      }
      catch (IOException e) {
        System.out.println( "can't write to '" + exportFile.getPath() + "'" );
        return ReturnCode.OK;
      }

      XMLOutputter output = new XMLOutputter( Format.getPrettyFormat() );
      try {
        output.output( targetDocument, fileWriter );
        fileWriter.close();
      }
      catch (Exception e) {
        System.out.println( "can't write to '" + exportFile.getPath() + "'" );
      }
    }
    return ReturnCode.OK;
  }
}
