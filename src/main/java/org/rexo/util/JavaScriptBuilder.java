/*
 * Copyright 2006 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Mar 9, 2006 by atolopko
 */
package org.rexo.util;

import java.util.ArrayList;

public class JavaScriptBuilder
{

  public static String buildArrayInstantiationCode(Object[] obj) {
    String defaultBegin = "new Array(";
    String defaultEnd = ")";
    if (obj == null)
      return defaultBegin + defaultEnd;
    else if (obj.length == 0)
      return defaultBegin + defaultEnd;
    else {
      String toRet = defaultBegin;
  
      if (obj.length > 0)
        toRet += "" + obj[0];
      for (int i = 1; i < obj.length; i++)
        toRet += "," + obj[i];
      toRet += defaultEnd;
      
      return toRet;
    }
  }

  public static Object[] buildArrayOfStrings(Object[] obj) {
    if (obj == null)
      return obj;
    
    ArrayList toRet = new ArrayList();
    for (int i = 0; i < obj.length; i++)
      toRet.add(buildQuotedString(obj[i]));
  
    return toRet.toArray();
  }

  public static String buildQuotedString(Object obj) {
    if (obj == null) return "''";
    else if (obj instanceof Object[]) {
      if (((Object[]) obj)[0] != null) {
        String str = "" + ((Object[]) obj)[0];
        str = str.replaceAll("'"," ");
        return "'" + str + "'";
      }
      else return "''";
    }
    else if (obj instanceof Object) {
      String str = "" + obj;
      str = str.replaceAll("'"," ");
      return "'" + str + "'";
    }
    else return "''";
  }

}
