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

/**
 * One way of incorporating the line break properties into features.
 *
 * Created: Oct 18, 2004
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: LineBreakPipe.java,v 1.2 2004/11/12 04:55:43 casutton Exp $
 */
public class LineBreakPipe extends Pipe {

  private static final String LINE_START_KEY = "LINE_START";
  private static final String LINE_IN_KEY = "LINE_IN";
  private static final String LINE_END_KEY = "LINE_END";

  public Instance pipe (Instance carrier)
  {
    TokenSequence data = (TokenSequence) carrier.getData ();
    for (int t = 0; t < data.size(); t++) {
      Token tok = data.getToken (t);

      if (hasTrueProperty (tok, LINE_START_KEY)) {
        tok.setFeatureValue (LINE_START_KEY, 1.0);
      }

      if (hasTrueProperty (tok, LINE_IN_KEY)) {
         tok.setFeatureValue (LINE_IN_KEY, 1.0);
      }

      if (hasTrueProperty (tok, LINE_END_KEY)) {
         tok.setFeatureValue (LINE_END_KEY, 1.0);
      }

    }

    return carrier;
  }


  private boolean hasTrueProperty (Token tok, String key)
  {
    if (!tok.hasProperty (key)) return false;
    boolean value = ((Boolean)tok.getProperty (key)).booleanValue ();
    return value;
  }

}
