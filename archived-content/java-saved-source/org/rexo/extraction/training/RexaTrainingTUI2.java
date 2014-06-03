/**
 * Created on Sep 29, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */

package org.rexo.extraction.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.rexo.extraction.CopyData2Source;
import org.rexo.extraction.FilePair2NewHtmlTokenizationIterator;
import org.rexo.extraction.IEEvaluator;
import org.rexo.extraction.NewHtmlTokenization2TokenSequence;
import org.rexo.extraction.RexaCRF;
import org.rexo.extraction.SourceAnnoListsIterator;

import edu.umass.cs.mallet.base.extract.CRFExtractor;
import edu.umass.cs.mallet.base.extract.Extraction;
import edu.umass.cs.mallet.base.extract.LatticeViewer;
import edu.umass.cs.mallet.base.extract.PerDocumentF1Evaluator;
import edu.umass.cs.mallet.base.extract.PerFieldF1Evaluator;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.fst.MEMM;
import edu.umass.cs.mallet.base.pipe.AddClassifierTokenPredictions;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.iterator.InstanceListIterator;
import edu.umass.cs.mallet.base.types.FeatureSequence;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * A TUI to train and serialize a CRFExtractor.  This version supports using 
 * the pipe AddClassifierTokenPredictions
 * <p/>
 * This version processes documents represented in XML text boxes
 * with a separate set of annotation (span) files
 */
public class RexaTrainingTUI2 
{
    private static final Logger logger = MalletLogger.getLogger (RexaTrainingTUI2.class.getName ());

    public static final int TRAIN_HEADERS = 0;
    public static final int TRAIN_REFERENCES = 1;

    private static CommandOption.File outputPrefix = new CommandOption.File
    (RexaTrainingTUI2.class, "output-prefix", null, true, null,
    		"Directory for output files..", "");

    private static CommandOption.File trainSrcListOption = new CommandOption.File
    (RexaTrainingTUI2.class, "train-src-list", null, true, null,
    		"File containing a list of source filenames for training set.", "");
    
    private static CommandOption.File testSrcListOption = new CommandOption.File
    (RexaTrainingTUI2.class, "test-src-list", null, true, null,
    		"File containing a list of source filenames for test set.", "");
    
    private static CommandOption.File trainAnnoListOption = new CommandOption.File
    (RexaTrainingTUI2.class, "train-anno-list", null, true, null,
    		"File containing a list of annotation filenames for training set.", "");
    
    private static CommandOption.File testAnnoListOption = new CommandOption.File
    (RexaTrainingTUI2.class, "test-anno-list", null, true, null,
    		"File containing a list of annotation filenames for test set.", "");
    
	private static CommandOption.File initWeightsOption = new CommandOption.File
        (RexaTrainingTUI2.class, "init-weights-from", null, true, null,
         "Optional argument.  If provided, the given Transducer object is deserialized and its weights are used to initialize the transducer to be trained.", "");
	
    private static CommandOption.Integer trainingType = new CommandOption.Integer
        (RexaTrainingTUI2.class, "training-type", null, true, TRAIN_HEADERS,
         "0 == train headers, 1 == train references", "");

    private static CommandOption.Integer modelType = new CommandOption.Integer
        (RexaTrainingTUI2.class, "model-type", null, true, 0,
         "0 ==> 1st order, 1 ==> 1st order + zero order, 2 ==> 0+1+default2, 3 ==> 0+1+2, 4 ==> 1st order weights+node weights, 5 ==> fully-connected 1st order weights+node weights, 6 ==> 1st order trajectories, 7 ==> 6+collapse authors, 8 ==> state cliques", null);

    // Feature options

    private static CommandOption.File featurePipesOption = new CommandOption.File
    (RexaTrainingTUI2.class, "feature-pipes", null, true, null,
    		"The serialized feature pipes to use to create features.", "");
    
    private static CommandOption.File tokenClassifiersOption = new CommandOption.File
    (RexaTrainingTUI2.class, "token-classifiers", null, true, null,
    		"The serialized feature TokenClassifiers for adding token predictions as features.", "");
    
    private static CommandOption.Object tokenPredsToAddOption = new CommandOption.Object
    (RexaTrainingTUI2.class, "token-predictions-to-add", null, true, new int[] { 1 },
    		"An int array specifying which of the token classifier's predictions to add as features.", "");
    
    private static CommandOption.File resourceDirectory = new CommandOption.File
        (RexaTrainingTUI2.class, "resource-directory", null, true, null,
         "Directory where lexicons, etc., located.", null);
  
    private static CommandOption.Boolean supportedOnlyOption = new CommandOption.Boolean
        (RexaTrainingTUI2.class, "supported-only", null, true, true,
         "Use only the observation/transition combos that occur in training data.", null);

