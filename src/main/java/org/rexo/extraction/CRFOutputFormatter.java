package org.rexo.extraction;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.rexo.span.CompositeSpan;

/**
 * Author: saunders Created Nov 16, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class CRFOutputFormatter {
    private static Logger log = Logger.getLogger( CRFOutputFormatter.class );

    //
    //private int lastWidth = 0;
	/**
	 *
	 * @param input
	 * @param tokenLabels
	 * @param parentName
	 * @return jdom fragment hierarchically representing the labelled tokens
	 */
	public Element toXmlElement(NewHtmlTokenization input, Sequence tokenLabels, String parentName) {
		Element rootElement = new Element( parentName );
        List<BoxCoordinates> columns = null;
        int currentColumn = 1;
        //kzaporojets: gets the columns and its coordinates in terms of the width
        if(parentName.equals("reference")) {
            columns = getColumnData(input.getLineSpans());
        }

		for (int i = 0; i < input.size(); i++) {
			Span span = (Span)input.get( i );
			String labels = tokenLabels.get( i ).toString();
			// hack: header/reference related fixes
			labels = labels.replaceAll( "author-begin", "authors:^author" );
			labels = labels.replaceAll( "author-inside", "authors:author" );
			String[] labelParts = labels.split( "[:|]" );

            //kzaporojets: gets the columns and its coordinates in terms of the width
            if(parentName.equals("reference")) {
                currentColumn = getCurrentColumn(columns,span);
            }

            //kzaporojets: insertTokenPosition also includes the position
			//insertToken( rootElement, getSpanText( span ), labelParts );
            insertTokenPosition(rootElement, getSpanText( span ), labelParts, getSpanBoxCoordinates(span),currentColumn);
		}
		return rootElement;
	}


    int getCurrentColumn(List<BoxCoordinates> columns, Span span)
    {
        double llxSpan = ((PropertyHolder) span).getNumericProperty( "llx" );
        double urxSpan = ((PropertyHolder) span).getNumericProperty( "urx" );
        int col=1;
        for(BoxCoordinates bc : columns)
        {
            if(bc.getLlx()<=llxSpan && urxSpan<=bc.getUrx())
            {
                return col;
            }
            col++;
        }
        return 1;
    }
    List<BoxCoordinates> getColumnData(List lineSpan)
    {
        List<BoxCoordinates> retVal = new ArrayList<BoxCoordinates>();
        int currCol=0;
        for (Object span:lineSpan)
        {
            Double llx = Double.valueOf(((CompositeSpan)span).getProperty("llx").toString());
            Double urx = Double.valueOf(((CompositeSpan)span).getProperty("urx").toString());
            if(retVal.size()<=currCol)
            {
                //double ury, double urx, double lly, double llx, int pageNum
                retVal.add(new BoxCoordinates(-1,urx,-1, llx, -1));
            }
            else
            {
                BoxCoordinates bc = retVal.get(currCol);
                if(bc.getLlx()<llx)
                {
                    currCol ++ ;
                    retVal.add(new BoxCoordinates(-1,urx,-1, llx, -1));
                }
                else
                {
                    if(bc.getLlx()>llx){
                        bc.setLlx(llx);
                    }
                    if(bc.getUrx()<urx)
                    {
                        bc.setUrx(urx);
                    }
                }

            }
        }
        return retVal;
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
    private void insertTokenPosition(Element parent, String span, String[] labelParts, BoxCoordinates positionSpan, int currentColumn) {
        //associate position here
        adjustPosition(parent, positionSpan, currentColumn);
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
            insertTokenPosition(child, span, labelTail, positionSpan, currentColumn);
        }
        else {
            parent.addContent( span );
        }
    }

    //adjust the position
    private void adjustPosition(Element elem, BoxCoordinates pos, int currentColumn)
    {
        try {
            //if suddenly all the attributes change abruptly, and we are in references section, don't change them:
            //probably its just a new column. Won't deel with it for now.
            String llxAttr = currentColumn==1?"llx":elem.getAttribute("llx")!=null?"llxc" + currentColumn:"llx";
            String llyAttr = currentColumn==1?"lly":elem.getAttribute("lly")!=null?"llyc" + currentColumn:"lly";
            String urxAttr = currentColumn==1?"urx":elem.getAttribute("urx")!=null?"urxc" + currentColumn:"urx";
            String uryAttr = currentColumn==1?"ury":elem.getAttribute("ury")!=null?"uryc" + currentColumn:"ury";

//            if (elem.getAttribute(llxAttr)!=null && elem.getName().equals("reference"))
//            {
//                if(Math.abs(elem.getAttribute(llyAttr).getDoubleValue() - pos.getLly())>400 &&
//                        Math.abs(elem.getAttribute(uryAttr).getDoubleValue() - pos.getUry())>400)
//                {
//                    return;
//                }
//            }
            if (elem.getAttribute(llxAttr) == null || elem.getAttribute(llxAttr).getDoubleValue() > pos.getLlx()) {
                elem.setAttribute(llxAttr, String.valueOf(pos.getLlx()));

            }
            if (elem.getAttribute(llyAttr) == null || elem.getAttribute(llyAttr).getDoubleValue() > pos.getLly()) {
                elem.setAttribute(llyAttr, String.valueOf(pos.getLly()));
            }
            if (elem.getAttribute(urxAttr) == null || elem.getAttribute(urxAttr).getDoubleValue() < pos.getUrx()) {
                elem.setAttribute(urxAttr, String.valueOf(pos.getUrx()));
            }
            if (elem.getAttribute(uryAttr) == null || elem.getAttribute(uryAttr).getDoubleValue() < pos.getUry()) {
                elem.setAttribute(uryAttr, String.valueOf(pos.getUry()));
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
