package org.rexo.extraction.training;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Logger;

import org.rexo.extraction.DirectoryPairIterator;
import org.rexo.extraction.FilePair2NewHtmlTokenizationIterator;
import org.rexo.extraction.NewHtmlTokenization2TokenSequence;

import edu.umass.cs.mallet.base.pipe.AddClassifierTokenPredictions;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.iterator.InstanceListIterator;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * Use this TUI to:
 * 		1) Split data into train/test sets and save this info in 4 text files. 
 * 		2) Train and serialize token classifiers (0th Markov order).
 *   
 * Created on Sep 29, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */
public class TokenClassifierTrainerTUI
{
    private static final Logger logger = MalletLogger.getLogger (TokenClassifierTrainerTUI.class.getName ());

    public static final int TRAIN_HEADERS = 0;
    public static final int TRAIN_REFERENCES = 1;

    private static CommandOption.File outputPrefix = new CommandOption.File
    (TokenClassifierTrainerTUI.class, "output-prefix", null, true, null,
    		"Directory for output files..", "");
    
    private static CommandOption.File dataDir = new CommandOption.File
    (TokenClassifierTrainerTUI.class, "data-dir", null, true, null,
    		"Directory from which to read source documents.", "");
    
    private static CommandOption.File annoDir = new CommandOption.File
    (TokenClassifierTrainerTUI.class, "annotation-dir", null, true, null,
    		"Directory that contains annotation files for (possibly a subset of) documents in the data-dir.", "");
    
    private static CommandOption.File testAnnoDir = new CommandOption.File
    (TokenClassifierTrainerTUI.class, "test-annotation-dir", null, true, null,
    		"Optional argument.  If provided, files in the annotation-dir become the training set, and files in this dir become the test set.", "");

    private static CommandOption.Boolean shouldTrainClassifier = new CommandOption.Boolean
    (TokenClassifierTrainerTUI.class, "should-train-classifier", null, true, true,
    		"Optional argument.  If provided, files in the annotation-dir become the training set, and files in this dir become the test set.", "");
    
    private static CommandOption.File featurePipesOption = new CommandOption.File
    (TokenClassifierTrainerTUI.class, "feature-pipes", null, true, null,
    		"The serialized feature pipes to use to create features for the classifier.", "");
    
    private static CommandOption.Integer trainingType = new CommandOption.Integer
        (TokenClassifierTrainerTUI.class, "training-type", null, true, TRAIN_HEADERS,
         "0 == train headers, 1 == train references", "");

    private static CommandOption.Integer numCrossValidations = new CommandOption.Integer
        (TokenClassifierTrainerTUI.class, "num-cross-validations", null, true, 4,
         "Number of cross validations for training the token classifier.  CV is done on training data only.", "");

    private static CommandOption.Integer randomSeed = new CommandOption.Integer
        (TokenClassifierTrainerTUI.class, "random-seed", null, true, 1868,
         "Seed for random test / training splits", "");

    private static CommandOption.Double trainingPct = new CommandOption.Double
        (TokenClassifierTrainerTUI.class, "training-pct", null, true, 0.75,
         "Percentage of data to use for training (rest is testing)", "");
    