    private static CommandOption.Boolean useSomeSupportedOption = new CommandOption.Boolean
        (RexaTrainingTUI2.class, "use-some-supported", null, true, true,
         "Use the some supported features trick.", null);

    private static CommandOption.Boolean useFeatureInductionOption = new CommandOption.Boolean
        (RexaTrainingTUI2.class, "use-feature-induction", null, true, false,
         "Use feature induction.", null);

    private static CommandOption.Boolean useCrfOption = new CommandOption.Boolean
        (RexaTrainingTUI2.class, "use-crf", null, true, true,
         "Use an CRF.  If false, use an MEMM.", null);

    // Beam search options
    
	private static CommandOption.Boolean beamSearchOption = new CommandOption.Boolean
        (RexaTrainingTUI2.class, "beam-search", null, true, false,
         "If true, use beam search, otherwise use forward-backward algorithm.", null);

	private static CommandOption.Double klEpsOption = new CommandOption.Double
        (RexaTrainingTUI2.class, "kl-eps", null, true, 0.5,
         "The KL epsilon used in beam search.", null);

	private static CommandOption.Integer beamWidthOption = new CommandOption.Integer
        (RexaTrainingTUI2.class, "beam-width", null, true, 10,
         "The beam width used in beam search.", null);


	public static void main (String args[])
	{
		try {
			CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
			options.add (RexaTrainingTUI2.class);
			options.process (args);
			options.logOptions (logger);
			initOutputDirectory ();
			
			System.setProperty ("java.util.logging.config.file", "config/logging.properties");
			
			// Create the trivial tokenization pipe
			Pipe tokPipe = new SerialPipes (new Pipe[]{
						new Noop(),
				});
			tokPipe.setTargetProcessing(false);
			Pipe featurePipe = null;
			CRF4 crf = null;
			AddClassifierTokenPredictions actp = null;

			if (! initWeightsOption.wasInvoked()) {				
				// Deserialize the feature pipe
				FileInputStream fis = new FileInputStream(featurePipesOption.value);
				ObjectInputStream ois = new ObjectInputStream(fis);
				featurePipe = (SerialPipes) ois.readObject();
				ois.close();
				
				// Deserialize the token classifier and add token prediction features
				if (tokenClassifiersOption.wasInvoked()) {
					int[] tokenPredsToAdd = (int []) tokenPredsToAddOption.value;
					fis = new FileInputStream(tokenClassifiersOption.value);
					ois = new ObjectInputStream(fis);
					AddClassifierTokenPredictions.TokenClassifiers tokenClassifiers = 
						(AddClassifierTokenPredictions.TokenClassifiers) ois.readObject();
					ois.close();
					
					actp = new AddClassifierTokenPredictions(tokenClassifiers, tokenPredsToAdd, true, null);
					actp.setInProduction(false);
					
					featurePipe = new SerialPipes(new Pipe[] { featurePipe, actp });
					
					System.out.println("actp alphabet size=" + actp.getDataAlphabet().size());
				}
			}
			else {
				System.out.println("Initializing weights from " + initWeightsOption.value);
				FileInputStream fis = new FileInputStream(initWeightsOption.value);
				ObjectInputStream ois = new ObjectInputStream(fis);					
				CRF4 transducer = (CRF4) ois.readObject();
				ois.close();
				
				if (useCrfOption.value)
					crf = new RexaCRF (transducer);
				else
					crf = new MEMM (transducer);
				
				featurePipe = crf.getInputPipe();
			}

			System.out.println("feature pipe alphabet growth stopped? " + featurePipe.getDataAlphabet().growthStopped());
			
			// Add all (annotation, document) pairs
			Pipe noop = new Noop ();
			noop.setTargetProcessing (false);
			
			InstanceList trainNamePairs = new InstanceList (noop);
			InstanceList testNamePairs = new InstanceList (noop);
			
			trainNamePairs.add(new SourceAnnoListsIterator(trainSrcListOption.value, trainAnnoListOption.value));
			testNamePairs.add(new SourceAnnoListsIterator(testSrcListOption.value, testAnnoListOption.value));

			// Create 0 or more insts from each (annotation, document) pair 
			String contextTag = (trainingType.value == TRAIN_REFERENCES) ? "reference-hlabeled" : "headers-hlabeled";
			Pipe postProcessPipe = new NewHtmlTokenization2TokenSequence (true, true, true, false);
			File dictForDehyphenation = new File(resourceDirectory.value.getPath() + "/mccallum", "lowercase-usr-dict-words");
			Pipe p = new CopyData2Source();  // needed by the IEEvaluator
			InstanceList rawTrainingData = new InstanceList (p);  // This is annoying.
			InstanceList rawTestingData = new InstanceList (p);

			rawTrainingData.add (new FilePair2NewHtmlTokenizationIterator(trainNamePairs, contextTag, dictForDehyphenation, postProcessPipe));
			rawTestingData.add (new FilePair2NewHtmlTokenizationIterator(testNamePairs, contextTag, dictForDehyphenation, postProcessPipe));
			
			System.out.println("Train: Num tokenizations = " + trainNamePairs.size() + ", Num insts = " + rawTrainingData.size());
			System.out.println("Test: Num tokenizations = " + testNamePairs.size() + ", Num insts = " + rawTestingData.size());

			// Create features for training data  
			InstanceList toked = new InstanceList (tokPipe);
			toked.add (new InstanceListIterator (rawTrainingData));
			InstanceList trainingData = new InstanceList (featurePipe);
			trainingData.add (new InstanceListIterator (toked));

			toked = new InstanceList (tokPipe);
			toked.add (new InstanceListIterator (rawTestingData));
			InstanceList testingData = new InstanceList (featurePipe);
			testingData.add (new InstanceListIterator (toked));

			// Print out instance info for debugging purposes
			for (int ii = 0; ii < trainingData.size(); ii++) {
				Instance inst = trainingData.getInstance(ii);
				Sequence input = (Sequence) inst.getData();
				FeatureSequence fs = (FeatureSequence) inst.getTarget();
				System.out.println("%%%tr " + inst.getName());
				
				System.out.println(input.size() + " " + fs.size());
				
				for (int fi = 0; fi < fs.size(); fi++)
					System.out.println("\t\t" + fs.get(fi) + " " + input.get(fi));
			}
			for (int ii = 0; ii < testingData.size(); ii++) {
				Instance inst = testingData.getInstance(ii);
				Sequence input = (Sequence) inst.getData();
				FeatureSequence fs = (FeatureSequence) inst.getTarget();
				System.out.println("%%%te " + inst.getName());
				for (int fi = 0; fi < fs.size(); fi++) {
					System.out.println("\t\t" + fs.get(fi) + " " + input.get(fi));
				}
			}

			// Train the CRF, setting the parameters as appropriate					
			if (! initWeightsOption.wasInvoked()) {
				if (useCrfOption.value)
					crf = new RexaCRF (featurePipe, null);
				else
					crf = new MEMM (featurePipe, null);

				// 0 ==> 1st order, 1 ==> 1st order + zero order, 2 ==> 0+1+default2, 3 ==> 0+1+2, null)
				featurePipe.getTargetAlphabet ().lookupIndex ("<START>");
				switch (modelType.value) {
				case 0:
					logger.info ("Using first-order model");
					crf.addOrderNStates (trainingData, new int[] { 1 }, null, "<START>", null, null, false); 
					break;

				case 1:
					logger.info ("Using first-order model + node weights");
					crf.addOrderNStates (trainingData, new int[] { 0, 1 }, null, "<START>", null, null, false);
					break;

				case 2:
					logger.info ("Using first-order model + node weights + 2nd-order default features");
					crf.addOrderNStates (trainingData,
							new int[] { 0, 1, 2 },
							new boolean[] { false, false, true},
							"<START>", null, null, false);
					break;

				case 3:
					logger.info ("Using second-order model (+ 1st- and 0th- order features)");
					crf.addOrderNStates (trainingData, new int[] { 0, 1, 2 }, null, "<START>", null, null, false);
					break;

				case 4:
					logger.info ("Using default  first-order weights + node weights");
					crf.addOrderNStates (trainingData, new int[] { 0, 1 }, new boolean[] { false, true }, "<START>", null, null, false);
					break;

				case 5:
					logger.info ("Using default fully-connected first-order weights + node weights");
					crf.addOrderNStates (trainingData, new int[] { 0, 1 }, new boolean[] { false, true }, "<START>", null, null, true);
					break;

				case 6:
					logger.info ("Using first-order model with label trajectories");
					if (! useCrfOption.value) throw new IllegalArgumentException("No such method for MEMM");
					((RexaCRF) crf).addStatesForLabelTrajectoriesAsIn(trainingData, "<START>");
					break;
					
				case 7:
					logger.info ("Using fully-connected first-order model");
					crf.addOrderNStates (trainingData, new int[] { 1 }, null, "<START>", null, null, true); 
					break;
					
				case 8:
					logger.info ("Using first-order model with label trajectories collapsing authors");
					if (! useCrfOption.value) throw new IllegalArgumentException("No such method for MEMM");
					((RexaCRF) crf).addStatesForLabelTrajectoriesAsInCollapseAuthors(trainingData, "<START>");
					break;
					
				case 9:
					logger.info ("Using state cliques model");
					if (! useCrfOption.value) throw new IllegalArgumentException("No such method for MEMM");
					((RexaCRF) crf).addStateCliquesForRexaReferences("<START>");
					break;
					
				case 10:
					logger.info ("Using reg-ex trajectories");
					if (! useCrfOption.value) throw new IllegalArgumentException("No such method for MEMM");
					((RexaCRF) crf).addStatesForReferencesFromRegex();
					break;
					
				case 11:
					logger.info ("Using fully-connected 0th + first-order model");
					crf.addOrderNStates (trainingData, new int[] { 0, 1 }, null, "<START>", null, null, true); 
					break;
				}
				
				if (modelType.value != 6 && !(modelType.value >= 8 && modelType.value <= 10))
					crf.setAsStartState (crf.getState ("<START>"));
				
				crf.setUseSparseWeights (supportedOnlyOption.value);
			}

			crf.setUseForwardBackwardBeam(beamSearchOption.value);
			crf.setBeamWidth(beamWidthOption.value);
			crf.setKLeps(klEpsOption.value);
			//crf.print ();

			System.out.println ("Number of training instances = "+trainingData.size());
			System.out.println ("Number of testing instances = "+testingData.size());

			String viterbiFilePrefix = "viterbi";
			int skipNum = 30;
			//	effectively infinite	
			int trainingIterations = Integer.parseInt (System.getProperty ("rexo.crf.training.iterations", "99999"));
			IEEvaluator eval = new IEEvaluator (outputPrefix.value, viterbiFilePrefix, skipNum, 1);
			eval.setNumIterationsToSkip (0);
			eval.setNumIterationsToWait (1);

			if (useSomeSupportedOption.value && supportedOnlyOption.value) {
				crf.train (trainingData, null, testingData, eval, 10);
			}

			// Why should I be doing this? Reusing the evaluator doesn't work...
			eval = new IEEvaluator (outputPrefix.value, viterbiFilePrefix, skipNum, 1); 
			eval.setNumIterationsToSkip (0);
			eval.setNumIterationsToWait (1);
			
			if (useFeatureInductionOption.value) {
				crf.trainWithFeatureInduction (trainingData, null, testingData,
						eval, 99999,
						10, 99, 500, 0.5, false,
						new double[] {.1, .2, .5, .7});
			} else {
				crf.train (trainingData, null, testingData,
						eval,
						//              5);
						trainingIterations);
			}

			// Set in-production flag for the token classifiers pipe and NewHtmlTokenization2TokenSequence.
			// These are used to, resp,  
			//    1) use the token classifier on full (vs cross-validated) data at production time, 
			//    2) turn off memory-saving code done at training time  
			AddClassifierTokenPredictions.setInProduction(featurePipe, true);
			NewHtmlTokenization2TokenSequence.setInProduction(featurePipe, true);
			
			// Create and serialize the extractor object
			CRFExtractor extor = new CRFExtractor (crf, tokPipe);
			extor.getTokenizationPipe().setTargetProcessing(false);
			extor.getFeaturePipe().setTargetProcessing(false);
			extor.getInputAlphabet().stopGrowth();
			
			File extorFile = new File (outputPrefix.value, "extor.dat");
			ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (extorFile));
			oos.writeObject (extor);
			oos.close ();

			Extraction results = extor.extract (new InstanceListIterator (rawTestingData));

			System.out.println ("*** PER-DOCUMENT F1");
			PerDocumentF1Evaluator docEval = new PerDocumentF1Evaluator ();
			docEval.evaluate (results);

			System.out.println ("*** PER-FIELD F1");
			PerFieldF1Evaluator fieldEval = new PerFieldF1Evaluator ();
			fieldEval.evaluate (results);

			// Do some funky error analysis stuff
			LatticeViewer.setNumFeaturesToDisplay (100);
			File errFile = new File (outputPrefix.value, "errors.html");
			PrintStream errStr = new PrintStream (new FileOutputStream (errFile));
			File latticeFile = new File (outputPrefix.value, "lattice.html");
			PrintStream latticeStr = new PrintStream (new FileOutputStream (latticeFile));

			try {
				LatticeViewer.extraction2html (results, extor, errStr, false);
				LatticeViewer.extraction2html (results, extor, latticeStr, true);
			} catch (AssertionError e) {
				e.printStackTrace ();
			}
			errStr.close(); latticeStr.close();
		}
		catch(Throwable t) {
			t.printStackTrace();
		}

	}


//	This magic creates a log file in the outputPrefix dir
	private static void initOutputDirectory () throws IOException
	{
		Logger.getLogger ("").addHandler (new FileHandler
				(new File
						(outputPrefix.value, "java.log").toString ()));
	}

}
