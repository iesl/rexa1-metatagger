package org.rexo.extraction;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.util.PropertyList;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/* A hierarchical wrapper around 'StringMultiSpan'. */

public class HSpans {
	private String name;
	private HSpans parent;
	private ArrayList children = new ArrayList();
	private HashMap nameMap = new HashMap();

	private StringMultiSpan span;
	private NewHtmlTokenization tokenization;
	
	private PropertyList properties = null;

	public HSpans(String name, NewHtmlTokenization tokenization, Span span) {
		this(name, tokenization, new StringMultiSpan((CharSequence)tokenization.getDocument(), span));
	}

	public HSpans(String name, NewHtmlTokenization tokenization, StringMultiSpan span) {
		this.name = name;
		this.span = span;
		this.tokenization = tokenization;
	}

	public HSpans(String name, NewHtmlTokenization tokenization, StringMultiSpan span, List childList) {
		this(name, tokenization, span);
		if (childList != null) {
			Iterator childI = children.iterator();
			while (childI.hasNext()) {
				addChild( (HSpans)childI.next() );
			}
		}
	}

	public HSpans getParent() {
		return parent;
	}

	public ArrayList getChildren() {
		return children;
	}

	private ArrayList nameList(String name) {
		if (!nameMap.containsKey( name )) {
			nameMap.put( name, new ArrayList() );
		}
		return (ArrayList)nameMap.get( name );
	}

	public HSpans getChild(String name) {
		List childList = getChildren( name );
		return (childList.size() > 0) ? (HSpans)childList.get( 0 ) : null;
	}

	public ArrayList getChildren(String name) {
		return nameList( name );
	}

	public ArrayList getDescendants(String name) {
		ArrayList ret = new ArrayList();
		findDescendants( name, ret );
		return ret;
	}

	private void findDescendants(String name, ArrayList descendants) {
		Iterator childI = children.iterator();

		// Add all descdents of this node's children
		while (childI.hasNext()) {
			((HSpans)childI.next()).findDescendants( name, descendants );
		}

		// Add all children of type 'name' of this node.
		descendants.addAll( nameList( name ) );
	}

	protected void setParent(HSpans parent) {
		this.parent = parent;
	}

	public void addChild(HSpans child) {
		children.add( child );
		child.setParent( this );
		nameList( child.getName() ).add( child );
		//System.out.println("child.getName:"+child.getName()+"size="+nameList(child.getName()).size());
	}

	public void addChild(String name, StringMultiSpan childSpan) {
		children.add( new HSpans( name, this.tokenization, childSpan ) );
	}

	public void addChildren(List addList) {
		Iterator addI = addList.iterator();
		while (addI.hasNext()) {
			addChild( (HSpans)addI.next() );
		}
	}

	public StringMultiSpan getMultiSpan() {
		return span;
	}

	// This method has been modified to, instead of returning the spanned text directly,
	// return each in
	public String getText() {
		//return span.getText();

		StringBuffer text = new StringBuffer();
		int prevEndIdx = -1;
		for (int i = 0; i < span.size(); i++) {
			Span componentSpan = span.getComponent(i);
			int[] tokenBounds = findTokenBoundaries(tokenization, componentSpan);
			for (int j = tokenBounds[0]; j < tokenBounds[1]; j++) {
				StringSpan token = (StringSpan) tokenization.getToken(j);
				// Add a space if this isn't the first token and some characters were
				// skipped between the previous tokens.
				if (prevEndIdx != -1 && token.getStartIdx() > prevEndIdx) {
					text.append(" ");
				}
				text.append(token.getText());
				prevEndIdx = token.getEndIdx();
			}
		}
		return text.toString();
	}

	private static int[] findTokenBoundaries(NewHtmlTokenization tokens, Span range) {
		int firstToken, lastToken;

		assert tokens.size() > 0;
		// Verify span does not occur entirely before the start of the tokenization
		assert range.getEndIdx() >= ((StringSpan)tokens.getToken(0)).getStartIdx();
		// Verify span does not occur entirely after the end of the tokenization
		assert range.getStartIdx() <= ((StringSpan)tokens.getToken(tokens.size()-1)).getEndIdx()-1;

    // Binary search for the first token ending after (inclusively) startIdx
    // 'low' represents the highest known token ending before the span begins
		int high = tokens.size()-1;
    // Initialize low out of bounds since token '0' may be in the span
    int low = -1;
    int middle;
    while (true) {
      middle = (low+high)/2;
      // -1 since getEndIdx is an exclusive limit
      if (((StringSpan)tokens.getToken(middle)).getEndIdx()-1 < range.getStartIdx())
				low = middle;
      else
				high = middle;

      if (high == low+1)
				break;
    }
    firstToken = high;

    // Binary search for the first token starting after (inclusivly) endIdx
    // 'low' represents the highest known token starting before the span ends
		low=0;
		// Initialize high out of bounds since the last token may be in the span
    high = tokens.size();
    
    while (true) {
      middle = (low+high)/2;
      // check if token starts prior to the end of the span
      if (((StringSpan)tokens.getToken(middle)).getStartIdx() < range.getEndIdx())
				low = middle;
      else
				high = middle;

      if (high == low+1)
				break;
    }
    lastToken = high;

		return new int[] {firstToken, lastToken};
	} 

