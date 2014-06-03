package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.File;
import java.io.IOException;

import org.rexo.extraction.IEEvaluator;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.Target2LabelSequence;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureVectorSequence;
import edu.umass.cs.mallet.base.types.InstanceList;


public class ReferenceSegmentorTrainerUsingAlexFeaturesTUI 
{
	public static void main(String[] args) throws IOException 
	{
		if (args.length != 2) {
			System.out.println("Usage: ClassifierSegmentor trainingData.features.verbose testingData.features.verbose");
			System.exit(1);
		}

		File trainFile = new File(args[0]);
		File testFile = new File(args[1]);

		SerialPipes pipes = new SerialPipes(new Pipe[] {
				new StringArray2TokenSequence(),
				new TokenSequence2FeatureVectorSequence (true, false),
				new Target2LabelSequence(),
		});
		
		InstanceList trainList = new InstanceList(pipes);
		InstanceList testList = new InstanceList(pipes);

		trainList.add(new FeatureLineListIterator(trainFile));
		testList.add(new FeatureLineListIterator(testFile));

		System.out.println("Num training insts=" + trainList.size());
		System.out.println("Num test insts=" + testList.size());

		File outputPrefix = new File("/m/dalisvr/data1/ghuang/rexa-seg/logs/");
		int skipNum = 30;

		IEEvaluator eval = new IEEvaluator (outputPrefix, "viterbi", skipNum, 1);
		eval.setNumIterationsToSkip (0);
		eval.setNumIterationsToWait (1);

		CRF4 crf = new CRF4 (pipes, null);
		crf.setUseSparseWeights (true);
		crf.addOrderNStates (trainList, new int[] { 0, 1 }, null, "<START>", null, null, true);

		// Some supported features
		crf.train (trainList, null, testList, eval, 10);

		// Why should I be doing this? Reusing the evaluator doesn't work...
		eval = new IEEvaluator (outputPrefix, "viterbi", skipNum, 1); 
		eval.setNumIterationsToSkip (0);
		eval.setNumIterationsToWait (1);

		crf.train (trainList, null, testList, eval, 99999); 
	}

}
