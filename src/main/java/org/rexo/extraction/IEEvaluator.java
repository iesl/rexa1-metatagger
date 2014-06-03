/**
 Fuchun Peng <a href="mailto:fuchun@cs.umass.edu">fuchun@cs.umass.edu</a>, July, 2003
 */

package org.rexo.extraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import edu.umass.cs.mallet.base.fst.CRF;
import edu.umass.cs.mallet.base.fst.CRF3;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.fst.Transducer;
import edu.umass.cs.mallet.base.fst.TransducerEvaluator;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.TokenSequence;

public class IEEvaluator extends TransducerEvaluator {
  private static Logger logger = Logger.getLogger( IEEvaluator.class.getName() );

  boolean alwaysEvaluateWhenFinished = true;
  boolean printCrfAtEnd = false;

  boolean checkpointTransducer = false;
  String checkpointFilePrefix = null;
  int checkpointIterationsToSkip = 30;

  String viterbiOutputFilePrefix = null;
  int viterbiOutputIterationsToWait = 10;
  int viterbiOutputIterationsToSkip = 10;
  String viterbiOutputEncoding = null;
  private File outputPrefix = null;
  private PrintStream finalStr;
  private PrintStream overallStr;
  private PrintStream out;
  private int fnum = 0;


  public IEEvaluator() {
    this (null, null, 10, 0);
  }

  public IEEvaluator(String viterbiOutputFilePrefix, int viterbiOutputIterationsToSkip) {
    this.viterbiOutputFilePrefix = viterbiOutputFilePrefix;
    this.viterbiOutputIterationsToSkip = viterbiOutputIterationsToSkip;
  }


  public IEEvaluator(File outputPrefix, String viterbiOutputFilePrefix, int viterbiOutputIterationsToSkip, int fnum) {
    this.viterbiOutputFilePrefix = viterbiOutputFilePrefix;
    this.viterbiOutputIterationsToSkip = viterbiOutputIterationsToSkip;
    this.outputPrefix = outputPrefix;
    this.fnum = fnum;
    intializeOutStream();
    try {
      this.finalStr = new PrintStream (new FileOutputStream (new File (outputPrefix, "final-by-field.log"), true));
      this.overallStr = new PrintStream (new FileOutputStream (new File (outputPrefix, "overall-f1.log"), true));
    } catch (FileNotFoundException e) {
      e.printStackTrace ();
    }
  }


  private void intializeOutStream ()
  {
    File f = new File (outputPrefix, "all-prf1-"+fnum+".log");
    try {
      out = new PrintStream (new FileOutputStream (f, true));
    } catch (FileNotFoundException e) {
      e.printStackTrace ();
    }
  }


  public void test(Transducer transducer, InstanceList data,
                   String description, PrintStream viterbiOutputStream) {
  }

  private PrintStream makeOutputFile (String name, int iteration) {
    try {
      File file = new File (outputPrefix, name+iteration+".log");
      return new PrintStream (new FileOutputStream (file));
    } catch (IOException e) {
      throw new RuntimeException (e);
    }
  }

