package org.rexo.extraction;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import org.jdom.Element;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Author: saunders Created Nov 16, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class CRFOutputFormatter {
    private static Logger log = Logger.getLogger( CRFOutputFormatter.class );

    //
    private int lastWidth
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
            //kzaporojets: insertTokenPosition also includes the position
			//insertToken( rootElement, getSpanText( span ), labelParts );
            insertTokenPosition(rootElement, getSpanText( span ), labelParts, getSpanBoxCoordinates(span));
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

    private BoxCoordinates getSpanBoxCoordinates(Span span) {
        BoxCoordinates boxCoordinates = new BoxCoordinates();
        boxCoordinates.setLlx(((PropertyHolder) span).getNumericProperty( "llx" ));
        boxCoordinates.setLly(((PropertyHolder) span).getNumericProperty("lly"));
        boxCoordinates.setUrx(((PropertyHolder) span).getNumericProperty("urx"));
        boxCoordinates.setUry(((PropertyHolder) span).getNumericProperty("ury"));
        boxCoordinates.setPageNum( (int)((PropertyHolder) span).getNumericProperty("pageNum"));
        return boxCoordinates;
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


    /**
     * kzaporojets
     * insert a token into a jdom tree under the element specified by labelParts, includes the position of the element
     * @param parent
     * @param span
     * @param labelParts
     */
    private void insertTokenPosition(Element parent, String span, String[] labelParts, BoxCoordinates positionSpan) {
        //associate position here
        adjustPosition(parent, positionSpan);
        //end associate position
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
            //associate position here

            //end associate position
            insertTokenPosition(child, span, labelTail, positionSpan);
        }
        else {
            parent.addContent( span );
        }
    }

    //adjust the position
    private void adjustPosition(Element elem, BoxCoordinates pos)
    {
        try {
            //if suddenly all the attributes change abruptly, and we are in references section, don't change them:
            //probably its just a new column. Won't deel with it for now.
            if (elem.getAttribute("llx")!=null && elem.getName().equals("reference"))
            {
                if(Math.abs(elem.getAttribute("lly").getDoubleValue() - pos.getLly())>400 &&
                        Math.abs(elem.getAttribute("ury").getDoubleValue() - pos.getUry())>400)
                {
                    return;
                }
            }
            if (elem.getAttribute("llx") == null || elem.getAttribute("llx").getDoubleValue() > pos.getLlx()) {
                elem.setAttribute("llx", String.valueOf(pos.getLlx()));
            }
            if (elem.getAttribute("lly") == null || elem.getAttribute("lly").getDoubleValue() > pos.getLly()) {
                elem.setAttribute("lly", String.valueOf(pos.getLly()));
            }
            if (elem.getAttribute("urx") == null || elem.getAttribute("urx").getDoubleValue() < pos.getUrx()) {
                elem.setAttribute("urx", String.valueOf(pos.getUrx()));
            }
            if (elem.getAttribute("ury") == null || elem.getAttribute("ury").getDoubleValue() < pos.getUry()) {
                elem.setAttribute("ury", String.valueOf(pos.getUry()));
            }
            if (elem.getAttribute("pageNum") == null) {
                elem.setAttribute("pageNum", String.valueOf(pos.getPageNum()));
            }
        }
        catch(org.jdom.DataConversionException ex)
        {
            log.error( ex.getClass().getName() + ": " + ex.getMessage() );
        }
    }

	private Element lastChild(Element parent) {
		List children = parent.getChildren();
		return children.isEmpty() ? null : (Element)children.get( children.size() - 1 );
	}

}
