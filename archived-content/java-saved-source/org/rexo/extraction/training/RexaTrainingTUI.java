/**
 * Created on Feb 26, 2004
 * <p/>
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * author: asaunders
 */

package org.rexo.extraction.training;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.fst.MEMM;
import edu.umass.cs.mallet.base.fst.TokenAccuracyEvaluator;
import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.umass.cs.mallet.base.pipe.iterator.InstanceListIterator;
import edu.umass.cs.mallet.base.pipe.tsf.*;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;
import edu.umass.cs.mallet.base.extract.*;
import org.rexo.extraction.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A CRF trainer that runs on the new style Rexa data and saved
 * an Extractor Object.
 * <p/>
 * This version assumes that headers and references have been partitioned
 * into separate files.
 */
public class RexaTrainingTUI {

  private static final Logger logger = MalletLogger.getLogger (RexaTrainingTUI.class.getName ());

  private static String CAPS = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ]";
  private static String ALPHA = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü]";
  private static String ALPHANUM = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü0-9]";
  private static String PUNT = "[,\\.;:?!()]";

//  private static int[][] _offsetsOption = new int[][]{{-1}, {1}};
  private static int[][] _offsetsOption = new int[][]{{-2}, {-1}, {1}, {2}};

  public static final int TRAIN_HEADERS = 0;
  public static final int TRAIN_REFERENCES = 1;

  private static CommandOption.File outputPrefix = new CommandOption.File
          (RexaTrainingTUI.class, "output-prefix", null, true, null,
                  "Directory for output files..", "");

  private static CommandOption.File dataFile = new CommandOption.File
          (RexaTrainingTUI.class, "data-file", null, true, null,
                  "File to read training data from.", "");

  private static CommandOption.Integer trainingType = new CommandOption.Integer
          (RexaTrainingTUI.class, "training-type", null, true, TRAIN_HEADERS,
                  "0 == train headers, 1 == train references", "");

  private static CommandOption.Integer numReps = new CommandOption.Integer
          (RexaTrainingTUI.class, "num-reps", null, true, 1,
                  "Number of random test / training splits", "");

  private static CommandOption.Integer randomSeed = new CommandOption.Integer
          (RexaTrainingTUI.class, "random-seed", null, true, 1,
                  "Seed for random test / training splits", "");

  private static CommandOption.Double trainingPct = new CommandOption.Double
          (RexaTrainingTUI.class, "training-pct", null, true, 0.5,
                  "Percentage of data to use for training (rest is testing)", "");
  
  private static CommandOption.Integer modelType = new CommandOption.Integer
  (RexaTrainingTUI.class, "model-type", null, true, 0,
          "0 ==> 1st order, 1 ==> 1st order + zero order, 2 ==> 0+1+default2, 3 ==> 0+1+2", null);

  // Feature options

  private static CommandOption.File resourceDirectory = new CommandOption.File
  (RexaTrainingTUI.class, "resource-directory", null, true, null,
          "Directory where lexicons, etc., located.", null);

  private static CommandOption.Boolean usePageishPipe = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-pagish", null, true, true,
          "Use page regexps.", null);

  private static CommandOption.Boolean useCensorPipe = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-censor", null, true, false,
          "If true, remove strange targets from headers.", null);

  private static CommandOption.Boolean useNerLexicons = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-ner-lexicons", null, true, true,
          "Use named-entity lexicons.", null);

  private static CommandOption.Boolean useDblpLexicons = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-dblp-lexicons", null, true, true,
          "Use lexicons generated from DBLP.", null);
  
  private static CommandOption.Boolean useFontChangeFeatures = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-font-change", null, true, false,
          "Add a feature for whenever the font changes", null);

  private static CommandOption.Boolean useLargeFontFeatures = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-large-font", null, true, false,
          "Add a feature for all words in the largest font.", null);

  private static CommandOption.Boolean supportedOnlyOption = new CommandOption.Boolean
  (RexaTrainingTUI.class, "supported-only", null, true, true,
          "Use only the observation/transition combos that occur in training data.", null);

  private static CommandOption.Boolean useSomeSupportedOption = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-some-supported", null, true, true,
          "Use the some supported features trick.", null);

  private static CommandOption.Boolean useFeatureInductionOption = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-feature-induction", null, true, false,
          "Use feature induction.", null);

  private static CommandOption.Boolean useCrfOption = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-crf", null, true, true,
          "Use an CRF.  If false, use an MEMM.", null);

  private static CommandOption.Boolean useAuthorFSM = new CommandOption.Boolean
  (RexaTrainingTUI.class, "use-author-fsm", null, true, false,
          "Use hand-designed author FSM; not currently implemented.", null);

 public static void main (String args[]) throws IOException
  {
    CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
    options.add (RexaTrainingTUI.class);
    options.process (args);
    options.logOptions (logger);
    initOutputDirectory ();

    System.setProperty ("java.util.logging.config.file", "config/logging.properties");

    // Setup headers-specific and refs-specific features
    Pipe lineBreakPipe;
    Pipe censorPipe;
    Pipe pagishPipe;
    switch (trainingType.value) {
      case TRAIN_HEADERS:
        lineBreakPipe = new LineBreakPipe ();
        censorPipe = useCensorPipe.value ? (Pipe) new TargetCensorTokens (
                new String[] {
                  "title", "B-author", "author", "authors",
                  "institution", "address", "email", "abstract",
                  "note", "keyword", "editor", "date",
                  "tech", "phone", "web", "intro",
                }, "O")
        : new Noop();
        pagishPipe = new Noop();
        break;

      case TRAIN_REFERENCES:
        lineBreakPipe = new Noop ();
        censorPipe = new Noop ();
        pagishPipe = usePageishPipe.value ? (Pipe) new PageishPipe () : new Noop();
        break;

      default:
        throw new IllegalArgumentException ("Unknown training type " + trainingType);
    }

    Pipe tokPipe = new SerialPipes (new Pipe[]{
      new Input2CharSequence (),
      new XML2TokenSequence (new CharSequenceLexer (Pattern.compile ("\\n|&amp;|1st|2nd|3rd|[4-9]th|Ph.D.|\\S+@\\S+|\\w+-\\w+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}")),
              "O", trainingType.value == TRAIN_REFERENCES)
    });

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
//              new LexiconMembership ("DBLPTITLEEND", new File (resourceDirectory.value, "rexa/title-end"), true),
              new LexiconMembership ("DBLPTITLEHIGH", new File (resourceDirectory.value, "rexa/title.high"), true),
              new LexiconMembership ("DBLPTITLEMED", new File (resourceDirectory.value, "rexa/title.med"), true),
              new LexiconMembership ("DBLPTITLELOW", new File (resourceDirectory.value, "rexa/title.low"), true),
              new LexiconMembership ("DBLPAUTHORFIRST", new File (resourceDirectory.value, "rexa/author-first"), false),
              new LexiconMembership ("DBLPAUTHORMIDDLE", new File (resourceDirectory.value, "rexa/author-middle"), false),
              new LexiconMembership ("DBLPAUTHORLAST", new File (resourceDirectory.value, "rexa/author-last"), false),
              new LexiconMembership ("DBLPPUBLISHER", new File (resourceDirectory.value, "rexa/publisher"), false),
              new LexiconMembership ("DBLPPUBLISHER", new File (resourceDirectory.value, "rexa/publisher"), false),
              new TrieLexiconMembership ("CONFABBR", new File (resourceDirectory.value, "rexa/conferences.abbr"), false),
              new TrieLexiconMembership ("CONFFULL", new File (resourceDirectory.value, "rexa/conferences.full"), true),
              new TrieLexiconMembership ("JOURNAL", new File (resourceDirectory.value, "rexa/journals"), true),
            })
    :
            new Noop ();

    Pipe fontPipe = (useFontChangeFeatures.value) ? (Pipe) new FontChangePipe() : new Noop();
    Pipe largestFontPipe = (useLargeFontFeatures.value) ? (Pipe) new BiggestFontPipe() : new Noop();

    Pipe featurePipe = new SerialPipes (new Pipe[] {
//      new PrintInputAndTarget (),
      new RegexMatches ("INITCAP", Pattern.compile (CAPS + ".*")),
      new RegexMatches ("ALLCAPS", Pattern.compile (CAPS + "+")),
      new RegexMatches ("CAPTIALIZED", Pattern.compile (CAPS + "[a-z]+")),
      new RegexMatches ("CONTAINSDIGITS", Pattern.compile (".*[0-9].*")),
      new RegexMatches ("ALLDIGITS", Pattern.compile ("[0-9]+")),
      new RegexMatches ("PHONEORZIP", Pattern.compile ("[0-9]+-[0-9]+")), //added
      new RegexMatches ("USZIP", Pattern.compile ("[0-9][0-9][0-9][0-9][0-9](?:-[0-9][0-9][0-9][0-9])?")),
      new RegexMatches ("USPHONE", Pattern.compile ("[0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]")),
      new RegexMatches ("CONTAINSDOTS", Pattern.compile ("[^\\.]*\\..*")), //added
      new RegexMatches ("CONTAINSDASH", Pattern.compile (ALPHANUM + "+-" + ALPHANUM + "*")), //added
      new RegexMatches ("ACRO", Pattern.compile ("[A-Z][A-Z\\.]*\\.[A-Z\\.]*")), //added
      new RegexMatches ("LONELYINITIAL", Pattern.compile (CAPS + "\\.")), //added
      new RegexMatches ("SINGLECHAR", Pattern.compile (ALPHA)),
      new RegexMatches ("CAPLETTER", Pattern.compile ("[A-Z]")),
      new RegexMatches ("PUNC", Pattern.compile (PUNT)),
      new RegexMatches ("URL", Pattern.compile ("www\\..*|http://.*|ftp\\..*")),
      new RegexMatches ("EMAIL", Pattern.compile ("\\S+@\\S+|e-mail.*|email.*|Email.*")), //added
      new RegexMatches ("YEAR", Pattern.compile ("(?:19|20)\\d\\d")),
      pagishPipe,
      nerLexPipe,
      dblpLexPipe,
      fontPipe,
      largestFontPipe,
      new TokenText ("WORD="),
      lineBreakPipe,
      new OffsetConjunctions (_offsetsOption),
      censorPipe,
      new Target2LabelSequence (),
      new TokenSequence2FeatureVectorSequence (true, false), // true),
//      new PrintInputAndTarget (),
    });


    { // BOOKMARK print out the read-in pipes, just for debugging purpose FOLD=true
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



    Pipe nope = new Noop ();
    nope.setTargetProcessing (false);
    InstanceList raw = new InstanceList (nope);  // This is annoying.
    raw.add (new LineGroupIterator (new FileReader (dataFile.value),
            Pattern.compile ("</doc>"), true));

    Random rand = new Random (randomSeed.value);

    for (int rep = 0; rep < numReps.value; rep++) {


      logger.info ("Training repetition " + rep);

      // Proportions below is: {training, testing, ignore}
      InstanceList[] trainingLists = raw.split (rand,
              new double[]{trainingPct.value, 1 - trainingPct.value});

      InstanceList rawTrainingData = trainingLists[0];
      InstanceList rawTestingData = trainingLists[1];

      InstanceList toked = new InstanceList (tokPipe);
      toked.add (new InstanceListIterator (rawTrainingData));
      InstanceList trainingData = new InstanceList (featurePipe);
      trainingData.add (new InstanceListIterator (toked));

      toked = new InstanceList (tokPipe);
      toked.add (new InstanceListIterator (rawTestingData));
      InstanceList testingData = new InstanceList (featurePipe);
      testingData.add (new InstanceListIterator (toked));

      // Train the CRF

      CRF4 crf;
      if (useCrfOption.value) {
        crf = new CRF4 (featurePipe, null);
      } else {
        crf = new MEMM (featurePipe, null);
      }

      // 0 ==> 1st order, 1 ==> 1st order + zero order, 2 ==> 0+1+default2, 3 ==> 0+1+2", null)
      featurePipe.getTargetAlphabet ().lookupIndex ("<START>");
      Pattern forbidden = null;
      if (useAuthorFSM.value) forbidden = Pattern.compile (".*author.*");
      switch (modelType.value) {
        case 0:
          logger.info ("Using first-order model");
          crf.addOrderNStates (trainingData, new int[] { 1 }, null, "<START>", forbidden, null, false);
          break;

         case 1:
          logger.info ("Using first-order model + node weights");
          crf.addOrderNStates (trainingData, new int[] { 0, 1 }, null, "<START>", forbidden, null, false);
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
          crf.addOrderNStates (trainingData, new int[] { 0, 1, 2 }, null, "<START>", forbidden, null, false);
          break;

        case 4:
         logger.info ("Using default  first-order weights + node weights");
         crf.addOrderNStates (trainingData, new int[] { 0, 1 }, new boolean[] { false, true }, "<START>", forbidden, null, false);
         break;

        case 5:
         logger.info ("Using default fully-connected first-order weights + node weights");
         crf.addOrderNStates (trainingData, new int[] { 0, 1 }, new boolean[] { false, true }, "<START>", forbidden, null, true);
         break;

        case 6:
          logger.info ("Using second-order defaults (+ dense 1st- and 0th- order features)");
          crf.addOrderNStates (trainingData, new int[] { 0, 1, 2 }, new boolean[] { false, false, true },
                               "<START>", forbidden, null, false);
          break;


      }

//      if (useAuthorFSM.value)
//        addAuthorFSM (crf);

      CRF4.State start = crf.getState ("<START>");
      if (start == null) start = crf.getState ("<START>,<START>");
      crf.setAsStartState (start);

      crf.setUseSparseWeights (supportedOnlyOption.value);
      crf.print ();

      System.out.println ("Number of training instances = "+trainingData.size());
      System.out.println ("Number of testing instances = "+testingData.size());

      String viterbiFilePrefix = "viterbi";
      int skipNum = 30;
      // int trainingIterations = 99999; // effectively infinite
      int trainingIterations = Integer.parseInt (System.getProperty ("rexo.crf.training.iterations", "99999"));
      IEEvaluator eval = new IEEvaluator (outputPrefix.value, viterbiFilePrefix, skipNum, rep);
      eval.setNumIterationsToSkip (10);
      eval.setNumIterationsToWait (1);

      if (useSomeSupportedOption.value && supportedOnlyOption.value) {
        TokenAccuracyEvaluator tokenEval = new TokenAccuracyEvaluator ();
        tokenEval.setNumIterationsToSkip (5);
        crf.train (trainingData, null, testingData, tokenEval, 20);
      }

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

      // Create the extractor object and save it

      CRFExtractor extor = new CRFExtractor (crf, tokPipe);

      // Save the extractor object
      File extorFile = new File (outputPrefix.value, "extor-" + rep + ".dat");
      ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (extorFile));
      oos.writeObject (extor);
      oos.close ();

      // Do some funky error analysis stuff
      LatticeViewer.setNumFeaturesToDisplay (100);
      File errFile = new File (outputPrefix.value, "errors-r"+rep+".html");
      PrintStream errStr = new PrintStream (new FileOutputStream (errFile));
      File latticeFile = new File (outputPrefix.value, "lattice-r"+rep+".html");
      PrintStream latticeStr = new PrintStream (new FileOutputStream (latticeFile));

      Extraction results = extor.extract (new InstanceListIterator (rawTestingData));
      try {
        LatticeViewer.extraction2html (results, extor, errStr, false);
        LatticeViewer.extraction2html (results, extor, latticeStr, true);
      } catch (AssertionError e) {
        e.printStackTrace ();
      }
      errStr.close(); latticeStr.close();

      System.out.println ("*** PER-DOCUMENT F1 for rep "+rep);
      PerDocumentF1Evaluator docEval = new PerDocumentF1Evaluator ();
      docEval.evaluate (results);

      System.out.println ("*** PER-FIELD F1 for rep "+rep);
      PerFieldF1Evaluator fieldEval = new PerFieldF1Evaluator ();
      fieldEval.evaluate (results);

    }

  }

  /*
  private static void addAuthorFSMforRefs (CRF4 crf, InstanceList training)
  {
String[][] paths = {
      { "author|lfm|author-last", "author|lfm|author-first", "author|lfm|author-middle" },
      { "author|lfm|author-last", "author|lfm|author-first" },
      { "author|fml|author-first", "author|fml|author-middle", "author|fml|author-last",  },
      { "author|fml|author-first", "author|fml|author-last",  },
    };

    crf.addState ("author|lfm|author-last", 0, 0,
            new String[] { "author|lfm|author-first", "author|lfm|author-last" },
            new String[] { "author-first", "author-last" },
            new String[] { "author-first", "author-last" });
    crf.addState ("author|lfm|author-first", 0, 0,
            concat (new String[] { "author|lfm|author-middle", "author|lfm|author-first", "author|lfm|author-last", },
                    exit),
            concat (new String[] { "author-middle", "author-first", "author-last" }, exit),
            concat (new String[][] { "author-middle", "author-first", "author-last", }, exitWnames));
    crf.addState ("author|lfm|author-middle", 0, 0,
            concat (new String[] { "author|lfm|author-last", }, exit),
            concat (new String[] { "author-last" }, exit),
            concat (new String[][] { "author-last", }, exitWnames));

    crf.addState ("author|fml|author-first", 0, 0,
            new String[] { "author|fml|author-middle", "author|fml|author-last", },
            new String[] { "author-middle", "author-last" },
            new String[] { "author-middle", "author-last" });
    crf.addState ("author|fml|author-middle", 0, 0,
            new String[] { "author|fml|author-last", },
            new String[] { "author-last" },
            new String[] { "author-last" });
    crf.addState ("author|fml|author-last", 0, 0,
            concat (new String[] { "author|fml|author-last", "author|fml|author-first"}, exit),
            concat (new String[] { "author-last", "author-first", }, exit),
            concat (new String[][] { "author-last", "author-first", }, exitWNames));

    crf.addTransition ("<START>", "author|fml|author-first", "author-first", "author-first", "author-first");
    crf.addTransition ("<START>", "author|lfm|author-last", "author-last", "author-last", "author-last");
    crf.addTransition ("ref-marker", "author|fml|author-first", "author-first", "author-first", "author-first");
    crf.addTransition ("ref-marker", "author|lfm|author-last", "author-last", "author-last", "author-last");
  }
 */

  // This magic creates a log file in the outputPrefix dir
  private static void initOutputDirectory () throws IOException
  {
    Logger.getLogger ("").addHandler (new FileHandler
            (new File
                    (outputPrefix.value, "java.log").toString ()));
  }


}



