/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet

   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
	Fuchun Peng, July 2003
 */

//package edu.umass.cs.project.extraction;
package org.rexo.extraction;

//import edu.umass.cs.mallet.users.fuchun.info_extraction_base.IEEvaluator;
//import edu.umass.cs.mallet.users.fuchun.info_extraction_base.IEInterface3;
//import edu.umass.cs.mallet.users.fuchun.info_extraction_base.IEInterface;

//import edu.umass.cs.mallet.users.fuchun.clustering.cora2.SGML2TokenSequence;
//import edu.umass.cs.mallet.users.fuchun.info_extraction_base.CRFIO;

import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.fst.*;
import edu.umass.cs.mallet.base.minimize.*;
import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.pipe.iterator.*;
import edu.umass.cs.mallet.base.pipe.tsf.*;
import edu.umass.cs.mallet.base.util.*;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.*;
import java.io.*;
import java.util.ArrayList;


public class TUI {

	private static String CAPS = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ]";
	private static String LOW = "[a-zàèìòùáéíóúçñïü]";
	private static String CAPSNUM = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ0-9]";
	private static String ALPHA = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü]";
	private static String ALPHANUM = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü0-9]";
	private static String PUNT = "[,\\.;:?!()]";
	private static String COM = "[\"`']";
//	private static String[] SEPERATOR = new String[] {"<NEW_HEADER>", "<NEWREFERENCE>"};
//	private static String[] SEPERATOR = new String[] {"<NEW_HEADER>", "^$"};
	private static String[] SEPERATOR = new String[] {"<NEW_INSTANCE>", "<NEW_INSTANCE>"};

	private static String[] filePrefix = new String[] {"header", "reference"};	

        private static String lexdir = "/usr/col/scratch1/fuchun/mallet/src/edu/umass/cs/mallet/users/fuchun/cora2/resources/";
	private static int[][] offsetsOption = new int[][] { {-1}, {1}} ;
//	private static int[][] offsetsOption = new int[][] { {-1,0}};

	private static double validationPortion = 0;//0.1069;//
//	private static double trainingPortion = 0.5347-validationPortion;//headers
//	private static double testingPortion  = 1-validationPortion-trainingPortion;//0.4653;//

