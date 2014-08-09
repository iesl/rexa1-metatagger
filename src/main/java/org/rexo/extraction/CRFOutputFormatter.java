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

        int page = -1;
		for (int i = 0; i < input.size(); i++) {

			Span span = (Span)input.get( i );
			String labels = tokenLabels.get( i ).toString();
			// hack: header/reference related fixes
			labels = labels.replaceAll( "author-begin", "authors:^author" );
			labels = labels.replaceAll( "author-inside", "authors:author" );
            //for body
            labels = labels.replaceAll( "text-begin", "^text" );
            labels = labels.replaceAll( "text-inside", "text" );

			String[] labelParts = labels.split( "[:|]" );
            BoxCoordinates bcord = getSpanBoxCoordinates(span);
            //kzaporojets: gets the columns and its coordinates in terms of the width
            if(parentName.equals("reference")) {
                currentColumn = getCurrentColumn(columns,span);
                //all in the same page
//                if(page!=-1 && bcord.getPageNum()!=page)
//                {
//                    bcord.setPageNum(page);
//                }
            }
//            page = bcord.getPageNum();

            //kzaporojets: insertTokenPosition also includes the position
			//insertToken( rootElement, getSpanText( span ), labelParts );
            insertTokenPosition(rootElement, getSpanText( span ), labelParts, bcord,currentColumn);
		}
		return rootElement;
	}


    int getCurrentColumn(List<BoxCoordinates> columns, Span span)
    {
        double llxSpan = ((PropertyHolder) span).getNumericProperty( "llx" );
        double urxSpan = ((PropertyHolder) span).getNumericProperty( "urx" );
        int col=1;
        int estimateCol = 1;
        double estimateDistance = 10000;
        for(BoxCoordinates bc : columns)
        {
            if(bc.getLlx()<=llxSpan && urxSpan<=bc.getUrx())
            {
                return col;
            }
            else
            {
                if(bc.getLlx()<=llxSpan&&bc.getUrx()>=llxSpan)
                {
//                    estimateDistance=0;
                    estimateCol = col;
                }
                else if (bc.getLlx()<=urxSpan&&bc.getUrx()>=urxSpan)
                {
//                    estimateDistance=0;
                    estimateCol = col;
                }
                else if(bc.getLlx()>=llxSpan&&bc.getUrx()<=urxSpan)
                {
                    estimateCol = col;
                }
                else if(bc.getUrx()<=urxSpan&&bc.getUrx()<=llxSpan)
                {
                    estimateCol = col;
                }
            }
            col++;
        }
        return estimateCol;
    }
    List<BoxCoordinates> getColumnData(List lineSpan)
    {
        List<BoxCoordinates> retVal = new ArrayList<BoxCoordinates>();
        int currCol=0;
        for (Object span:lineSpan)
        {
            if(span instanceof CompositeSpan) {
                Double llx = Double.valueOf(((CompositeSpan)span).getProperty("llx").toString());
                Double urx = Double.valueOf(((CompositeSpan)span).getProperty("urx").toString());
                if (retVal.size() <= currCol) {
                    //double ury, double urx, double lly, double llx, int pageNum
                    retVal.add(new BoxCoordinates(-1, urx, -1, llx, -1));
                } else {
                    BoxCoordinates bc = retVal.get(currCol);
                    if (bc.getUrx() < llx) {
                        currCol = retVal.size();
                        retVal.add(new BoxCoordinates(-1, urx, -1, llx, -1));
                    } else if (urx < bc.getLlx()) {
                        //add in the beginning of the list
                        retVal.add(0, new BoxCoordinates(-1, urx, -1, llx, -1));
                    } else {
                        if (bc.getLlx() > llx) {
                            bc.setLlx(llx);
                        }
                        if (bc.getUrx() < urx) {
                            bc.setUrx(urx);
                        }
                    }
                }
            }
        }

        if(retVal.size()>1) {
            BoxCoordinates prevBc = retVal.get(0);
            for (BoxCoordinates bc : retVal.subList(1,retVal.size())) {
                if(bc.getLlx()<prevBc.getUrx())
                {
                    bc.setLlx(prevBc.getUrx()+1);
                }
                prevBc = bc;
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
            int initCol = 1;
            if(elem.getAttribute("initialCol")!=null)
            {
                initCol = elem.getAttribute("initialCol").getIntValue();
            }

            String llxAttr = currentColumn==initCol?"llx":elem.getAttribute("llx")!=null?"llxc" + (Math.abs(currentColumn - initCol)+1):"llx";
            String llyAttr = currentColumn==initCol?"lly":elem.getAttribute("lly")!=null?"llyc" + (Math.abs(currentColumn - initCol)+1):"lly";
            String urxAttr = currentColumn==initCol?"urx":elem.getAttribute("urx")!=null?"urxc" + (Math.abs(currentColumn - initCol)+1):"urx";
            String uryAttr = currentColumn==initCol?"ury":elem.getAttribute("ury")!=null?"uryc" + (Math.abs(currentColumn - initCol)+1):"ury";




            if (elem.getAttribute(llxAttr)!=null && elem.getName().equals("reference"))
            {
                if(Math.abs(elem.getAttribute(llyAttr).getDoubleValue() - pos.getLly())>400 &&
                        Math.abs(elem.getAttribute(uryAttr).getDoubleValue() - pos.getUry())>400)
                {
                    return;
                }
            }
            //inserts the number of column with respect to which the llxc_ is calculated
            if(llxAttr.equals("llx") && elem.getAttribute("initialCol")==null)
            {
                elem.setAttribute("initialCol",String.valueOf(currentColumn));
            }

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
