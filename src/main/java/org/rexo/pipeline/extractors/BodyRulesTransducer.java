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
//        String label = "";
        boolean currentlyInColumn = false;
        for(int i=0; i<data.getLineSpans().size(); i++)
        {
            String label = "";
            Span currentSpan = (Span)data.getLineSpans().get(i);
          //  boolean isNoCollumnAssociated = LayoutUtils.isActiveFeature(currentSpan,"noColumnAssociated");





            if(LayoutUtils.isActiveFeature(currentSpan, "noColumnAssociated"))
            {
                currentlyInColumn = false;
            }

            if(LayoutUtils.isActiveFeature(currentSpan,"columnLayoutChange"))
            {
                label = "column";
                currentlyInColumn = true;
            }

            if(currentlyInColumn)
            {
                label = "column";
            }
            else
            {
                label = "nocolumn";
            }


            //((TokenSequence)transducedData).add(label);
            addLabelToAllSpans(currentSpan, label, (TokenSequence)transducedData);
//            System.out.println("inside loop");

        }

        return transducedData;
    }


    public void addLabelToAllSpans(Span span, String label, TokenSequence transducedData)
    {
        if(span instanceof CompositeSpan)
        {
            for(int i =0; i<((CompositeSpan)span).getSpans().size(); i++)
            {
                transducedData.add(label);
            }
        }
        else
        {
            transducedData.add(label);
        }
    }
}