    private static CommandOption.File resourceDirectory = new CommandOption.File
    (TokenClassifierTrainerTUI.class, "resource-directory", null, true, null,
     "Directory where lexicons, etc., located.", null);

    
    public static void main (String args[]) throws Exception
    {
    	CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
    	options.add (TokenClassifierTrainerTUI.class);
    	options.process (args);
    	options.logOptions (logger);
    	
    	System.setProperty ("java.util.logging.config.file", "config/logging.properties");

    	// Create the trivial tokenization pipe 
    	Pipe tokPipe = new SerialPipes (new Pipe[]{
				new Noop(),
			});
		tokPipe.setTargetProcessing(false);
    	
		// Deserialize the feature pipe
		FileInputStream fis = new FileInputStream(featurePipesOption.value);
		ObjectInputStream ois = new ObjectInputStream(fis);
		SerialPipes featurePipe = (SerialPipes) ois.readObject();
		ois.close();
		
		// Add all (annotation, document) pairs
		Noop noop = new Noop();	
		noop.setTargetProcessing(false);
		InstanceList namePairsList = new InstanceList(noop);

		namePairsList.add(new DirectoryPairIterator(dataDir.value, annoDir.value, ".pstotext.xml", ".spans"));

		// Create 0 or more insts from each (annotation, document) pair
		String contextTag = (trainingType.value == TRAIN_REFERENCES) ? "reference-hlabeled" : "headers-hlabeled";
		File dictForDehyphenation = new File(resourceDirectory.value.getPath() + "/mccallum", "lowercase-usr-dict-words");
		Pipe postProcessPipe = new NewHtmlTokenization2TokenSequence (true, true, true, false);
		InstanceList raw = new InstanceList(noop);  // This is annoying.

		logger.info("Reading input files...");
		
		raw.add(new FilePair2NewHtmlTokenizationIterator(namePairsList, contextTag, dictForDehyphenation, postProcessPipe));

		logger.info("Num file pairs read = " + namePairsList.size() + " , Num insts created = " + raw.size());

		Random rand = new Random (randomSeed.value);

		InstanceList[] trainingLists = null;
		if (! testAnnoDir.wasInvoked()) {
			// Proportions below is: {training, testing}
			trainingLists = raw.split (rand, new double[] {trainingPct.value, 1 - trainingPct.value});
		}
		else {
			// Add all (annotation, document) pairs
			InstanceList namePairsList2 = new InstanceList (noop);
			namePairsList2.add(new DirectoryPairIterator(dataDir.value, testAnnoDir.value, ".pstotext.xml", ".spans"));
			
			// Create 0 or more insts from each (annotation, document) pair 
			InstanceList testRaw = new InstanceList (noop);  // This is annoying.
			testRaw.add (new FilePair2NewHtmlTokenizationIterator(namePairsList2, contextTag, dictForDehyphenation, postProcessPipe));
			trainingLists = new InstanceList[2];
			trainingLists[0] = raw;
			trainingLists[1] = testRaw;
		}

		InstanceList rawTrainingData = trainingLists[0];
		InstanceList rawTestingData = trainingLists[1];

		// Write the (anno, src) pairs for train/test sets
		logger.info("Writing train/test set filenames...");
		HashSet fileNames = new HashSet();
		StringBuffer strBuf1 = new StringBuffer();
		StringBuffer strBuf2 = new StringBuffer();
		for (int i = 0; i < rawTrainingData.size(); i++) {
			File[] pair = (File[]) rawTrainingData.getInstance(i).getSource();
			
			if (fileNames.contains(pair[0].toString())) {
				continue;
			}
			
			fileNames.add(pair[0].toString());
			strBuf1.append(pair[0]).append("\n");
			strBuf2.append(pair[1]).append("\n");
		}
		File file1 = new File(outputPrefix.value(), "trainAnno.txt");
		File file2 = new File(outputPrefix.value(), "trainSrc.txt");
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(file1));
		BufferedWriter writer2 = new BufferedWriter(new FileWriter(file2));
		writer1.write(strBuf1.toString());
		writer2.write(strBuf2.toString());
		writer1.close();
		writer2.close();

		strBuf1 = new StringBuffer();
		strBuf2 = new StringBuffer();
		for (int i = 0; i < rawTestingData.size(); i++) {
			File[] pair = (File[]) rawTestingData.getInstance(i).getSource();
			
			if (fileNames.contains(pair[0].toString())) {
				continue;
			}
			
			fileNames.add(pair[0].toString());
			strBuf1.append(pair[0]).append("\n");
			strBuf2.append(pair[1]).append("\n");
		}
		file1 = new File(outputPrefix.value(), "testAnno.txt");
		file2 = new File(outputPrefix.value(), "testSrc.txt");
		writer1 = new BufferedWriter(new FileWriter(file1));
		writer2 = new BufferedWriter(new FileWriter(file2));
		writer1.write(strBuf1.toString());
		writer2.write(strBuf2.toString());
		writer1.close();
		writer2.close();

		if (! shouldTrainClassifier.value)
			System.exit(-1);
		
		// Tokenize and create features.
		logger.info("Tokenizing files and creating features...");
		InstanceList toked = new InstanceList (tokPipe);
		toked.add (new InstanceListIterator (rawTrainingData));
		InstanceList trainingData = new InstanceList (featurePipe);
		trainingData.add (new InstanceListIterator (toked));

		// Don't pipe the test data if we desire to keep the alphabet size small 
		// for training a Transducer later (might lead to faster convergence)

		// This is done to make the data alphabet complete
		toked = new InstanceList (tokPipe);
		toked.add (new InstanceListIterator (rawTestingData));
		InstanceList testingData = new InstanceList (featurePipe);
		testingData.add (new InstanceListIterator (toked));

		// Train and serialize token classifier
		logger.info("Converting FeatureVectorSequences to AugmentableFeatureVectors...");
		Noop alphabetsPipe = new Noop(trainingData.getDataAlphabet(), trainingData.getTargetAlphabet());
		InstanceList tokenTrainList = AddClassifierTokenPredictions.convert(trainingData, alphabetsPipe);

		logger.info("Training token classifiers...");
		AddClassifierTokenPredictions.TokenClassifiers tokClassifier = 
			new AddClassifierTokenPredictions.TokenClassifiers(tokenTrainList, randomSeed.value, numCrossValidations.value);
		File tokClassifierFile = new File (outputPrefix.value, "tokClassifier.dat");
		ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (tokClassifierFile));
		oos.writeObject(tokClassifier);
		oos.close();

		logger.info("DONE");
    }

}