	public CharSequence getDocument() {
		return (CharSequence)span.getDocument();
	}

	public String getName() {
		return name;
	}

	public void setProperty(String key, Object value) {
		properties = PropertyList.add( key, value, properties );
	}

	public void setNumericProperty(String key, double value) {
		properties = PropertyList.add( key, value, properties );
	}

	public PropertyList getProperties() {
		return properties;
	}

	public Object getProperty(String key) {
		return properties == null ? null : properties.lookupObject( key );
	}

	public double getNumericProperty(String key) {
		return (properties == null ? 0.0 : properties.lookupNumber( key ));
	}

	public boolean hasProperty(String key) {
		return (properties != null && properties.hasProperty( key ));
	}


	// Added as a temporary check on validity of loaded spans
	public static class LoadingException extends Exception {
		LoadingException(String msg) {
			super(msg);
		}
	}

	// Constructor from jdom element
	// FIXME: in the course of saving/loading, property order seems to get reversed
	public HSpans(NewHtmlTokenization docTok, Element e) throws LoadingException {
		this( e.getAttributeValue( "name" ), docTok, new StringMultiSpan( (CharSequence) docTok.getDocument(), e.getChild( "SMultiSpan" ) ) );
		assert e.getName().equals( "HSpans" );
		// Set properties
		Iterator attributeI = e.getAttributes().iterator();
		while (attributeI.hasNext()) {
			Attribute property = (Attribute)attributeI.next();
			String propName = property.getName();
			if (propName.endsWith( "Number" )) {
				propName = propName.substring( 0, propName.length() - "Number".length() );
				this.setNumericProperty( propName, Double.parseDouble( property.getValue() ) );
			}
			else if (propName.endsWith( "String" )) {
				propName = propName.substring( 0, propName.length() - "String".length() );
				this.setProperty( propName, property.getValue() );
			}
		}
		// Add children
		List childrenElts = e.getChildren( "HSpans" );
		Iterator childI = childrenElts.iterator();
		while (childI.hasNext()) {
			addChild( new HSpans( docTok, (Element)childI.next() ) );
		}
		// Check validity of loaded text
		// String thisText = this.span.getText().toString().replaceAll("\\s+", "");
		// String loadedText = getMultiSpanElementText(e).replaceAll("\\s+", "");
		// if (! thisText.equals(loadedText)) {
		// 	log.warn( "loaded text: "+loadedText);
		// 	log.warn("internal text: "+thisText);
		// 	// throw new LoadingException("Loaded text for span '"+name+"' does not match internal text.  Misaligned span?");
		// }
	}

	// Used exclusivly for checking for the above exception.  
	private static String getMultiSpanElementText(Element hspansE) {
		StringBuffer appendedText = new StringBuffer();
		List spans = hspansE.getChild("SMultiSpan").getChildren("StringSpan");
		Iterator spanI = spans.iterator();
		while (spanI.hasNext()) {
			Element span = (Element)spanI.next();
			appendedText.append(span.getText());
		}
		return appendedText.toString();
	}

	public Element toJdomElement() {

		Element ret = new Element( "HSpans" );

		// Set name
		ret.setAttribute( "name", name );

		// Add span
		Element spanE = span.toJdomElement();
		ret.addContent( spanE );
		// Store properties in attributes
		if (properties != null) {
			PropertyList.Iterator propertyI = properties.iterator();
			while (propertyI.hasNext()) {
				PropertyList next = (PropertyList)propertyI.next();
				// System.out.println( "got property!!" );

				String attributeName;
				if (propertyI.isNumeric()) {
					attributeName = propertyI.getKey() + "Number";
					ret.setAttribute( attributeName, Double.toString( propertyI.getNumericValue() ) );
				}
				else {
					// System.out.println( "got non-numeric property" );
					Object value = propertyI.getObjectValue();
					if (value instanceof CharSequence) {
						attributeName = propertyI.getKey() + "String";
						String attributeText = ((CharSequence)value).toString();
						// System.out.println( "got property name:" + attributeName );
						// System.out.println( "got property value:" + attributeText );
						ret.setAttribute( attributeName, attributeText );
					}
				}
			}
		}
		for (int i = 0; i < children.size(); i++) {
			HSpans child = (HSpans)children.get( i );
			ret.addContent( child.toJdomElement() );
		}

		return ret;
	}
}
