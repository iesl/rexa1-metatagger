package org.rexo.extraction;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import org.jdom.Element;
import org.rexo.referencetagging.NewHtmlTokenization;

import java.util.Arrays;
import java.util.List;

/**
 * Author: saunders Created Nov 16, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class CRFOutputFormatter {

	/**
	 *
	 * @param input
	 * @param tokenLabels
	 * @param parentName
	 * @return jdom fragment hierarchically representing the labelled tokens
	 */
	public Element toXmlElement(NewHtmlTokenization input, Sequence tokenLabels, String parentName) {
		Element rootElement = new Element( parentName );
		for (int i = 0; i < input.size(); i++) {
			Span span = (Span)input.get( i );
			String labels = tokenLabels.get( i ).toString();
			// hack: header/reference related fixes
			labels = labels.replaceAll( "author-begin", "authors:^author" );
			labels = labels.replaceAll( "author-inside", "authors:author" );
			String[] labelParts = labels.split( "[:|]" );
			insertToken( rootElement, getSpanText( span ), labelParts );
		}
		return rootElement;
	}

	public Element toXmlElement(String[] tokenStrings, Sequence tokenLabels, String parentName) {
		Element rootElement = new Element( parentName );
		for (int i = 0; i < tokenStrings.length; i++) {
			String span = tokenStrings[i] ;
			String labels = tokenLabels.get( i ).toString();
			// hack: grant-related fixes:
			labels = labels.replaceAll( "B-", "^" );
			labels = labels.replaceAll( "I-", "" );
			String[] labelParts = labels.split( "[:|]" );
			insertToken( rootElement, span + " ", labelParts );
		}
		return rootElement;
	}

	private String getSpanText(Span span) {
		double numericProperty = ((PropertyHolder)span).getNumericProperty( "trailing-ws-1" ) + 1;
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append( span.getText() );
		for (int i = 0; i < numericProperty; i++) {
			stringBuffer.append( " " );
		}
		return stringBuffer.toString();
	}

	/**
	 * insert a token into a jdom tree under the element specified by labelParts
	 * @param parent
	 * @param span
	 * @param labelParts
	 */
	private void insertToken(Element parent, String span, String[] labelParts) {
		if (labelParts.length > 0) {
			String labelPart = labelParts[0];
			Element child;

			if ((child = lastChild( parent )) == null || labelPart.startsWith( "^" ) || !labelPart.equals( child.getName() )) {
				labelPart = labelPart.replaceFirst( "^\\^", "" );
				child = new Element( labelPart );
				parent.addContent( child );
			}
			List tails = Arrays.asList( labelParts ).subList( 1, labelParts.length );
			String[] labelTail = (String[])tails.toArray( new String[tails.size()] );
			insertToken( child, span, labelTail );
		}
		else {
			parent.addContent( span );
		}
	}

	private Element lastChild(Element parent) {
		List children = parent.getChildren();
		return children.isEmpty() ? null : (Element)children.get( children.size() - 1 );
	}

}