  public boolean evaluate(Transducer crf, boolean finishedTraining, int iteration,
                          boolean converged, double cost,
                          InstanceList training, InstanceList validation, InstanceList testing) {

    if (!shouldDoEvaluate (iteration, finishedTraining) || (training == null && validation == null && testing == null)) {
      return true;
    }

    out.println( "*********************************************" );
    out.println( "Iteration=" + iteration + " Cost=" + cost );

    // Possibly write CRF to a file
    if (checkpointTransducer && iteration > 0
        && iteration % checkpointIterationsToSkip == 0) {
      String checkFilename = checkpointFilePrefix == null ? "" : checkpointFilePrefix + '.';
      checkFilename = checkFilename + "checkpoint" + iteration + ".crf";

      if (crf instanceof CRF) {
        ((CRF)crf).write( new File( checkFilename ) );
      }
      else if (crf instanceof CRF3) {
        ((CRF3)crf).write( new File( checkFilename ) );
      }

    }

    int numCorrectTokens, totalTokens;
    int[] numTrueSegments, numPredictedSegments, numCorrectSegments;
    int[] numCorrectSegmentsInVocabulary, numCorrectSegmentsOOV;
    int[] numIncorrectSegmentsInVocabulary, numIncorrectSegmentsOOV;
    int[][] matrixEntry;

    InstanceList[] lists = new InstanceList[]{training, validation, testing};
    String[] listnames = new String[]{"Training", "Validation", "Testing"};
    TokenSequence sourceTokenSequence = null;

	// Serialize the (partially) trained transducer
	File tempCRF = new File(outputPrefix, "tempCRF");
	logger.info("Serializing (partially) trained CRF to " + tempCRF);
    if (crf instanceof CRF) {
      ((CRF)crf).write(tempCRF);
    }
    else if (crf instanceof CRF3) {
      ((CRF3)crf).write(tempCRF);
    }
    else if (crf instanceof CRF4) {
        ((CRF4)crf).write(tempCRF);
      }
	else if  (crf instanceof RexaCRF) {
      ((RexaCRF)crf).write(tempCRF);
    }

    PrintStream viterbiOutputStream = null;
    if ((iteration >= viterbiOutputIterationsToWait && iteration % viterbiOutputIterationsToSkip == 0)
        || (alwaysEvaluateWhenFinished && finishedTraining)
    ) {
      if (viterbiOutputFilePrefix == null) {
        viterbiOutputStream = System.out;
      }
      else {
        String viterbiFilename = null;
        viterbiFilename = viterbiOutputFilePrefix + ".viterbi_" + iteration;
        File viterbiFile = (outputPrefix == null) ? new File (viterbiFilename) : new File (outputPrefix, viterbiFilename);
        try {
          FileOutputStream fos = new FileOutputStream( viterbiFile );
          if (viterbiOutputEncoding == null) {
            viterbiOutputStream = new PrintStream( fos );
          }
          else {
            viterbiOutputStream = new PrintStream( fos, true, viterbiOutputEncoding );
          }
        }
        catch (IOException e) {
          logger.warning(
              "Couldn't open Viterbi output file '" + viterbiFilename + "'; continuing without Viterbi output trace." );
          viterbiOutputStream = null;
        }
      }
    }

    // find out the vocabulary of targets
    //		Pipe p = lists[0].getInstance(0).getInstancePipe();
    Pipe p = crf.getInputPipe();
    Alphabet targets = p.getTargetAlphabet();
    assert(targets != null);

    numTrueSegments = new int[targets.size()];
    numPredictedSegments = new int[targets.size()];
    numCorrectSegments = new int[targets.size()];
    numCorrectSegmentsInVocabulary = new int[targets.size()];
    numCorrectSegmentsOOV = new int[targets.size()];
    numIncorrectSegmentsInVocabulary = new int[targets.size()];
    numIncorrectSegmentsOOV = new int[targets.size()];
    matrixEntry = new int[targets.size()][targets.size()];

    for (int k = 0; k < lists.length; k++) {
      if (lists[k] == null) {
        continue;
      }

      //initialize the variables
      totalTokens = numCorrectTokens = 0;
      for (int t = 0; t < targets.size(); t++) {
        numTrueSegments[t] = numPredictedSegments[t] = numCorrectSegments[t] = 0;
        numCorrectSegmentsInVocabulary[t] = numCorrectSegmentsOOV[t] = 0;
        numIncorrectSegmentsInVocabulary[t] = numIncorrectSegmentsOOV[t] = 0;

        for (int tt = 0; tt < targets.size(); tt++) {
          matrixEntry[t][tt] = 0;
        }
      }

      int numCorrectInstances = 0;
      for (int i = 0; i < lists[k].size(); i++) {
        if (viterbiOutputStream != null) {
          viterbiOutputStream.println( "Viterbi path for " + listnames[k] + " instance #" + i );
        }
        Instance instance = lists[k].getInstance( i );
        Sequence input = (Sequence)instance.getData();
        
        //String tokens = null;
        //if (instance.getSource() != null)
        //tokens = (String) instance.getSource().toString();
        Sequence trueOutput = (Sequence)instance.getTarget();
        assert (input.size() == trueOutput.size());
        Sequence predOutput = crf.viterbiPath( input ).output();
        assert (predOutput.size() == trueOutput.size());

        sourceTokenSequence = (TokenSequence)instance.getSource();
        boolean instanceCorrect = true;

        for (int j = 0; j < trueOutput.size(); j++) {

          Object predO = predOutput.get( j );
          Object trueO = trueOutput.get( j );
          int predIndex = targets.lookupIndex( predO );
          int trueIndex = targets.lookupIndex( trueO );

          totalTokens++;
          numTrueSegments[trueIndex]++;
          numPredictedSegments[predIndex]++;

          matrixEntry[trueIndex][predIndex]++;

          if (predIndex == trueIndex) {
            numCorrectTokens++;
            numCorrectSegments[trueIndex]++;
          }
          else {
            // Here is an incorrect prediction, find out if the word is in the lexicon
            //						String sb = sourceTokenSequence.getToken(j).getText();

            //                                              if (HashFile.allLexicons.contains(sb) )
            //                              	                  numIncorrectSegmentsInVocabulary[trueIndex]++;
            //              	                        else
            //                      	                          numIncorrectSegmentsOOV[trueIndex]++;
            instanceCorrect = false;
          }



          if (viterbiOutputStream != null) {
            FeatureVector fv = (FeatureVector)input.get( j );
            //viterbiOutputStream.println (tokens.charAt(j)+" "+trueOutput.get(j).toString()+
            //'/'+predOutput.get(j).toString()+"  "+ fv.toString(true));
            if (sourceTokenSequence != null) {
              viterbiOutputStream.print( sourceTokenSequence.getToken( j ).getText() + ": " );
            }
            viterbiOutputStream.println(
                trueOutput.get( j ).toString() + '/' + predOutput.get( j ).toString() + "  " + fv.toString( true ) );
          }
        }

        if (instanceCorrect) numCorrectInstances++;
      }

      double accuracy = (double)numCorrectTokens / totalTokens;
      out.println(
          "\n" + listnames[k] + " accuracy=" + numCorrectTokens + "/" + totalTokens + " = " + accuracy + "\n" );
      double wholeInstanceAccuracy = numCorrectInstances / (double) lists[k].size();
      out.println(
           "\n" + listnames[k] + " whole instance accuracy=" + numCorrectInstances + "/" + lists[k].size() + " = " + wholeInstanceAccuracy + "\n" );

        DecimalFormat f = new DecimalFormat ("0.####");
        int totalCorrect = 0, totalPred = 0, totalSegs = 0;
        for (int t = 0; t < targets.size(); t++) {
          double precision = numPredictedSegments[t] == 0
                             ? 1
                             : ((double)numCorrectSegments[t]) / numPredictedSegments[t];
          double recall = numTrueSegments[t] == 0 ? 1 : ((double)numCorrectSegments[t]) / numTrueSegments[t];
          double f1 = recall + precision == 0.0 ? 0.0 : (2.0 * recall * precision) / (recall + precision);
          totalCorrect += numCorrectSegments[t]; totalPred += numPredictedSegments[t] ; totalSegs += numTrueSegments[t];
          out.println( targets.lookupObject( t ) + " precision=" + f.format(precision) + " recall=" + f.format(recall) + " f1=" + f.format(f1) );
          out.println(
              "segments true=" + numTrueSegments[t] + " pred=" + numPredictedSegments[t] + " correct=" + numCorrectSegments[t] + " misses=" + (numTrueSegments[t] - numCorrectSegments[t]) + " alarms=" + (numPredictedSegments[t] - numCorrectSegments[t]) + "\n" );

          //				System.out.println ("correct segments OOV="+numCorrectSegmentsOOV[t]+" IV="+numCorrectSegmentsInVocabulary[t]);
          //				System.out.println ("incorrect segments OOV="+numIncorrectSegmentsOOV[t]+" IV="+numIncorrectSegmentsInVocabulary[t]);

          if (finishedTraining) {
            finalStr.print (" "+f.format(precision)+" "+f.format(recall)+" "+f.format(f1));
          }
        }

      if (finishedTraining)
        finalStr.println();

        // overall results
        double P = totalCorrect / ((double)totalPred);
        double R = totalCorrect / ((double)totalSegs);
        double F1 = (2 * P * R) / (P + R);
        out.println (listnames[k]+" OVERALL precision="+f.format(P)+"  recall="+f.format(R)+"   f1="+f.format(F1));
        out.println ("  segments true="+totalSegs+"   predicted="+totalPred+"     correct="+totalCorrect);
        out.flush ();

      if (finishedTraining)
        overallStr.println (f.format(P)+"\t"+f.format(R)+"\t"+f.format(F1));

        printConfusion (targets, matrixEntry, iteration);


    }

    if (viterbiOutputStream != null && viterbiOutputFilePrefix != null && viterbiOutputStream != System.out) {
      viterbiOutputStream.close();
    }

    if (finishedTraining) {
      finalStr.close();
      overallStr.close();
      out.close();
    }

    if (printCrfAtEnd && finishedTraining) {
      if (crf instanceof CRF) {
        ((CRF)crf).print();
      }
      else if (crf instanceof CRF3) {
        ((CRF3)crf).print();
      }
    }

    return true;
  }


  private void printConfusion (Alphabet targets, int[][] matrixEntry, int iteration)
  {
    PrintStream out = makeOutputFile ("confusion", iteration);
    out.println ("********** At iteration "+iteration);
    out.println( "\n Confusion Matrix (row: true label, col: predicted label)" );
    out.print( "\t" );
    for (int t = 0; t < targets.size(); t++) {
      out.print( targets.lookupObject( t ) + "\t" );
    }
    out.println();

    for (int t = 0; t < targets.size(); t++) {
      out.print( targets.lookupObject( t ) + "\t" );
      for (int tt = 0; tt < targets.size(); tt++) {
        out.print( matrixEntry[t][tt] + "\t" );
      }
      out.println();
    }
  }

}
