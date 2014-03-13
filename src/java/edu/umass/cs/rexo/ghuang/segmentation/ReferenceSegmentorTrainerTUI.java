package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.rexo.extraction.IEEvaluator;
import org.rexo.referencetagging.NewHtmlTokenization;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.Target2LabelSequence;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureVectorSequence;
import edu.umass.cs.mallet.base.pipe.tsf.OffsetConjunctions;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;


public class ReferenceSegmentorTrainerTUI 
{
	static Pattern LEXER_PATTERN = NewHtmlTokenization.LEXER_PATTERN;

	private static final Logger logger = MalletLogger.getLogger (ReferenceSegmentorTrainerTUI.class.getName ());

    private static CommandOption.File outputPrefixOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "output-prefix", null, true, null,
    		"output directory for logs", "");
	
    private static CommandOption.File trainingDataOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "training-data", null, true, null,
    		"file containing training data in the \"list of lines\" format..", "");
    
    private static CommandOption.File testDataOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "test-data", null, true, null,
    		"file containing test data in the \"list of lines\" format..", "");
    
    private static CommandOption.File publisherFileOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "publishers-list", null, true, null,
    		"file containing a list of publisher names..", "");

    private static CommandOption.File lastNameFileOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "last-names-list", null, true, null,
    		"file containing a list of last names..", "");
    
    private static CommandOption.File firstNameFileOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "first-names-list", null, true, null,
    		"file containing a list of first names..", "");
    
    private static CommandOption.File countryNameFileOption = new CommandOption.File
    (ReferenceSegmentorTrainerTUI.class, "country-names-list", null, true, null,
    		"file containing a list of country names..", "");
    
    private static int[][] offsetsOption = new int[][] { { -5 }, { -4 },
			{ -3 }, { -2 }, { -1 }, { 1 }, { 2 }, { 3 }, { 4 }, { 5 },
			{ 0, 0 }, { -1, 0 }, { 0, 1 } };
    
    
	public static void main(String[] args) throws IOException 
	{
		CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
		options.add (ReferenceSegmentorTrainerTUI.class);
		options.process (args);
		options.logOptions (logger);
		
		SerialPipes pipes = new SerialPipes(new Pipe[] {
				new LabeledSegmentationData2LineInfo(),
				new LineInfo2BiblioSelection(),
				new LineInfo2BiblioBIOLabels(),
				new TokenizeLineInfo(LEXER_PATTERN),
				new TrieLexiconMembership4LineInfo("containsPublisher",	publisherFileOption.value, 
						false, LEXER_PATTERN, null),
				new TrieLexiconMembership4LineInfo("containsFirstName", firstNameFileOption.value, 
						true, LEXER_PATTERN,Pattern.compile("^.{1,2}\\.?$")),
				new TrieLexiconMembership4LineInfo ("containsLastName", lastNameFileOption.value, 
						true, LEXER_PATTERN, Pattern.compile("^.{1,2}\\.?$")),
				new TrieLexiconMembership4LineInfo ("containsCountryName", countryNameFileOption.value, 
						false, LEXER_PATTERN, null),
				new LineInfo2TokenSequence(),
				new OffsetConjunctions (offsetsOption),
				new TokenSequence2FeatureVectorSequence (true, false),
				new Target2LabelSequence(),
		});

		InstanceList trainList = new InstanceList(pipes);
		InstanceList testList = new InstanceList(pipes);

		trainList.add(new LabeledSegmentationDataIterator(trainingDataOption.value));
		testList.add(new LabeledSegmentationDataIterator(testDataOption.value));

		System.out.println("Num training insts=" + trainList.size());
		System.out.println("Num test insts=" + testList.size());

		int skipNum = 30;

		IEEvaluator eval = new IEEvaluator (outputPrefixOption.value, "viterbi", skipNum, 1);
		eval.setNumIterationsToSkip (0);
		eval.setNumIterationsToWait (1);

		CRF4 crf = new CRF4 (pipes, null);
		crf.setUseSparseWeights (true);
		crf.addOrderNStates (trainList, new int[] { 0, 1 }, null, "<START>", null, null, true);

		// Some supported features
		crf.train (trainList, null, testList, eval, 10);

		// Why should I be doing this? Reusing the evaluator doesn't work...
		eval = new IEEvaluator (outputPrefixOption.value, "viterbi", skipNum, 1); 
		eval.setNumIterationsToSkip (0);
		eval.setNumIterationsToWait (1);

		crf.train (trainList, null, testList, eval, 99999);
		
		// Replace the input pipe to handle NewHtmlTokenization as input
		for (int i = 0; i < 2; i++)
			pipes.removePipe(0);
		
		pipes.replacePipe(0, new NewHtmlTokenization2LineInfo());
		
		// Serialize the trained transducer
		crf.getInputPipe().setTargetProcessing(false);
		crf.getInputAlphabet().stopGrowth();
		
		File extorFile = new File (outputPrefixOption.value, "crf.dat");
		ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (extorFile));
		oos.writeObject (crf);
		oos.close ();
	}

}
