/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 3, 2004
 * author: asaunders
 */

package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;

import java.io.File;

public class PipelineControlFilter extends AbstractFilter {
  private File _stopFile = new File( "stop" );
  private File _statusFile = null;
  private String _controlString = "pipeline";

  protected PipelineControlFilter() {
  }

  public PipelineControlFilter(String controlString) {
    _controlString = controlString;
    _stopFile = new File( "stop-" + controlString );
  }

  public PipelineControlFilter(File stopFile) {
    _stopFile = stopFile;
  }

  public int accept(RxDocument rdoc) {

    if (_stopFile.exists()) {
      return ReturnCode.ABORT_SESSION;
    }

    return ReturnCode.OK;
  }
}
