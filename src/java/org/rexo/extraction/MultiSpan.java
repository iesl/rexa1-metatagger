/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

// Rename to Segment, (then also Segmentation, SegmentSequence, SegmentList)
// Alternatively, think about names: Annotation, AnnotationList, 

package org.rexo.extraction;

public interface MultiSpan
{

  /** Returns a textual representatio of the span, suitable for XML output, e.g. */
  String getText ();

  Object getDocument ();

  // boolean intersects (Span r);

//   boolean isSubspan (Span r);

  // Returns a list of the spans that make up this multi-span.
  public java.util.ArrayList getComponents();

  /**
   *  Returns an integer index identifying the start of this span.
   *   Beware that in some cases (e.g., for images), this may not
   *   correspond directly to a sequence index.
   */
  // Disabled for now for multispans -APD
  //int getStartIdx ();


  /**
   *  Returns an integer index identifying the end of this span.
   *   Beware that in some cases (e.g., for images), this may not
   *   correspond directly to a sequence index.
   */
  //int getEndIdx ();

}
