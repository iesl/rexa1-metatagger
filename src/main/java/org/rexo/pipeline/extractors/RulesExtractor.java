package org.rexo.pipeline.extractors;

import edu.umass.cs.mallet.base.extract.Extraction;
import edu.umass.cs.mallet.base.extract.Extractor;
import edu.umass.cs.mallet.base.extract.Tokenization;
import edu.umass.cs.mallet.base.extract.TokenizationFilter;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.iterator.PipeInputIterator;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.LabelAlphabet;

/**
 * Created by klimzaporojets on 8/1/14.
 */
public class RulesExtractor implements Extractor {
    private Pipe featurePipe;

    public RulesExtractor (Pipe featurePipe) {
        this.featurePipe = featurePipe;
    }

    public Pipe getTokenizationPipe ()
    {
        return null;
    }
    public Extraction extract(PipeInputIterator source) {

        return null;
    }

    public Extraction extract(Object o) {
        return null;
    }

    public Pipe getFeaturePipe() {
        return featurePipe;
    }
    public Alphabet getInputAlphabet ()
    {
        return null;
    }

    public void setTokenizationPipe (Pipe tokenizationPipe)
    {

    }
    public LabelAlphabet getTargetAlphabet ()
    {
        return null;
    }
    public Extraction extract (Tokenization spans)
    {
        Instance carrier = new Instance (spans, null, null, null, featurePipe);

        System.out.println("extraction on RulesExtractor");
        return null;
    }
}