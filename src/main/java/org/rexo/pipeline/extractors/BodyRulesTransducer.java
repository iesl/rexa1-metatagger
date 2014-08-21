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

import java.util.*;

/**
 * Created by klimzaporojets on 8/4/14.
 */
public class BodyRulesTransducer  {

    //class representing the properties of a particular section marker
    class SectionMarker
    {
        private List features;
    }


    public Sequence transduce(NewHtmlTokenization data)
    {


        Sequence transducedData = new TokenSequence();
        boolean currentlyInColumn = false;
        int tokenId = 0;

        boolean previousSectionMarker = false;
        String figureOrTableMarker = "";
        String footerMarker = "";

        List<String> featuresTableContent = new ArrayList<String>();
        featuresTableContent.add("pixelsPerCharacter2pxGreater");
        featuresTableContent.add("pixelsPerCharacterUndefined");
        featuresTableContent.add("noWordsFromDictionary");
        featuresTableContent.add("3wordFromDictLess");

        List<String> relaxedFeaturesTableContent = new ArrayList<String>();
        relaxedFeaturesTableContent.add("pixelsPerCharacter2pxGreater");
        relaxedFeaturesTableContent.add("noWordsFromDictionary");
        relaxedFeaturesTableContent.add("oneWordFromDictionary");


        boolean previousFigure;
        boolean debugMe  = false;

        Map<String, Integer> lastLabelIndexes = new HashMap<String, Integer>();

        for(int i=0; i<data.getLineSpans().size(); i++)
        {
            previousFigure = false;
            String label = "";
            Span previousSpan = i>0?(Span)data.getLineSpans().get(i-1):null;
            Span currentSpan = (Span)data.getLineSpans().get(i);
            if(currentSpan==null)
            {
                continue;
            }
            Span nextSpan = i<data.getLineSpans().size()-1?(Span)data.getLineSpans().get(i+1):null;
          //  boolean isNoCollumnAssociated = LayoutUtils.isActiveFeature(currentSpan,"noColumnAssociated");
            if(!debugMe)
            {
                debugMe = currentSpan instanceof CompositeSpan && ((Double)((CompositeSpan) currentSpan).getProperty("pageNum")) == 1.0; // && currentSpan.getText().contains("Acknowledgments") ;
            }

            if((((LayoutUtils.isActiveFeature(currentSpan, "firstLevelSectionPtrn") || LayoutUtils.isActiveFeature(currentSpan, "secondLevelSectionPtrn") ||
                    LayoutUtils.isActiveFeature(currentSpan, "thirdLevelSectionPtrn")
                    ||
                    LayoutUtils.isActiveFeature(currentSpan, "allCaps") /*in some papers the titles are in caps*/
                    ||
                    (LayoutUtils.isActiveFeature(currentSpan, "centeredLine"))
                    ||
                    ((LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft") && LayoutUtils.isActiveFeature(currentSpan, "startsCap") &&
                           !LayoutUtils.isActiveFeature(currentSpan, "tabbedLeftMargin") && !LayoutUtils.isActiveFeature(currentSpan, "endsInDotAndNumber")) ||
                            (previousSectionMarker && LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft")))

                    )
                    && (!LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated")
                            ||

                            columnInFutureWithTitles(i, data, 3)
                        ) &&
                    ( ((LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater") && LayoutUtils.isActiveFeature(currentSpan, "verticalDistanceUry2pxGreater")) || /* with "verticalDistance4pxGreater" doesn't work on INTRODUCTION section
                                    of 2014W%F6hlertSynthesis,_Structures paper*/
                            (previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance4pxGreater")) ||
                            (LayoutUtils.isActiveFeature(currentSpan, "lineHeight2pxGreater") && LayoutUtils.isActiveFeature(currentSpan, "verticalDistanceUry2pxGreater") && LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater")) ||

                            (LayoutUtils.isActiveFeature(currentSpan, "lineHeight2pxGreater") && (LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft"))
                    ))
                            /*first section marker always distance above*/
                            && (previousSectionMarker || LayoutUtils.isActiveFeature(currentSpan, "newColumn") ||
                                LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated") ||
                                (previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistanceUry2pxGreater") && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater")))
                            /*end first marker always distance above*/
                            /*the width never wider than the columns*/
                            && (!LayoutUtils.isActiveFeature(currentSpan, "lineWidth10pxGreater"))
                            /*end width never wider than the columns*/
                    ) &&
                    (!LayoutUtils.isActiveFeature(currentSpan, "endsInDot") /*&& in papers such as  2010Song_REcent_p... it makes the section markers to be ignored
                                    LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft")*/)

                    )
                    /*sometimes doesn't work because pstotext ommits sentences*/
                    /* (previousSpan == null || (LayoutUtils.isActiveFeature(previousSpan, "endsInDot") && !previousSectionMarker) || previousSectionMarker )*/
                    ||
                    (previousSectionMarker && LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater") &&
                            LayoutUtils.isActiveFeature(currentSpan, "verticalDistanceUry2pxGreater")) /* in case the section marker has several lines*/


            )   //any section has to have alphabetic characters and have words in dictionary (this latter changed by word forms), no words in dict
                    //in papers such as 2010Song...
                && (!LayoutUtils.isActiveFeature(currentSpan, "noAlphabetic") && LayoutUtils.isActiveFeature(currentSpan, "1wordFormOrGreater") && !LayoutUtils.isActiveFeature(currentSpan, "endsInDot")))
            {
                if ((LayoutUtils.isActiveFeature(currentSpan, "firstLevelSectionPtrn") || LayoutUtils.isActiveFeature(currentSpan, "secondLevelSectionPtrn") ||
                        LayoutUtils.isActiveFeature(currentSpan, "thirdLevelSectionPtrn"))
                     ||
                        (previousSectionMarker && LayoutUtils.isActiveFeature(previousSpan, "rightMarginToTheLeft") &&
                                LayoutUtils.isActiveFeature(previousSpan, "verticalDistanceUry2pxGreater") && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater"))
                        )
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
                            LayoutUtils.isActiveFeature(previousSpan, "noColumnAssociated") ||
                            LayoutUtils.isActiveFeature(currentSpan, "columnLayoutChange") ||
                            LayoutUtils.isActiveFeature(currentSpan, "lineHeight30pxGreater") ||
                            LayoutUtils.isActiveFeature(currentSpan, "up")
                    ))
            {
                label = "notext";
            }
            else if(label.equals(""))
            {
                label = "text-inside";
            }

            if(!label.contains("section-marker") && LayoutUtils.isActiveFeature(currentSpan,"columnLayoutChange") /*&& !(previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan,"columnLayoutChange"))*/)
            {
                label = "text-begin";
            }


            if((figureOrTableMarker.contains("table-marker") && ( (LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater") &&
                                     LayoutUtils.isActiveFeature(previousSpan, "verticalDistanceUry2pxGreater") &&
                                    !LayoutUtils.isActiveFeature(currentSpan, "pixelsPerCharacter2pxGreater") &&
                                    !LayoutUtils.isActiveFeature(currentSpan, "pixelsPerCharacterUndefined") &&
                                    !LayoutUtils.isActiveFeature(currentSpan, "noWordsFromDictionary") &&
                                    !LayoutUtils.isActiveFeature(currentSpan, "3wordFromDictLess") &&
                                    !(LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft") && !LayoutUtils.isActiveFeature(currentSpan, "endsInDot"))
            ) ||

/*                                LayoutUtils.isActiveFeature(currentSpan, "up") ||*/
                                  LayoutUtils.isActiveFeature(currentSpan, "up20PxGreater") ||
                                    //todo: test well on other documents, commented because in 1999Fey_Synthesis... didn't work well
//                                  LayoutUtils.isActiveFeature(currentSpan, "lineHeight10pxGreater") ||

                                /*LayoutUtils.isActiveFeature(currentSpan, "right") ||*/
                                LayoutUtils.isActiveFeature(currentSpan, "newPage") )))
            {
                figureOrTableMarker = "";
            }


            if((figureOrTableMarker.contains("figure-marker") && ( (LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater") &&
                    LayoutUtils.isActiveFeature(previousSpan, "verticalDistanceUry2pxGreater") &&
                    !(LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft") && !LayoutUtils.isActiveFeature(currentSpan, "endsInDot"))
            ) ||

/*                                LayoutUtils.isActiveFeature(currentSpan, "up") ||*/
                    LayoutUtils.isActiveFeature(currentSpan, "up20PxGreater") ||
                    //todo: test well on other documents
                    LayoutUtils.isActiveFeature(currentSpan, "lineHeight10pxGreater") ||

                                /*LayoutUtils.isActiveFeature(currentSpan, "right") ||*/
                    LayoutUtils.isActiveFeature(currentSpan, "newPage") )))
            {
                figureOrTableMarker = "";
                previousFigure=true;
            }


            if(!figureOrTableMarker.equals("") &&
                    (!LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater") ||
                            !LayoutUtils.isActiveFeature(previousSpan, "verticalDistanceUry2pxGreater") ||
                            (LayoutUtils.isActiveFeature(currentSpan, "pixelsPerCharacter2pxGreater")  && figureOrTableMarker.contains("table-marker")) ||
                            (LayoutUtils.isActiveFeature(currentSpan, "pixelsPerCharacterUndefined")  && figureOrTableMarker.contains("table-marker")) ||
                            (LayoutUtils.isActiveFeature(currentSpan, "noWordsFromDictionary")  && figureOrTableMarker.contains("table-marker")) ||
                            (LayoutUtils.isActiveFeature(currentSpan, "3wordFromDictLess") && figureOrTableMarker.contains("table-marker") ) ||
                            (LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft") && !LayoutUtils.isActiveFeature(currentSpan, "endsInDot"))
                    ))
            {
                label = figureOrTableMarker;
            }
            boolean futureLayout = LayoutUtils.isAnyOfFeaturesInFuture(data, i, relaxedFeaturesTableContent, 3, 10);
            if(LayoutUtils.isActiveFeature(currentSpan, "startsTableWord") && (LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft") //

             ||
                    (LayoutUtils.isActiveFeature(currentSpan, "up") && futureLayout) ||
                    (LayoutUtils.isActiveFeature(currentSpan, "up")
                                && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
             ||
                    (LayoutUtils.isActiveFeature(currentSpan, "right") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
             ||
                    (previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance100pxGreater") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop") && !LayoutUtils.isActiveFeature(currentSpan, "newPage"))
             ||     (previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance2pxGreater") && futureLayout && !LayoutUtils.isActiveFeature(currentSpan, "newPage"))
            ))
            {
                label = "table-marker-begin";
                figureOrTableMarker = "table-marker-inside";
            }


            boolean futureFigureLayout = LayoutUtils.isFigureInTheFuture(data, i,
                    LayoutUtils.isActiveFeature(currentSpan,"lineWidth20pxLess")?15:6);

            if(LayoutUtils.isActiveFeature(currentSpan, "startsFigureWord") && (LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft")
                    //todo: work on it
                     ||
                    (LayoutUtils.isActiveFeature(currentSpan, "up") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
                    ||
                    (LayoutUtils.isActiveFeature(currentSpan, "right") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
                    ||
                    (previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance100pxGreater") && !LayoutUtils.isActiveFeature(currentSpan, "nearThe150PxOfTop"))
                    ||
                    futureFigureLayout
                    || (previousFigure && LayoutUtils.isActiveFeature(currentSpan,"lineWidth20pxLess") && previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance12pxGreater"))

                    ))
            {
                label = "figure-marker-begin";
                figureOrTableMarker = "figure-marker-inside";
            }

            //for footers
            if(footerMarker.equals("footer-start") && !LayoutUtils.isActiveFeature(currentSpan,"up"))
            {
                label = "notext";
            }
            else if (footerMarker.equals("footer-start") && LayoutUtils.isActiveFeature(currentSpan,"up"))
            {
                footerMarker="";
            }
            else if(previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistanceUry4pxGreater") &&
                    LayoutUtils.isActiveFeature(currentSpan, "startsEnum") && LayoutUtils.isActiveFeature(currentSpan, "nearThe100PxOfBottom") &&
                    LayoutUtils.isActiveFeature(currentSpan, "lineHeight1pxLess"))
            {
                footerMarker = "footer-start";
                label = "notext";
            }


            //for paragraphs
            if(label.equals("text-inside") || label.equals("text-begin"))
            {
                //todo: the last line of text

                int lastIndex = getLastIndex(lastLabelIndexes, new String[]
                                                {"text-begin","text-inside", "paragraph-begin", "paragraph-inside"});
                Span lastTextLineRead = null;
                if(lastIndex>-1)
                {
                    lastTextLineRead = (Span)data.getLineSpans().get(lastIndex);
                }


                if((LayoutUtils.isActiveFeature(currentSpan, "tabbedLeftMargin") &&
                                    LayoutUtils.isActiveFeature(currentSpan, "startsCap")) ||
                        LayoutUtils.isActiveFeature(currentSpan, "newColumn") ||
                        (lastTextLineRead!=null
                                && LayoutUtils.isActiveFeature(lastTextLineRead, "rightMarginToTheLeft")
                                && LayoutUtils.isActiveFeature(lastTextLineRead, "endsInDot")))
                {
                    label = "paragraph-begin";
                }
                else
                {
                    label = "paragraph-inside";
                }
            }

            //end for paragraphs

            lastLabelIndexes.put(label, i);

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

    private int getLastIndex(Map<String, Integer> indexes, String [] keys)
    {
        int lastIndex = -1;
        for(String key: keys)
        {
            Integer kValue = indexes.get(key);
            if(kValue!=null && kValue>lastIndex)
            {
                lastIndex = kValue;
            }
        }
        return lastIndex;
    }
    private boolean columnInFutureWithTitles(int i, NewHtmlTokenization data, int linesInFuture)
    {
                            /*(LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated") &&
                            nextSpan != null && !LayoutUtils.isActiveFeature(nextSpan, "newColumn") &&
                                !LayoutUtils.isActiveFeature(nextSpan, "newPage") &&
                                !LayoutUtils.isActiveFeature(nextSpan, "noColumnAssociated"))*/
        for(int cnt = 0; cnt< linesInFuture && cnt+i<data.getLineSpans().size(); cnt++)
        {
            Span currentSpan = (Span)data.getLineSpans().get(cnt+i);
            Span nextSpan = null;
            if(cnt+i+1<data.getLineSpans().size())
            {
                nextSpan = (Span)data.getLineSpans().get(cnt+i+1);
            }
            Span previousSpan = null;
            if (cnt>0)
            {
                previousSpan = (Span)data.getLineSpans().get(cnt + i - 1);
            }
            if(cnt>0 && (LayoutUtils.isActiveFeature(currentSpan, "newColumn") || LayoutUtils.isActiveFeature(currentSpan, "newPage")))
            {
                return false;
            }
            else if(LayoutUtils.isActiveFeature(currentSpan, "columnLayoutChange") /*|| LayoutUtils.isActiveFeature(currentSpan, "sloppyStrictLeft")*/)
            {
                return true;
            }
            else if ((((LayoutUtils.isActiveFeature(currentSpan, "firstLevelSectionPtrn") || LayoutUtils.isActiveFeature(currentSpan, "secondLevelSectionPtrn") ||
                    LayoutUtils.isActiveFeature(currentSpan, "thirdLevelSectionPtrn")
                    ||
                    LayoutUtils.isActiveFeature(currentSpan, "allCaps") /*in some papers the titles are in caps*/
                    ||
                    (LayoutUtils.isActiveFeature(currentSpan, "centeredLine"))
                    ||
                    (LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft"))

            )
//                    && (!LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated"))
                    &&
                    ( (LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater") && LayoutUtils.isActiveFeature(currentSpan, "verticalDistanceUry2pxGreater")) || /* with "verticalDistance4pxGreater" doesn't work on INTRODUCTION section
                                    of 2014W%F6hlertSynthesis,_Structures paper*/
                            (previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance4pxGreater")) ||
                            (LayoutUtils.isActiveFeature(currentSpan, "lineHeight2pxGreater") && LayoutUtils.isActiveFeature(currentSpan, "verticalDistanceUry2pxGreater") && LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater"))
                    ) &&
                    (!LayoutUtils.isActiveFeature(currentSpan, "endsInDot") )

            )
                    /*sometimes doesn't work because pstotext ommits sentences*/
                    /* (previousSpan == null || (LayoutUtils.isActiveFeature(previousSpan, "endsInDot") && !previousSectionMarker) || previousSectionMarker )*/
//                    ||
//                    (previousSectionMarker && LayoutUtils.isActiveFeature(currentSpan, "verticalDistance2pxGreater") &&
//                            LayoutUtils.isActiveFeature(currentSpan, "verticalDistanceUry2pxGreater")) /* in case the section marker has several lines*/


            )   //any section has to have alphabetic characters
                    && (!LayoutUtils.isActiveFeature(currentSpan, "noAlphabetic")))
            {
                continue;
            }
            return false;
        }
        return false;
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
            if(label.equals("figure-marker-begin"))
            {
                label = "figure-marker-inside";
            }
            if(label.equals("table-marker-begin"))
            {
                label = "table-marker-inside";
            }
            if(label.equals("paragraph-begin"))
            {
                label = "paragraph-inside";
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
