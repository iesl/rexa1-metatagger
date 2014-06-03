package org.rexo.extraction.training;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.rexo.extraction.NewHtmlTokenization2TokenSequence;

import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.Target2LabelSequence;
import edu.umass.cs.mallet.base.pipe.TokenSequence2FeatureVectorSequence;
import edu.umass.cs.mallet.base.pipe.tsf.LexiconMembership;
import edu.umass.cs.mallet.base.pipe.tsf.OffsetConjunctions;
import edu.umass.cs.mallet.base.pipe.tsf.RegexMatches;
import edu.umass.cs.mallet.base.pipe.tsf.TokenText;
import edu.umass.cs.mallet.base.pipe.tsf.TrieLexiconMembership;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * Use this TUI to serialize a feature pipe, which can be shared among processes later
 *   
 *   
 * Created on Sep 29, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */
public class SerializeFeaturePipesTUI
{
    private static final Logger logger = MalletLogger.getLogger (SerializeFeaturePipesTUI.class.getName ());

    private static String CAPS = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ]";
    private static String ALPHA = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü]";
    private static String ALPHANUM = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü0-9]";
    private static String PUNT = "[,\\.;:?!()]";

    private static int[][] offsetsOption = new int[][] {{-5}, {-4}, {-3}, {-2}, {-1}, {1}, {2}, {3}, {4}, {5}, {0,0}, {-1,0}, {0,1}};

    private static CommandOption.File outputPrefix = new CommandOption.File
        (SerializeFeaturePipesTUI.class, "output-prefix", null, true, null,
         "Directory into which to put logs and the serialized pipes.", "");

    // Feature options

    private static CommandOption.File resourceDirectory = new CommandOption.File
        (SerializeFeaturePipesTUI.class, "resource-directory", null, true, null,
         "Directory where lexicons, etc., located.", null);

    private static CommandOption.Boolean useNerLexicons = new CommandOption.Boolean
        (SerializeFeaturePipesTUI.class, "use-ner-lexicons", null, true, true,
         "Use named-entity lexicons.", null);

    private static CommandOption.Boolean useDblpLexicons = new CommandOption.Boolean
        (SerializeFeaturePipesTUI.class, "use-dblp-lexicons", null, true, true,
         "Use lexicons generated from DBLP.", null);

    static CommandOption.Object featOffsetOptions = new CommandOption.Object
    (SerializeFeaturePipesTUI.class, "feature-offset-options", "Constructor for feature/feature conjunction positions",
     true, offsetsOption,
     "Java code for the int[][] constructor used to create the feature set of each position", null)
    {
		public void parseArg (java.lang.String arg) 
		{
			super.parseArg(arg);
		}
		public void postParsing (CommandOption.List list) {
			assert (this.value instanceof int[][]);
			offsetsOption = (int[][]) this.value;
			System.out.print("feature-offset-options= ");
			for (int i = 0; i < offsetsOption.length; i++) {
				System.out.print("[");
				for (int j = 0; j < offsetsOption[i].length; j++)
					System.out.print(offsetsOption[i][j] + ",");
				System.out.print("], ");
			}
			System.out.println();
		}
    };

    public static void main (String args[]) throws IOException
    {
    	CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
    	options.add (SerializeFeaturePipesTUI.class);
    	options.process (args);
    	options.logOptions (logger);
    	
    	System.setProperty ("java.util.logging.config.file", "config/logging.properties");
    	
   		Pipe nerLexPipe = useNerLexicons.value ?
    			(Pipe) new SerialPipes (new Pipe[] {
    					new LexiconMembership ("FIRSTHIGHEST", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prfirsthighest"), true),
    					new LexiconMembership ("FIRSTHIGH", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prfirsthigh"), true),
    					new LexiconMembership ("FIRSTMED", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prfirstmed"), true),
    					new LexiconMembership ("FIRSTLOW", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prfirstlow"), true),
    					new LexiconMembership ("LASTHIGHEST", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prlasthighest"), true),
    					new LexiconMembership ("LASTHIGH", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prlasthigh"), true),
    					new LexiconMembership ("LASTMED", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prlastmed"), true),
    					new LexiconMembership ("LASTLOW", new File(resourceDirectory.value, "ner/conllDict/personname/ssdi.prlastlow"), true),
    					new LexiconMembership ("HONORIFIC", new File(resourceDirectory.value, "ner/conllDict/personname/honorifics"), true),
    					new LexiconMembership ("NAMESUFFIX", new File(resourceDirectory.value, "ner/conllDict/personname/namesuffixes"), true),
    					new LexiconMembership ("NAMEPARTICLE", new File(resourceDirectory.value, "ner/conllDict/personname/name-particles"), true),
    					new TrieLexiconMembership ("NAMEPARTICLEPHRASE", new File(resourceDirectory.value, "ner/conllDict/personname/name-particle-phrases"), true),
    					new LexiconMembership ("DAY", new File(resourceDirectory.value, "ner/conllDict/days"), true),
    					new LexiconMembership ("MONTH", new File(resourceDirectory.value, "ner/conllDict/months"), true),
    					new LexiconMembership ("PLACESUFFIX", new File(resourceDirectory.value, "ner/conllDict/place-suffixes"), true),
    					new TrieLexiconMembership ("COUNTRY", new File(resourceDirectory.value, "ner/conllDict/countries"), true),
    					new TrieLexiconMembership ("USSTATE", new File(resourceDirectory.value, "ner/conllDict/US-states"), true),
    					new TrieLexiconMembership ("CONTINENT", new File(resourceDirectory.value, "ner/conllDict/continents"), true),
    					new LexiconMembership ("STOPWORD", new File(resourceDirectory.value, "ner/conllDict/stopwords"), true),
    					new TrieLexiconMembership (new File(resourceDirectory.value,"ner/conllDict/utexas/UNIVERSITIES")),
    					new LexiconMembership ("STREETHIGH", new File(resourceDirectory.value,"casutton/streets_high"), true),
    					new LexiconMembership ("STREETALL", new File(resourceDirectory.value,"casutton/streets_all"), true),
    					new LexiconMembership ("ROOM", new File(resourceDirectory.value,"casutton/rooms"), true),
    					new LexiconMembership ("USSTATEABBR", new File(resourceDirectory.value,"rexa/state_abbreviations"), true),
    			})
    			:
   				new Noop ();
    			
    	Pipe dblpLexPipe = useDblpLexicons.value ?
    			(Pipe) new SerialPipes (new Pipe[] {
    					new LexiconMembership ("DBLPTITLESTARTHIGH", new File (resourceDirectory.value, "rexa/title.start.high"), false),
    					new LexiconMembership ("DBLPTITLESTARTMED", new File (resourceDirectory.value, "rexa/title.start.med"), false),
//    					new LexiconMembership ("DBLPTITLEEND", new File (resourceDirectory.value, "rexa/title-end"), true),
    					new LexiconMembership ("DBLPTITLEHIGH", new File (resourceDirectory.value, "rexa/title.high"), true),
    					new LexiconMembership ("DBLPTITLEMED", new File (resourceDirectory.value, "rexa/title.med"), true),
    					new LexiconMembership ("DBLPTITLELOW", new File (resourceDirectory.value, "rexa/title.low"), true),
    					new LexiconMembership ("DBLPAUTHORFIRST", new File (resourceDirectory.value, "rexa/author-first"), false),
    					new LexiconMembership ("DBLPAUTHORMIDDLE", new File (resourceDirectory.value, "rexa/author-middle"), false),
    					new LexiconMembership ("DBLPAUTHORLAST", new File (resourceDirectory.value, "rexa/author-last"), false),
    					new LexiconMembership ("DBLPPUBLISHER", new File (resourceDirectory.value, "rexa/publisher"), false),
    					new TrieLexiconMembership ("CONFABBR", new File (resourceDirectory.value, "rexa/conferences.abbr"), false),
    					new TrieLexiconMembership ("CONFFULL", new File (resourceDirectory.value, "rexa/conferences.full"), true),
    					new TrieLexiconMembership ("JOURNAL", new File (resourceDirectory.value, "rexa/journals"), true),
    					new TrieLexiconMembership ("THESIS", new File (resourceDirectory.value, "rexa/thesis"), true)
    			})
    			:
   				new Noop ();
    					
    	offsetsOption = (int[][]) featOffsetOptions.value;
    					
		Pipe featurePipe = new SerialPipes (new Pipe[] {
				//      new PrintInputAndTarget (),
				new NewHtmlTokenization2TokenSequence (true, true, true, false),
				new RegexMatches ("INITCAP", Pattern.compile (CAPS + ".*")),
				new RegexMatches ("ALLCAPS", Pattern.compile (CAPS + "+")),
				new RegexMatches ("CAPTIALIZED", Pattern.compile (CAPS + "[a-z]+")),
				new RegexMatches ("CONTAINSDIGITS", Pattern.compile (".*[0-9].*")),
				new RegexMatches ("ALLDIGITS", Pattern.compile ("[0-9]+")),
				new RegexMatches ("PHONEORZIP", Pattern.compile ("[0-9]+-[0-9]+")), 
				new RegexMatches ("USZIP", Pattern.compile ("[0-9][0-9][0-9][0-9][0-9](?:-[0-9][0-9][0-9][0-9])?")),
				new RegexMatches ("USPHONE", Pattern.compile ("[0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]")),
				new RegexMatches ("CONTAINSDOTS", Pattern.compile ("[^\\.]*\\..*")), 
				new RegexMatches ("CONTAINSDASH", Pattern.compile (ALPHANUM + "+-" + ALPHANUM + "*")), 
				new RegexMatches ("ACRO", Pattern.compile ("[A-Z][A-Z\\.]*\\.[A-Z\\.]*")), 
				new RegexMatches ("LONELYINITIAL", Pattern.compile (CAPS + "\\.")), 
				new RegexMatches ("SINGLECHAR", Pattern.compile (ALPHA)),
				new RegexMatches ("CAPLETTER", Pattern.compile ("[A-Z]")),
				new RegexMatches ("PUNC", Pattern.compile (PUNT)),
				new RegexMatches ("URL", Pattern.compile ("www\\..*|http://.*|ftp\\..*")),
				new RegexMatches ("EMAIL", Pattern.compile ("\\S+@\\S+|e-mail.*|email.*|Email.*")), 
				new RegexMatches ("YEAR", Pattern.compile ("(?:19|20)\\d\\d")),
				nerLexPipe,
				dblpLexPipe,
				new TokenText ("WORD="),
				new OffsetConjunctions (offsetsOption),
				new Target2LabelSequence (),
				new TokenSequence2FeatureVectorSequence (true, true),
				//new DebugPipe(),
				//new PrintInputAndTarget (),
		});
		
		printFeaturePipe(featurePipe);
		
		// serialize the pipe
		File of = new File(outputPrefix.value, "featurePipes.dat");
		FileOutputStream fos = new FileOutputStream(of);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(featurePipe);
		oos.close();
		
		System.out.println("DONE");
    }


//  print out the given pipe, just for debugging purpose
    private static void printFeaturePipe(Pipe featurePipe)
    { 
    	ArrayList pipes = ((SerialPipes) featurePipe).getPipes ();
    	System.out.println ("feature pipes");
    	for (int i = 0; i < pipes.size (); i++) {
    		System.out.print ("Pipe: " + i + ": ");
    		Pipe tempP = (Pipe) pipes.get (i);
    		if (tempP == null) {
    			System.out.println ("Pipe is null");
    		} else {
    			String pipeName = tempP.getClass ().getName ();
    			System.out.println (pipeName);
    			if (tempP instanceof SerialPipes) {
    				ArrayList pipes2 = ((SerialPipes) tempP).getPipes ();
    				
    				for (int j = 0; j < pipes2.size (); j++) {
    					System.out.print ("	Pipe: " + j + ": ");
    					Pipe tempP2 = (Pipe) pipes2.get (j);
    					if (tempP2 == null) {
    						System.out.println ("	Pipe is null");
    					} else {
    						String pipeName2 = tempP2.getClass ().getName ();
    						System.out.println (pipeName2);
    					}
    				}
    			}
    		}
    	}
    }
    
}