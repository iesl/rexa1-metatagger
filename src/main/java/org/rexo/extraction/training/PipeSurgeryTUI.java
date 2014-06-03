package org.rexo.extraction.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import edu.umass.cs.mallet.base.extract.CRFExtractor;
import edu.umass.cs.mallet.base.pipe.AddClassifierTokenPredictions;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * This is a utility class that performs pipe surgery on a serialized CRFExtractor
 * 
 * @author ghuang
 *
 */
public class PipeSurgeryTUI
{
	private static final Logger logger = MalletLogger.getLogger (PipeSurgeryTUI.class.getName ());
	
	private static CommandOption.File outputPrefixOption = new CommandOption.File
	(PipeSurgeryTUI.class, "output-prefix", null, true, null,
			"Directory for output file..", "");
	
	private static CommandOption.File inputFileOption = new CommandOption.File
	(PipeSurgeryTUI.class, "input-extractor", null, true, null,
			"The serialized CRFExtractor object.", "");
	
	public static void main(String[] args)
	{
		try {
			CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
			options.add (PipeSurgeryTUI.class);
			options.process (args);
			options.logOptions (logger);
			initOutputDirectory ();
			
			System.setProperty ("java.util.logging.config.file", "config/logging.properties");
			
			// Deserialize the CRFExtractor
			FileInputStream fis = new FileInputStream(inputFileOption.value);
			ObjectInputStream ois = new ObjectInputStream(fis);
			CRFExtractor extractor = (CRFExtractor) ois.readObject();
			ois.close();
			
			Pipe featurePipe = extractor.getFeaturePipe();
			
			featurePipe = replaceACTP(featurePipe);
			featurePipe.getDataAlphabet().stopGrowth();
			
			// todo reinstate: extractor.setFeaturePipe(featurePipe);
			
			// Serialize the CRFExtractor
			File extorFile = new File (outputPrefixOption.value, "extor.dat");
			ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (extorFile));
			oos.writeObject (extractor);
			oos.close ();
		}
		catch (Exception e) {
			System.err.println(e.toString());
		}

	}

	public static Pipe replaceACTP(Pipe p)
	{
		if (p instanceof AddClassifierTokenPredictions)
			return new Noop();
		else if (p instanceof SerialPipes) {
			SerialPipes sp = (SerialPipes) p;
			for (int i = 0; i < sp.size(); i++)
				sp.replacePipe(i, replaceACTP(sp.getPipe(i)));
		}
		
		return p;
	}
	
	//	This magic creates a log file in the outputPrefix dir
	private static void initOutputDirectory () throws IOException
	{
		Logger.getLogger ("").addHandler (new FileHandler
				(new File
						(outputPrefixOption.value, "java.log").toString ()));
	}
}