//	private static double trainingPortion = 0.6667;//reference

	private static double trainingPortion = 0.7-validationPortion;//headers
	private static double testingPortion  = 1-validationPortion-trainingPortion;//0.4653;//


	static CommandOption.File crfInputFileOption = new CommandOption.File
	(TUI.class, "crf-input-file", "FILENAME", true, null,
	 "The name of the file to read the trained CRF for testing.", null);

	static CommandOption.File dataFileOption = new CommandOption.File
	(TUI.class, "data-file", "FILENAME", true, null,
	 "The name of the file containing the training data (and potentially also testing data, "+
	 "after a split.", null);

	static CommandOption.File testFileOption = new CommandOption.File
	(TUI.class, "test-file", "FILENAME", true, null,
	 "The name of the file containing the testing data.", null);

	static CommandOption.String ttestFileOption = new CommandOption.String
	(TUI.class, "ttest-file", "FILENAME", true, null,
	 "The name of the file containing the testing data.", null);


	static CommandOption.File validationFileOption = new CommandOption.File
	(TUI.class, "validation-file", "FILENAME", true, null,
	 "The name of the file containing the validation data.", null);

	static CommandOption.Double trainingProportionOption = new CommandOption.Double
	(TUI.class, "training-proportion", "DECIMAL", true, trainingPortion,
	 "The fraction of the data-file instances that should be used for training.", null);

	static CommandOption.Double testingProportionOption = new CommandOption.Double
	(TUI.class, "testing-proportion", "DECIMAL", true, testingPortion,
	 "The fraction of the data-file instances that should be used for testing.", null);

	static CommandOption.Set statesOption = new CommandOption.Set
	(TUI.class, "state-type", "STATETYPE", true,
	 new String[] {"fullyconnected", "halfconnected", "3quarterconnected", "bilabels"}, 1,
	 "The method for adding states into CRF ", null);

	static CommandOption.Boolean featureInductionOption = new CommandOption.Boolean
	(TUI.class, "feature-induction", "FI", false, true,
	 "Whether or not using feature induction", null);

	static CommandOption.Integer headOrRefOption = new CommandOption.Integer
	(TUI.class, "head-or-ref", "INTEGER", true, 0,
	 "0 for header, 1 for reference", null);


	static CommandOption.File bibtexLexiconTitleFileOption = new CommandOption.File
	(TUI.class, "bibtex-lexicon-title", "FILENAME", true, null,
	 "", null);

	static CommandOption.File bibtexLexiconAuthorFileOption = new CommandOption.File
	(TUI.class, "bibtex-lexicon-author", "FILENAME", true, null,
	 "", null);

	static CommandOption.File bibtexLexiconNoteFileOption = new CommandOption.File
	(TUI.class, "bibtex-lexicon-note", "FILENAME", true, null,
	 "", null);

	static CommandOption.File bibtexLexiconDateFileOption = new CommandOption.File
	(TUI.class, "bibtex-lexicon-date", "FILENAME", true, null,
	 "", null);

	static CommandOption.File bibtexLexiconDegreeFileOption = new CommandOption.File
	(TUI.class, "bibtex-lexicon-degree", "FILENAME", true, null,
	 "", null);

	static CommandOption.File bibtexLexiconAffiliationFileOption = new CommandOption.File
	(TUI.class, "bibtex-lexicon-affiliation", "FILENAME", true, null,
	 "", null);


	static final CommandOption.List commandOptions =
	new CommandOption.List (
		"Training, testing and running information extraction on paper header or reference.",
		new CommandOption[] {
			crfInputFileOption,
			dataFileOption,
			testFileOption,
			ttestFileOption,
			validationFileOption,
			trainingProportionOption,
			testingProportionOption,
	//		statesOption,
			featureInductionOption,
			headOrRefOption,
			bibtexLexiconTitleFileOption,
			bibtexLexiconAuthorFileOption,
			bibtexLexiconNoteFileOption,
			bibtexLexiconDateFileOption,
			bibtexLexiconDegreeFileOption,
			bibtexLexiconAffiliationFileOption
		});


	public static void main (String[] args) throws FileNotFoundException
	{
		commandOptions.process (args);

		boolean excludeBibtexLexiconsOption = true;
		Pipe bibtexLexiconsPipe = null;
		if(!excludeBibtexLexiconsOption){
			bibtexLexiconsPipe = new SerialPipes (new Pipe[] {
//			new TrieLexiconMembership ("BIBTEX_TITLE", bibtexLexiconTitleFileOption.value, true),
			new LexiconMembership ("BIBTEX_AUTHOR", bibtexLexiconAuthorFileOption.value, true),
			new LexiconMembership ("BIBTEX_DATE", bibtexLexiconDateFileOption.value, true),
			new LexiconMembership ("NOTES", bibtexLexiconNoteFileOption.value, true),
//			new LexiconMembership ("DEGREE", bibtexLexiconDegreeFileOption.value, true),
//			new LexiconMembership ("AFFILIATION", bibtexLexiconAffiliationFileOption.value, true),
			});
		}

		String emailStr = "\\S+@\\S+";
		Pipe p = new SerialPipes (new Pipe[] {
			new Input2CharSequence(),
//			new SGML2TokenSequence(new CharSequenceLexer (Pattern.compile ("\\n|\\S+@\\S+|\\w+-\\w+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}")), "O"),//old

//			new SGML2TokenSequence(new CharSequenceLexer (Pattern.compile ("\\n|\\S+")), "O"),//new

			//changed from SGML to XML on Nov. 1, 2003
//			new XML2TokenSequence(new CharSequenceLexer (Pattern.compile ("\\n|\\S+")), "O"),
			new XML2TokenSequence(new CharSequenceLexer (Pattern.compile ("\\n|\\S+@\\S+|\\w+-\\w+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}")), "O"),

			new RegexMatches ("INITCAP", Pattern.compile (CAPS+".*")),
			new RegexMatches ("ALLCAPS", Pattern.compile (CAPS+"+")),
			new RegexMatches ("CONTAINSDIGITS", Pattern.compile (".*[0-9].*")),
			new RegexMatches ("ALLDIGITS", Pattern.compile ("[0-9]+")),
////			new RegexMatches ("NUMERICAL", Pattern.compile ("[-0-9\\.]*\\.[0-9\\.]+")),
			new RegexMatches ("PHONEORZIP", Pattern.compile ("[0-9]+-[0-9]+")),//added
			new RegexMatches ("CONTAINSDOTS", Pattern.compile ("[^\\.]*\\..*")), //added
			new RegexMatches ("CONTAINSDASH", Pattern.compile (ALPHANUM+"+-"+ALPHANUM+"*")),//added
			new RegexMatches ("ACRO", Pattern.compile ("[A-Z][A-Z\\.]*\\.[A-Z\\.]*")),//added
			new RegexMatches ("LONELYINITIAL", Pattern.compile (CAPS+"\\.")),//added
			new RegexMatches ("SINGLECHAR", Pattern.compile (ALPHA)),
			new RegexMatches ("CAPLETTER", Pattern.compile ("[A-Z]")),
			new RegexMatches ("PUNC", Pattern.compile (PUNT)),
			new RegexMatches ("URL", Pattern.compile ("www\\..*|http://.*|ftp\\..*")),
			new RegexMatches ("EMAIL", Pattern.compile ("\\S+@\\S+|e-mail.*|email.*|Email.*")),//added

//			new LineBreakFeature(), 
//			new FontFeature(),

			(!excludeBibtexLexiconsOption ? bibtexLexiconsPipe : new Noop ()),

			new TokenText ("WORD="),
//			new TokenTextNGrams("ChARNGRAM=", new int[] {1,2,3}), //added Nov. 6
			
////			new PrintTokenSequenceFeatures(),
			new OffsetConjunctions (offsetsOption),
			new Target2LabelSequence (),
			new TokenSequence2FeatureVectorSequence (true, true)
		});

                //xxx print out the read-in pipes, just for debugging purpose
                ArrayList pipes = ((SerialPipes)p).getPipes();
                System.out.println("pipes");
                for (int i = 0; i < pipes.size(); i++) {
                        System.out.print("Pipe: " + i + ": ");
                        Pipe tempP = (Pipe) pipes.get (i);
                        if (tempP == null) {
                                System.out.println("Pipe is null");
                        }
                        else {
                                String pipeName = tempP.getClass().getName();
                                System.out.println(pipeName);
				if(tempP instanceof SerialPipes){
					ArrayList pipes2 = ((SerialPipes)tempP).getPipes();
			
					for(int j=0; j<pipes2.size(); j++){
						System.out.print("	Pipe: " + j + ": ");
						Pipe tempP2 = (Pipe) pipes2.get(j);
						if(tempP2 == null){
							System.out.println("	Pipe is null");
						}
						else{
				                        String pipeName2 = tempP2.getClass().getName();
                                			System.out.println(pipeName2);
						}
					}
				}
                        }
                }

		if (crfInputFileOption.wasInvoked())
			mainRun (p);
		else{
		
			if(headOrRefOption.value == 0){
				if(dataFileOption.value == null){
					dataFileOption.value = new File (lexdir + "tagged_headers.txt");
				}
			
			}
			else if(headOrRefOption.value == 1){
				if(dataFileOption.value == null){
					dataFileOption.value = new File(lexdir + "tagged_references.txt");
				}
			}
			else{	
				throw new UnsupportedOperationException ("Not supported option value.");
			}

			System.out.println ("Data file = "+dataFileOption.value);

			mainTrain (p);
		}
	}

	public static void printOut(InstanceList list, String tag, String outFile)
	{
		String dir = "/usr/col/tmp1/fuchun/mallet/src/edu/umass/cs/mallet/users/fuchun/LMTagging/data/";
		outFile = dir + outFile;
		PrintStream outputStream = null;
		try{
			outputStream = new PrintStream ( new FileOutputStream (outFile) );
		}
		catch(FileNotFoundException e){
			
		}
		
		if(outputStream == null) outputStream = System.out;

		TokenSequence sourceTokenSequence = null;
	
		for(int i=0; i<list.size(); i++){
			outputStream.println("### " + tag + " instance #" + i);
			Instance instance = list.getInstance(i);
			Sequence trueOutput = (Sequence) instance.getTarget();

			sourceTokenSequence = (TokenSequence)instance.getSource();
			for (int j = 0; j < trueOutput.size(); j++) {
				if (outputStream != null && sourceTokenSequence != null) {
					outputStream.println (sourceTokenSequence.getToken(j).getText()+" :: " 
					 + "S_"+trueOutput.get(j).toString());
				}
			}

		}

		outputStream.close();
	}

	public static void printOut2(InstanceList list, String tag, Alphabet targets)
	{

		String dir = "/usr/col/tmp1/fuchun/mallet/src/edu/umass/cs/mallet/users/fuchun/LMTagging/data/";
		String statesFile = dir + tag + "_states";
		PrintStream statesStream = null;
		try{
			statesStream = new PrintStream ( new FileOutputStream (statesFile) );
		}
		catch(FileNotFoundException e){
			System.out.println("Can not create stream for states");
		}
		
		if(statesStream == null) statesStream = System.out;

		PrintStream[] singleStateStream = new PrintStream[targets.size()];
		for(int i=0; i<targets.size(); i++){
			try{
				singleStateStream[i] = new PrintStream(new FileOutputStream( dir + tag + "_" + targets.lookupObject(i)));
			}
			catch(FileNotFoundException e){
				System.out.println("Can not create stream for target " + targets.lookupObject(i));
			}
			if(singleStateStream[i] == null) singleStateStream[i] = System.out;
		}

		TokenSequence sourceTokenSequence = null;
	
		for(int i=0; i<list.size(); i++){
			Instance instance = list.getInstance(i);
			Sequence trueOutput = (Sequence) instance.getTarget();

			sourceTokenSequence = (TokenSequence)instance.getSource();
			for (int j = 0; j < trueOutput.size(); j++) {
				if (statesStream != null && trueOutput != null) {
					statesStream.print ("S_"+trueOutput.get(j).toString() + " ");
				}

				int index = targets.lookupIndex(trueOutput.get(j));
				if (singleStateStream[index] != null && sourceTokenSequence != null) {
					singleStateStream[index].print (sourceTokenSequence.getToken(j).getText() + " ");
				}

				
			}

			//line break
			statesStream.println ();
			for(int j=0; j<targets.size(); j++){
				singleStateStream[j].println();
			}

		}

		statesStream.close();
		for(int i=0; i<targets.size(); i++){
			singleStateStream[i].close();
		}
	}

	public static void mainTrain (Pipe p) 
	{
		boolean avoidFeatureInduction = true;
  
                int labelGramOption = 1;//   1: fully connected
                                        //   2: half connected
                                        //   3: three quarter connected
                                        //   4: biLabel connected

 
		InstanceList trainingData = new InstanceList (p);
		Reader reader;
		try {
			reader = new FileReader (dataFileOption.value);
		} catch (Exception e) {
			throw new IllegalArgumentException ("Can't read file "+dataFileOption.value);
		}
		
		trainingData.add (new LineGroupIterator (reader, Pattern.compile(SEPERATOR[headOrRefOption.value()]), true));

		InstanceList testingData = null;
		InstanceList validationData = null;
		if (testFileOption.wasInvoked()) {
			testingData = new InstanceList (p);
						
			try {
				reader = new FileReader (testFileOption.value);
			} catch (Exception e) {
				throw new IllegalArgumentException ("Can't read file "+ testFileOption.value);
			}

			testingData.add (new LineGroupIterator (reader, Pattern.compile(SEPERATOR[headOrRefOption.value()]),true));
		}

		if (testingData == null) {
			// For now, just train on a small fraction of the data
			Random r = new Random (1);
			// Proportions below is: {training, testing, ignore}
			InstanceList[] trainingLists = trainingData.split (r, new double[] {trainingProportionOption.value, 
												testingProportionOption.value, 
								1-trainingProportionOption.value-testingProportionOption.value});
			trainingData = trainingLists[0];
			testingData = trainingLists[1];
			validationData = trainingLists[2];
			trainingLists = null;
		}

//		trainingData = combineData(trainingData,bibtexData);
	

		System.out.println("Fuchun: " + p.getTargetAlphabet());	
		// Print out all the target names
		Alphabet targets = p.getTargetAlphabet();

		assert(targets != null);
		System.out.println("target size: " +  targets.size());

		System.out.print ("State labels:");
		for (int i = 0; i < targets.size(); i++)
			System.out.print (" " + targets.lookupObject(i));
		System.out.println ("");


		//output training, testing data
		boolean onlyWantData =false;
		if(onlyWantData){
			printOut(trainingData, "Training", "trainingdata");
			printOut(testingData, "Testing", "testingdata");

			printOut2(trainingData, "Training", targets);
			printOut2(testingData, "Testing", targets);
				
			return;
		}


		// Print out some feature information
		System.out.println ("Number of features = "+p.getDataAlphabet().size());


              CRF crf = new CRF (p, null);
//		CRF3 crf = new CRF3(p, null); 
		System.out.println("crf is : " + (crf.getClass()).getName() );

                if (labelGramOption == 1)
                        crf.addStatesForLabelsConnectedAsIn(trainingData);
                else if(labelGramOption == 2)
                        crf.addStatesForHalfLabelsConnectedAsIn (trainingData);
                else if(labelGramOption == 3){
//                        crf.addStatesForThreeQuarterLabelsConnectedAsIn (trainingData);
                }
		else if (labelGramOption == 4)
                        crf.addStatesForBiLabelsConnectedAsIn (trainingData);
                else
                        throw new IllegalStateException ("label-gram must be 1 or 2, not "+ labelGramOption);

		String crfOutputFile = "CRFOutput_"+ filePrefix[headOrRefOption.value];

		long timeStart = System.currentTimeMillis();

		System.out.println("Training on "+trainingData.size()+" training instances, " 
		+ validationData.size() + " validation instances, " 
		+ testingData.size()+" testing instances...");
	
		String viterbiFilePrefix = "viterbi";	
		int skipNum = 30;
		if(avoidFeatureInduction){
	//		crf.train (trainingData, null, testingData, new IEEvaluator(viterbiFilePrefix, skipNum), 5);
	//		crf.train (trainingData, null, testingData, new IEEvaluator(viterbiFilePrefix, skipNum), 10);
	//		crf.train (trainingData, null, testingData, new IEEvaluator(viterbiFilePrefix, skipNum), 15);
				
			crf.train (trainingData, validationData, testingData, new IEEvaluator(viterbiFilePrefix, skipNum), 99999);
		}
		else{
			crf.trainWithFeatureInduction (trainingData, null, testingData, new IEEvaluator(viterbiFilePrefix, skipNum), 99999, 8, 20, 500, 0.8, false, new double[] {.2, .6});
		}

		crf.write(new File(crfOutputFile) );

//		System.out.println("================= start of CRF ============");
//		crf.print();
//		System.out.println("==================end of crf ==============");

		long timeEnd = System.currentTimeMillis();
		double timeElapse = (timeEnd - timeStart)/(1000.000);

		System.out.println("Time elapses " + timeElapse + " seconds for training.");

//		IEInterface ieInterface = new IEInterface();
//		ieInterface.loadCRF(crf);
//		ieInterface.viterbiCRF(ttestFileOption.value, true, SEPERATOR[headOrRefOption.value()]);
	}


	public static void mainRun (Pipe p) 
	{

		IEInterface ieInterface = new IEInterface(crfInputFileOption.value);
//		IEInterface3 ieInterface = new IEInterface3(crfInputFileOption.value);

		ieInterface.loadCRF();

		ieInterface.viterbiCRF(ttestFileOption.value, true, SEPERATOR[headOrRefOption.value()]);
	}
	
}
