/*
 * Created on Feb 10, 2004
 *
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * 
 */
package org.rexo.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Timestamp {

  // prints out a timestamp for logfiles. Perhaps later will take constructor
  // that indicates what format the timestamp should be in?

  public Timestamp() {
  }

  public String stamp() {
    Date now = new Date(System.currentTimeMillis());
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd H:mm:ss:SSS", Locale.US);
    return formatter.format(now) + " ";
  }

}
