/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Apr 15, 2004
 * author: asaunders
 */

package org.rexo.util;



public class IDGenerator {
  private long _lastID = 0;
  private static IDGenerator _instance = new IDGenerator();

  /**
   * Creates and returns a new unique object ID of type long.
   * The ID is generated using the Date's getTime() method.
   */
  public synchronized long createTimestampID() {
    long id = System.currentTimeMillis();
    while (id == _lastID) {
      try {
        Thread.sleep( 1 );
      }
      catch (InterruptedException e) {}
      id = System.currentTimeMillis();
    }
    _lastID = id;
    return id;
  }

  public static String stringValue(long id) {
    return Long.toString( id );
  }

  public static long longValue(String id) {
    return Long.parseLong( id );
  }

  public static IDGenerator instance() {
    return _instance;
  }

  static public class IDGenerationException extends Exception {
    public IDGenerationException(String message) {
      super( message );
    }
  }
}


