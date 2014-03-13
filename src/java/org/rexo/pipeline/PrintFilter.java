/*
 * Created on Feb 11, 2004
 *
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * 
 */
package org.rexo.pipeline;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;
import org.rexo.pipeline.components.RxPipeline;

/**
 */
public class PrintFilter implements RxFilter {

  /* (non-Javadoc)
   * @see org.rexo.pipeline.components.RxFilter#accept(org.rexo.pipeline.components.RxDocument)
   */
  public int accept(RxDocument rdoc) {

    XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());
    Element docRoot = rdoc.getDocument();
    try {
      output.output(docRoot, System.out);
    }
    catch (Exception e) {
      // TODO: handle exception
    }
    return ReturnCode.OK;
  }

  public int init(RxPipeline pipeline) {
    return ReturnCode.OK;
  }
}
