/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package org.rexo.extraction;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.types.Token;

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created: Nov 13, 2004
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: BiggestFontPipe.java,v 1.2 2004/11/23 06:27:56 casutton Exp $
 */
public class BiggestFontPipe extends Pipe {

  private static final String fontProp = "FONT";
  private static final String largestFontProp = "LARGEST_FONT";

  public Instance pipe (Instance instance)
  {
    TokenSequence ts = (TokenSequence) instance.getData ();
    String largeString = findLargestFont (ts);
    for (int i = 0; i < ts.size(); i++) {
      Token token = ts.getToken (i);
      String fontString = (String) token.getProperty (fontProp);
      if (largeString.equals (fontString)) {
        token.setFeatureValue (largestFontProp, 1.0);
      }
    }
    return instance;
  }


  private String findLargestFont (TokenSequence ts)
  {
    int largeVal = 120; // Initial default value, according to mm
    String largeFontString = "120";
    for (int i = 0; i < ts.size(); i++) {
      Token token = ts.getToken (i);
      String fontString = (String) token.getProperty (fontProp);
      if (fontString == null) continue;
      int fontVal = Integer.parseInt (fontString);
      if (fontVal > largeVal) {
        largeVal = fontVal;
        largeFontString = fontString;
      }
    }
    return largeFontString;
  }

  // Serialization nonsense

  private static final long serialVersionUID = 1;
  private static final int CURRENT_SERIAL_VERSION = 1;

  private void writeObject (ObjectOutputStream out) throws IOException {
    out.defaultWriteObject ();
    out.writeInt (CURRENT_SERIAL_VERSION);
  }

  private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject ();
    int version = in.readInt();
  }

}
