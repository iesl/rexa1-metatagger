package org.rexo.extraction.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.rexo.extraction.NewHtmlTokenization2TokenSequence;

import edu.umass.cs.mallet.base.extract.CRFExtractor;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.AddClassifierTokenPredictions;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * Use this class to create and serialize a CRFExtractor from a partially 
 * trained CRF4 
 * 
 * @author ghuang
 *
 */
public class ProcessPartiallyTrainedTransducerTUI
{
	private static final Logger logger = MalletLogger.getLogger (ProcessPartiallyTrainedTransducerTUI.class.getName ());
	
	private static CommandOption.File outputPrefixOption = new CommandOption.File
	(ProcessPartiallyTrainedTransducerTUI.class, "output-prefix", null, true, null,
			"Directory for output file..", "");
	
	private static CommandOption.File inputFileOption = new CommandOption.File
	(ProcessPartiallyTrainedTransducerTUI.class, "input-transducer", null, true, null,
			"The (partially) trained and serialized transducer object.", "");
	
	public static void main(String[] args)
	{
		try{
			CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
			options.add (ProcessPartiallyTrainedTransducerTUI.class);
			options.process (args);
			options.logOptions (logger);
			initOutputDirectory ();
			
			System.setProperty ("java.util.logging.config.file", "config/logging.properties");
	
			// Deserialize the partially trained transducer
			FileInputStream fis = new FileInputStream(inputFileOption.value);
			ObjectInputStream ois = new ObjectInputStream(fis);
			CRF4 crf4 = (CRF4) ois.readObject();
			ois.close();
			
			// Set in-production flag for the token classifiers pipe and NewHtmlTokenization2TokenSequence 
			Pipe featurePipe = crf4.getInputPipe();
			AddClassifierTokenPredictions.setInProduction(featurePipe, true);
			NewHtmlTokenization2TokenSequence.setInProduction(featurePipe, true);
			
			// Create and serialize the extractor object
			Pipe tokPipe = new Noop();
			tokPipe.setTargetProcessing(false); 
			
			CRFExtractor extor = new CRFExtractor (crf4, tokPipe);
			extor.getTokenizationPipe().setTargetProcessing(false);
			extor.getFeaturePipe().setTargetProcessing(true);
			extor.getInputAlphabet().stopGrowth();
			
			File extorFile = new File (outputPrefixOption.value, "part-trained-extor.dat");
			ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (extorFile));
			oos.writeObject (extor);
			oos.close ();
		}
		catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	
	
//	This magic creates a log file in the outputPrefix dir
	private static void initOutputDirectory () throws IOException
	{
		Logger.getLogger ("").addHandler (new FileHandler
				(new File
						(outputPrefixOption.value, "java.log").toString ()));
	}
}