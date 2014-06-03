/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Apr 13, 2004
 * author: asaunders
 */

package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;
import org.rexo.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class MetatagPreconditionTestFilter extends AbstractFilter {
  public int accept(RxDocument rdoc) {
    //
    File corpusPath = (File)rdoc.getScope( "document" ).get( "corpus.path" );
    File targetRootDirectory = (File)rdoc.getScope( "session" ).get( "target.root.directory" );
    File statusFile0 = FileUtils.generateCorpusResourceFile( targetRootDirectory, corpusPath, ".tag.0" );
    File statusFile1 = FileUtils.generateCorpusResourceFile( targetRootDirectory, corpusPath, ".tag.1" );

    String corpusBaseFilename = FileUtils.getCorpusFilename( statusFile0 );

    boolean doReprocess = ((Boolean)rdoc.getScope( "session" ).get( "reprocess.boolean" )).booleanValue();

    //APD
    if (!doReprocess) {
    
      if (statusFile0.exists() || statusFile1.exists()) {
	getLogger(rdoc).warn( "skipping '" + corpusBaseFilename + "': already tagged" );
	//APD report an error message
	//rdoc.docErrorString("Already tagged");
	rdoc.docInfoString("Already tagged");
	return ReturnCode.ABORT_PAPER;
      }

    }

    if (!statusFile0.exists()) {
      statusFile0.getParentFile().mkdirs();
      try {
        if (!statusFile0.createNewFile()) {
          getLogger( rdoc ).error( "aborting '" + corpusBaseFilename + "'; couldn't create status file" );
          rdoc.docInfoString( "STATUS FILE CREATION ERROR" );
          return ReturnCode.ABORT_PAPER;
        }
      }
      catch (IOException e) {
        getLogger( rdoc ).error( "aborting '" + corpusBaseFilename + "'; " + e.getMessage() );
        rdoc.docInfoString( "STATUS FILE CREATION ERROR2" );
        return ReturnCode.ABORT_PAPER;
      }
    }
    //TODO : need to grab a file lock for this to be safe

    incrementIntAttribute( rdoc, "session", "metric.documents.attempted.integer" );
    //System.out.println( corpusPath.getName() + "..." );
    //APD
    System.out.println( "Processing file: " + corpusPath.toString() + " ..." );

    return ReturnCode.OK;
  }
}
