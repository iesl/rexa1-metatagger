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

/*<body>

  <section>
    <section-marker> <text page="3" pos="x:30,y:304">2 Experimental </text> </section-.>

    <section>
      <section-marker> 2.3. Electrochemical measurement </section-marker>
      <section-text>
        <text pos...>... for electrochemical measurements, the Fe2P nanoparticle </text><text pos="">>elec-trodes were constructed through mixing the products withcarbonyl nickel powders in a weight ratio of 1:3. The powdermixtures were pressed under 30 MPa pressure into a small pellet of10 mm in diameter. Then, the electrodes were conducted in a threecompartment  cell  using  an  LAND  battery  test  instrument(CT2001A). NiOOH/Ni(OH)2and Hg/HgO were used as the counterelectrode and the referenc
      </section-text>
    </section>

  </section>

</body>*/


        Sequence transducedData = new TokenSequence();
        boolean currentlyInColumn = false;
        int tokenId = 0;

        boolean previousSectionMarker = false;
        for(int i=0; i<data.getLineSpans().size(); i++)
        {
            String label = "";
            Span previousSpan = i>0?(Span)data.getLineSpans().get(i-1):null;
            Span currentSpan = (Span)data.getLineSpans().get(i);
            Span nextSpan = i<data.getLineSpans().size()-1?(Span)data.getLineSpans().get(i+1):null;
          //  boolean isNoCollumnAssociated = LayoutUtils.isActiveFeature(currentSpan,"noColumnAssociated");

            if((LayoutUtils.isActiveFeature(currentSpan, "firstLevelSectionPtrn") || LayoutUtils.isActiveFeature(currentSpan, "secondLevelSectionPtrn") ||
                    LayoutUtils.isActiveFeature(currentSpan, "thirdLevelSectionPtrn"))
                    && (!LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated")
                            || (LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated") &&
                            nextSpan != null && !LayoutUtils.isActiveFeature(nextSpan, "newColumn") &&
                                !LayoutUtils.isActiveFeature(nextSpan, "newPage") &&
                                !LayoutUtils.isActiveFeature(nextSpan, "noColumnAssociated"))
                        ) &&
                    (LayoutUtils.isActiveFeature(currentSpan, "verticalDistance4pxGreater") ||
                            (previousSpan!=null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance4pxGreater"))) &&

                    (!LayoutUtils.isActiveFeature(currentSpan, "endsInDot") &&
                                    LayoutUtils.isActiveFeature(currentSpan, "rightMarginToTheLeft"))
                    //sometimes doesn't work because pstotext ommits sentences
                    // (previousSpan == null || (LayoutUtils.isActiveFeature(previousSpan, "endsInDot") && !previousSectionMarker) || previousSectionMarker )
                    )
            {
                label = "section-marker";
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

            if(LayoutUtils.isActiveFeature(currentSpan, "startsTableWord") && (LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft") //

            //todo: work on it
            // ||
            //        (previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance12pxGreater"))
            ))
            {
                label = "table-marker";
            }

            if(LayoutUtils.isActiveFeature(currentSpan, "startsFigureWord") && (LayoutUtils.isActiveFeature(currentSpan, "upAndToTheLeft")
                    //todo: work on it
                    // ||
                    //(previousSpan != null && LayoutUtils.isActiveFeature(previousSpan, "verticalDistance12pxGreater"))
                    ))
            {
                label = "figure-marker";
            }

            tokenId = addLabelToAllSpans(currentSpan, label, (TokenSequence)transducedData, data,tokenId);

            if(label.equals("section-marker"))
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
