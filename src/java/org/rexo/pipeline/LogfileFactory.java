/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jun 15, 2004
 * author: adingle
 */

package org.rexo.pipeline;

import org.apache.log4j.Logger;
import org.rexo.pipeline.components.RxDocument;

import java.io.File;

public class LogfileFactory {

    private final static int MAX_SID = 300;

    /* Creates a new session ID file and returns the session ID */
    private static int newSessionID(RxDocument rdoc) {
      File logRoot = (File) rdoc.getScope("session").get("log.directory");
      File IDFile;
      int sID;

      Logger l = Logger.getLogger( "org.rexo.pipeline.LogfileFactory");
      l.info("Initializing sessionID...");

      // find the first free ID, and create the proper log directory
      // FIXME: this won't always work over NFS, use proper locking mechanism
      for (sID = 0; sID <= MAX_SID; sID++) {
	File IDDir = new File(logRoot, "/" + sID);
        if (!IDDir.exists()) {
	    // create the ID directory
	    if (!IDDir.mkdirs()) {
	      l.warn("newSessionID: unable to create directory" + IDDir);
	      continue;
	    }
	    /*
	    // create the ID file (currently unused)
	    IDFile = new File(IDDir, "SID" + "." + sID);
	    try {
	      if (!IDFile.createNewFile()) {
	        l.warn("newSessionID: unable to create sessionID file " + IDFile);
		continue;
	      }
	    }
	    catch (IOException e) {
	      l.error("newSessionID: " + e.getMessage());
	      continue;
	    }
	    */

	    break;
        } 
      }

      // TODO: output useful information in ID file
      l.info("Got sessionID="+sID);

      return sID;
    }

    private static int getSessionID(RxDocument rdoc) {
      Integer sID = (Integer) rdoc.getScope("session").get("sessionID.integer");
      if (sID == null) {
	Logger l = Logger.getLogger( "org.rexo.pipeline.LogfileFactory");
	l.info("getSessionID: 'sessionID.integer' not found in session scope");
	return MAX_SID+1;
      }
      return sID.intValue();
    }


    public static File getLogfile(RxDocument rdoc, String name, String extension) {
      File logRoot = (File) rdoc.getScope("session").get("log.directory");
      File logFile;

      // Get session ID
      int sID = getSessionID(rdoc);

      // Initialize session ID if necessary
      if (sID == -1) {
	sID = newSessionID(rdoc);
	rdoc.getScope("session").put("sessionID.integer", new Integer(sID));
      }

      // Build pathname
      // Form = name + .ID + extension
      name= name.replace(' ', '_');
      extension = extension.replace(' ', '_');
      //logFile = new File(logRoot, name + "." + sID + extension);
      logFile = new File(logRoot, sID + "/" + name + "." + sID + extension);
      return logFile;
    }
}
