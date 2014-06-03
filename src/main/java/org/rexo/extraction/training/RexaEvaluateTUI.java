/**
 * Created on Feb 7, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author asaunders, ghuang
 */

package org.rexo.extraction.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.rexo.extraction.CopyData2Source;
import org.rexo.extraction.FilePair2NewHtmlTokenizationIterator;
import org.rexo.extraction.IEEvaluator;
import org.rexo.extraction.NewHtmlTokenization2TokenSequence;
import org.rexo.extraction.SourceAnnoListsIterator;

import edu.umass.cs.mallet.base.extract.CRFExtractor;
import edu.umass.cs.mallet.base.extract.Extraction;
import edu.umass.cs.mallet.base.extract.LatticeViewer;
import edu.umass.cs.mallet.base.extract.PerFieldF1Evaluator;
import edu.umass.cs.mallet.base.pipe.AddClassifierTokenPredictions;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.iterator.InstanceListIterator;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * A TUI for running various evaluators for a serialized extractor.
 */
public class RexaEvaluateTUI 
{
	private static final Logger logger = MalletLogger.getLogger (RexaEvaluateTUI.class.getName ());
	
	public static final int TRAIN_HEADERS = 0;
	public static final int TRAIN_REFERENCES = 1;
	
	private static CommandOption.File outputPrefix = new CommandOption.File
	(RexaEvaluateTUI.class, "output-prefix", null, true, null,
			"Directory for output files.", "");
	
	private static CommandOption.File testSrcListOption = new CommandOption.File
	(RexaEvaluateTUI.class, "test-src-list", null, true, null,
			"File containing a list of source filenames for test set.", "");
	
	private static CommandOption.File testAnnoListOption = new CommandOption.File
	(RexaEvaluateTUI.class, "test-anno-list", null, true, null,
			"File containing a list of annotation filenames for test set.", "");
	
	private static CommandOption.File extractorFile = new CommandOption.File
	(RexaEvaluateTUI.class, "extractor-file", null, true, null,
			"The trained CRF4 extractor to deserialize and evaluate.", "");
	
	private static CommandOption.File resourceDirectory = new CommandOption.File
	(RexaEvaluateTUI.class, "resource-directory", null, true, null,
			"Directory where lexicons, etc., located.", null);
	
	private static CommandOption.Integer trainingType = new CommandOption.Integer
	(RexaEvaluateTUI.class, "training-type", null, true, TRAIN_HEADERS,
			"0 == train headers, 1 == train references", "");
	
	public static void main (String args[]) throws IOException
	{
		try {
			CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
			options.add (RexaEvaluateTUI.class);
			options.process (args);
			options.logOptions (logger);
			initOutputDirectory ();
			
			System.setProperty ("java.util.logging.config.file", "config/logging.properties");
			
			// Deserialize the extractor
			logger.info("Deserializing extractor...");
			FileInputStream fis = new FileInputStream(extractorFile.value);
			ObjectInputStream ois = new ObjectInputStream(fis);
			CRFExtractor extractor = (CRFExtractor) ois.readObject();
			ois.close();

			logger.info("Extractor targets:\n" + extractor.getTargetAlphabet());
			
			assert(extractor.getInputAlphabet().growthStopped());
			Pipe tokPipe = extractor.getTokenizationPipe();
			tokPipe.setTargetProcessing(false);
			Pipe featurePipe = extractor.getFeaturePipe();
			featurePipe.setTargetProcessing(true);

			// Set in-production flag for the appropriate feature pipes 
			AddClassifierTokenPredictions.setInProduction(featurePipe, false);
			NewHtmlTokenization2TokenSequence.setInProduction(featurePipe, false);

			// Add all (annotation, document) pairs
			Pipe noop = new Noop ();
			noop.setTargetProcessing (false);
			
			InstanceList testNamePairs = new InstanceList (noop);
			testNamePairs.add(new SourceAnnoListsIterator(testSrcListOption.value, testAnnoListOption.value));

			// Create 0 or more insts from each (annotation, document) pair 
			String contextTag = (trainingType.value == TRAIN_REFERENCES) ? "reference-hlabeled" : "headers-hlabeled";
			Pipe postProcessPipe = new NewHtmlTokenization2TokenSequence (true, true, true, false);
			File dictForDehyphenation = new File(resourceDirectory.value.getPath() + "/mccallum", "lowercase-usr-dict-words");
			Pipe p = new CopyData2Source();  // needed by the IEEvaluator
			InstanceList rawTestingData = new InstanceList (p); // This is annoying.

			rawTestingData.add (new FilePair2NewHtmlTokenizationIterator(testNamePairs, contextTag, dictForDehyphenation, postProcessPipe));
			
			System.out.println("Test: Num tokenizations = " + testNamePairs.size() + ", Num insts = " + rawTestingData.size());

			// Create features for test data  
			InstanceList toked = new InstanceList (tokPipe);
			toked.add (new InstanceListIterator (rawTestingData));
			InstanceList testingData = new InstanceList (featurePipe);
			testingData.add (new InstanceListIterator (toked));

			logger.info("Test: Num tokenizations = " + testNamePairs.size() + ", Num insts = " + testingData.size());

/*			logger.info("Using IEEvaluator...");
			IEEvaluator eval = new IEEvaluator (outputPrefix.value, "viterbi", 10, 1);
			eval.evaluate(extractor.getCrf(), true, 0, true, 0, null, null, testingData);

			Extraction results = extractor.extract (testingData);
			
			logger.info ("*** PER-DOCUMENT F1 ***");
			PerDocumentF1Evaluator docEval = new PerDocumentF1Evaluator ();
			docEval.evaluate (results);
			
			logger.info ("Evaluator=PerFieldF1Evaluator...");
			logger.info ("*** PER-FIELD F1 ***");
			PerFieldF1Evaluator fieldEval = new PerFieldF1Evaluator ();
			fieldEval.evaluate (results);
*/
						
			LatticeViewer.setNumFeaturesToDisplay (100);
			File errFile = new File (outputPrefix.value, "errors.html");
			PrintStream errStr = new PrintStream (new FileOutputStream (errFile));
			File latticeFile = new File (outputPrefix.value, "lattice.html");
			PrintStream latticeStr = new PrintStream (new FileOutputStream (latticeFile));
			
			try {
				logger.info ("Performing extraction...");
				Extraction results = extractor.extract (testingData);
				logger.info ("Generating error html...");
				LatticeViewer.extraction2html (results, extractor, errStr, false);
			/*	
				logger.info ("Generating lattice html...");
				 results = extractor.extract (new InstanceListIterator (testingData));
				 LatticeViewer.extraction2html (results, extractor, latticeStr, true);
				 */
			} catch (AssertionError e) {
				e.printStackTrace ();
			}
			errStr.close(); 
			latticeStr.close();
			
		}
		catch(Throwable t) {
			t.printStackTrace();
		}

	}

	// This magic creates a log file in the outputPrefix dir
	private static void initOutputDirectory () throws IOException
	{
		Logger.getLogger ("").addHandler (new FileHandler
				(new File
						(outputPrefix.value, "java.log").toString ()));
	}
}
