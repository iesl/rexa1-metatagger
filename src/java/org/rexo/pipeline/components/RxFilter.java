/*
 * Created on Feb 4, 2004
 *
 */
package org.rexo.pipeline.components;



/**
 * @author asaunders
 */
public interface RxFilter {

  public static class ReturnCode {
    static public final int OK = 0;
    static public final int WARNING = 1;
    static public final int ABORT_PAPER = 2;
    static public final int ABORT_SESSION = 3;
    static public final int ERROR = 4;
  }

  public int accept(RxDocument rdoc);
  public int init(RxPipeline pipeline);
}
