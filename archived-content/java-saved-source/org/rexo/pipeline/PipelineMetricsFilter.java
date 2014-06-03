/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Apr 22, 2004
 * author: asaunders
 */

package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class PipelineMetricsFilter extends AbstractFilter {
  private TreeMap infoCount = new TreeMap();
  private TreeMap errorCount = new TreeMap();

  /* Increments a message count */
  private void incCount(TreeMap c, String s) {
    long oldCount = 0;
    if (c.containsKey(s)) {
      oldCount = ((Long) c.get(s)).longValue();
    }
    c.put(s, new Long(oldCount+1));
  }

  /* Produces the formatted content of the metrics file */
  private String metricContent(RxDocument rdoc) {
    StringBuffer content = new StringBuffer();

    int attempted = getIntAttribute( rdoc,
                                     "session",
                                     "metric.documents.attempted.integer" );
    int totalIterations = getIntAttribute( rdoc,
                                           "session",
                                           "pipeline.progress.iteration.integer" );
    int successCount = getIntAttribute( rdoc,
                                        "session",
                                        "metric.corpus.iteration.documents.succeeded.integer" );
    int iterationsOverCorpus = getIntAttribute( rdoc,
                                                "session",
                                                "metric.corpus.iteration.integer" );

    // Summary
    content.append("1. Summary" + "\n");
    content.append("\tNo. of papers attempted: " + attempted + "\n"); 
    content.append("\tPipeline iterations: " + totalIterations + "\n" );
    content.append("\tSuccessfully processed documents: " + successCount + "\n" );
    content.append("\tNo. of scans over corpus: " + iterationsOverCorpus + "\n" );

    // Info & Errors
    NumberFormat pFormat = new DecimalFormat("##0.0%");

    // begin with 'Info'
    Map countMap = infoCount;
    Iterator keyIter = countMap.keySet().iterator();
    content.append("2. Info\n");

    while (keyIter.hasNext()) {
      // get next message
      String key = (String) keyIter.next();
      // get message info
      long count = ((Long)countMap.get(key)).longValue();
      long total;
      String percent;

      // (hack -- special treatment for 'Already tagged')
      if (key.equals("Already tagged")) {
	total = totalIterations;
	percent = "";
      }
      else {
	total = attempted;
	percent = attempted > 0 ? ":\t"+pFormat.format((double)count/(double)total) : "";
      }

      content.append("\t"+key+" - "+count+"/"+total+percent+"\n");

      // after 'Info' is done, switch to 'Errors'
      if (!keyIter.hasNext() && countMap == infoCount) {
	countMap = errorCount;
	keyIter = countMap.keySet().iterator();
        content.append("3. Errors\n");
      }
    }

    return content.toString();
  }

  /* Dumps metrics to stdout */
  public void printMetrics(RxDocument rdoc) {
    String content = metricContent(rdoc);
    System.out.print(content);
  }

  /* Updates info/error message counts and rewrites the metrics file appropriately */
  public int accept(RxDocument rdoc) {
    incrementIntAttribute( rdoc, "session", "pipeline.progress.iteration.integer" );

    // Update infoCount, errorCount
    LinkedList docInfo = (LinkedList) rdoc.getScope("document").get("info.list");
    if (docInfo != null) {
      for (Iterator i = docInfo.iterator(); i.hasNext(); ) {
	incCount(infoCount, (String) i.next());
      }
    }
    LinkedList docErrors = (LinkedList) rdoc.getScope("document").get("error.list");
    if (docErrors != null) {
      for (Iterator i = docErrors.iterator(); i.hasNext(); ) {
	incCount(errorCount, (String) i.next());
      }
    }

    // Create content
    String content = metricContent(rdoc);
    // Create the metrics file
    clearLogFile( rdoc, "metrics", ".inf" );
    // Output to file
    appendLogFile( rdoc, "metrics", ".inf", content);

    return ReturnCode.OK;
  }

}
