package org.rexo.pipeline.extractors;

import edu.umass.cs.mallet.base.fst.Transducer;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;

import java.util.Iterator;

/**
 * Created by klimzaporojets on 8/4/14.
 */
public class BodyRulesTransducer  {


    public Sequence transduce(Sequence data)
    {
        Sequence transducedData = new TokenSequence();
        String label;
        for(int i=0; i<((TokenSequence)data).size(); i++)
        {
//            Token tkn = (Token)(((TokenSequence)data).get(i));
//            if (tkn.getText().toUpperCase().trim().equals("REFERENCES"))
//            {
//                label = "biblioPrologue";
//            }
//            else if(tkn.getFeatures().hasProperty("possibleInit")
//            {
//                label = "biblio-B";
//            }
//            else if(tkn.getFeatures().hasProperty("ignore"))
//            {
//                label = "junk";
//            }
//            else
//            {
//                label = "biblio-I";
//            }
//
//            ((TokenSequence)transducedData).add(label);
            System.out.println("inside loop");
        }

        return transducedData;
    }

}
