package org.rexo.pipeline.extractors;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.fst.Transducer;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.rexo.ghuang.segmentation.utils.LayoutUtils;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.span.CompositeSpan;

import java.util.Iterator;

/**
 * Created by klimzaporojets on 8/4/14.
 */
public class BodyRulesTransducer  {


    public Sequence transduce(NewHtmlTokenization data)
    {


        Sequence transducedData = new TokenSequence();
        boolean currentlyInColumn = false;
        int tokenId = 0;

        boolean previousSectionMarker = false;
        String figureOrTableMarker = "";
        boolean debugMe  = false;
        for(int i=0; i<data.getLineSpans().size(); i++)
        {
            String label = "";
            Span previousSpan = i>0?(Span)data.getLineSpans().get(i-1):null;
            Span currentSpan = (Span)data.getLineSpans().get(i);
            Span nextSpan = i<data.getLineSpans().size()-1?(Span)data.getLineSpans().get(i+1):null;
          //  boolean isNoCollumnAssociated = LayoutUtils.isActiveFeature(currentSpan,"noColumnAssociated");
            if(!debugMe)
            {
                debugMe = currentSpan instanceof CompositeSpan && ((Double)((CompositeSpan) currentSpan).getProperty("pageNum")) == 3.0;
            }

            if(((LayoutUtils.isActiveFeature(currentSpan, "firstLevelSectionPtrn") || LayoutUtils.isActiveFeature(currentSpan, "secondLevelSectionPtrn") ||
                    LayoutUtils.isActiveFeature(currentSpan, "thirdLevelSectionPtrn"))
                    && (!LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated")
                            || (LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated") &&
                            nextSpan != null && !LayoutUtils.isActiveFeature(nextSpan, "newColumn") &&
                                !LayoutUtils.isActiveFeature(nextSpan, "newPage") &&
                                !LayoutUtils.isActiveFeature(nextSpan, "noColumnAssociated"))
                        ) &&
                    (LayoutUtils.isActiveFeature(currentSpan, "verticalDistance4pxGreater") ||
                            (previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance4pxGreater"))) &&

                    (!LayoutUtils.isActiveFeature(currentSpan, "endsInDot") /*&& in papers such as  2010Song_REcent_p... it makes the section markers to be ignored
                                    LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft")*/))
                    //sometimes doesn't work because pstotext ommits sentences
                    // (previousSpan == null || (LayoutUtils.isActiveFeature(previousSpan, "endsInDot") && !previousSectionMarker) || previousSectionMarker )
                    || // in case the section marker has several lines
                    (previousSectionMarker && LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater"))
            )
            {
                if (LayoutUtils.isActiveFeature(currentSpan, "firstLevelSectionPtrn") || LayoutUtils.isActiveFeature(currentSpan, "secondLevelSectionPtrn") ||
                        LayoutUtils.isActiveFeature(currentSpan, "thirdLevelSectionPtrn"))
                {
                    label = "section-marker-begin";
                }
                else
                {
                    label = "section-marker-inside";
                }
            }

            if(label.equals("") && LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated") &&
                    (LayoutUtils.isActiveFeature(currentSpan, "newPage") ||
                            LayoutUtils.isActiveFeature(currentSpan, "newColumn") ||
                            LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft")
                                        || (previousSpan==null) ||
                            LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater") ||
                            LayoutUtils.isActiveFeature(previousSpan, "noColumnAssociated") ||  //&&
                            LayoutUtils.isActiveFeature(currentSpan, "columnLayoutChange") ||
                            LayoutUtils.isActiveFeature(currentSpan, "lineHeight30pxGreater")
                    ))
            {
                label = "notext";
            }
            else if(label.equals(""))
            {
                label = "text-inside";
            }

            if(LayoutUtils.isActiveFeature(currentSpan,"columnLayoutChange") /*&& !(previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan,"columnLayoutChange"))*/)
            {
                label = "text-begin";
            }


            if((!figureOrTableMarker.equals("") && (LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater") ||
                    LayoutUtils.isActiveFeature(currentSpan, "up") || LayoutUtils.isActiveFeature(currentSpan, "newPage") )))
            {
                figureOrTableMarker = "";
            }

            if(LayoutUtils.isActiveFeature(currentSpan, "startsTableWord") && (LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft") //

             ||
                    (LayoutUtils.isActiveFeature(currentSpan, "up") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
            ))
            {
                label = "table-marker";
                figureOrTableMarker = "table-marker";
            }

            if(LayoutUtils.isActiveFeature(currentSpan, "startsFigureWord") && (LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft")
                    //todo: work on it
                     ||
                    (LayoutUtils.isActiveFeature(currentSpan, "up") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
                    //(previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance12pxGreater"))
                    ))
            {
                label = "figure-marker";
                figureOrTableMarker = "figure-marker";
            }

            if(!figureOrTableMarker.equals("") && !LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater"))
            {
                label = figureOrTableMarker;
            }


            tokenId = addLabelToAllSpans(currentSpan, label, (TokenSequence)transducedData, data,tokenId);

            if(label.equals("section-marker-begin") || label.equals("section-marker-inside"))
            {
                previousSectionMarker = true;
            }
            else
            {
                previousSectionMarker = false;
            }
        }

        return transducedData;
    }


    public int addLabelToAllSpans(Span span, String label, TokenSequence transducedData, NewHtmlTokenization data,
                                   int tokenId)
    {

        span.getEndIdx();
        Span tok = null;
//        Token tkn = data.getToken(1);
//here add to check "column-inside"
        while(tokenId< data.size() &&  (tok = data.getSpan(tokenId) ) .getEndIdx()<=span.getEndIdx() )
        {
            transducedData.add(label);
            ++tokenId;
            if(label.equals("text-begin"))
            {
                label = "text-inside";
            }
            if(label.equals("section-marker-begin"))
            {
                label = "section-marker-inside";
            }
        }
        return tokenId;

//        if(span instanceof CompositeSpan)
//        {
//            for(int i =0; i<((CompositeSpan)span).getSpans().size(); i++)
//            {
//
//                transducedData.add(label);
//            }
//        }
//        else
//        {
//            transducedData.add(label);
//        }
//        return lastTokenId;
    }
}
