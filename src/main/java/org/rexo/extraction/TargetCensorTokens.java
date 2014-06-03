/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package org.rexo.extraction;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.pipe.Pipe;

import java.util.List;
import java.util.Arrays;

/**
 * Created: Nov 17, 2004
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: TargetCensorTokens.java,v 1.3 2006/04/24 22:05:49 ghuang Exp $
 */
public class TargetCensorTokens extends Pipe {

  private String background;
  private List keepList;

  public TargetCensorTokens (String[] keep, String background) {
    this.keepList = Arrays.asList (keep);
    this.background = background;
  }

  public Instance pipe (Instance carrier)
  {
    TokenSequence tokseq = (TokenSequence) carrier.getTarget ();
    if (tokseq == null) return carrier;
    
    TokenSequence newtarget = new TokenSequence (tokseq.size());

    for (int t = 0; t < tokseq.size(); t++) {
      Token token = tokseq.getToken (t);
        if (keepList.contains (token.getText ())) {
          newtarget.add (token);
        } else {
          newtarget.add (background);
        }
      }

    assert newtarget.size() == tokseq.size ();
    carrier.setTarget (newtarget);

    return carrier;
  }

}
