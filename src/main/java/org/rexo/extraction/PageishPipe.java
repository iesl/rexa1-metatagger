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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Adds features for numbers that look like pages.
 *
 * Created: Nov 4, 2004
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: PageishPipe.java,v 1.3 2006/04/24 22:05:49 ghuang Exp $
 */
public class PageishPipe extends Pipe {

  private static Pattern pagesInOnePtn = Pattern.compile ("(\\d+)-(\\d+)");
  private static Pattern allDigitsPtn = Pattern.compile ("\\d+");

  private void addPagishFeature (Token token)
  {
    token.setFeatureValue ("PAGEISH", 1);
  }

  public Instance pipe (Instance carrier)
  {
    TokenSequence input = (TokenSequence) carrier.getData ();
    int N = input.size();
    for (int t = 0; t < N; t++) {
      Token token = input.getToken (t);
      String text = token.getText ();
      Matcher matcher = pagesInOnePtn.matcher(text);
      if (matcher.matches()) {
        int from = Integer.parseInt (matcher.group (1));
        int to = Integer.parseInt (matcher.group (2));
        if (from < to) {
          addPagishFeature (token);
        }
      } else if (t < N - 2 && allDigitsPtn.matcher(text).matches()) {
        Token tokenAt1 = input.getToken (t+1);
        Token tokenAt2 = input.getToken (t+2);
        String textAt1 = tokenAt1.getText ();
        String textAt2 = tokenAt2.getText ();
        if (textAt1.equals ("-") && allDigitsPtn.matcher (textAt2).matches()) {
          addPagishFeature (token);
          addPagishFeature (tokenAt1);
          addPagishFeature (tokenAt2);
        }
      }
    }
    return carrier;
  }

}
