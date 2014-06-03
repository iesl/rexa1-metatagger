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
 * @version $Id: FontChangePipe.java,v 1.3 2006/04/24 22:05:49 ghuang Exp $
 */
public class FontChangePipe extends Pipe {

  private static final String fontProp = "FONT";
  private static final String fontChangeProp = "FONT_CHANGED";

  public Instance pipe (Instance instance)
  {
    TokenSequence ts = (TokenSequence) instance.getData ();
    int last = -1000;
    for (int i = 0; i < ts.size(); i++) {
      Token token = ts.getToken (i);
      String fontString = (String) token.getProperty (fontProp);
      if (fontString == null) continue;
      int fontVal = Integer.parseInt (fontString);
      if ((fontVal != last)) {
        token.setFeatureValue (fontChangeProp, 1.0);
        last = fontVal;
      }
    }
    return instance;
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
