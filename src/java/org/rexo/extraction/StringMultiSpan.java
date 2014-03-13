/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package org.rexo.extraction;

import org.jdom.CDATA;
import org.jdom.Element;
import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.types.Token;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


/* Adapted from edu.umass.cs.mallet.base.extract.StringSpan */
/* Preliminary class.  May change, disappear, and/or may be refactored into Mallet sometime.
  -APD
*/
public class StringMultiSpan extends Token implements MultiSpan
{
  private CharSequence document;  // The larger string of which this is a span.
  private ArrayList components;

  public StringMultiSpan (CharSequence doc, int start, int end)
  {
    //is there a better way to initialize a list containing a new 'stringspan'?
    this(doc, Arrays.asList(new Object[] {new StringSpan(doc, start, end)}));
  }
	public StringMultiSpan (CharSequence doc, Span component)
  {
    super (component.getText());
    this.document = doc;
    this.components = new ArrayList();
		this.components.add(component);
  }

  public StringMultiSpan (CharSequence doc, List components)
  {
    super (constructTokenText (doc, components));
    this.document = doc;
    this.components = new ArrayList(components);
    assert this.components.size() > 0;
  }

  public StringMultiSpan (CharSequence doc, Element e) {
    this(doc, extractComponents(doc, e));
  }

  private static List extractComponents(CharSequence doc, Element e) {
    List ret = new LinkedList();
    Iterator childI = e.getChildren("StringSpan").iterator();
    while (childI.hasNext()) {
      Element spanElt = (Element) childI.next();
      int spanStart = Integer.parseInt(spanElt.getAttributeValue("start"));
      int spanEnd = Integer.parseInt(spanElt.getAttributeValue("end"));
      ret.add(new StringSpan(doc, spanStart, spanEnd));
    }
    return ret;
  }

  // TODO verify components don't intesect
  private static String constructTokenText (CharSequence doc, List components)
  {
    //CharSequence subseq = doc.subSequence(start,end);
    StringBuffer componentText = new StringBuffer();
    for (int i = 0; i < components.size(); i++) {
      Span component = (Span) components.get(i);
      componentText.append(component.getText());
    }
    return componentText.toString();
  }

  public Object getDocument ()
  {
    return document;
  }

	public Span getComponent(int i) {
    return (Span) components.get(i);
  }
  public ArrayList getComponents() {
    return components;
  }
	public int size() {
		return components.size();
	}

  public Element toJdomElement() {
    Element ret = new Element("SMultiSpan");
    for (int i = 0; i < components.size(); i++) {
      Span s = (Span) components.get(i);
      Element componentE = new Element("StringSpan");
      componentE.setAttribute("start", Integer.toString(s.getStartIdx()));
      componentE.setAttribute("end", Integer.toString(s.getEndIdx()));
      componentE.addContent(new CDATA(s.getText()));
      ret.addContent(componentE);
    }
    return ret;
  }

  // public boolean intersects (Span r)
//   {
//     if (!(r instanceof StringSpan))
//       return false;
//     StringSpan sr = (StringSpan)r;
//     return (sr.document == this.document && !(sr.end < this.start || sr.start > this.end));
//   }


//   public boolean isSubspan (Span r)
//   {
//     return (r.getDocument() == this.document &&
// 	    (this.start <= r.getStartIdx ()) && (r.getEndIdx () <= this.end));
//   }

  // public int getStartIdx () { return start; }

//   public int getEndIdx () { return end; }

  // public String toString() {
//     return super.toString() + "  span["+start+".."+end+"]";
//   }

}