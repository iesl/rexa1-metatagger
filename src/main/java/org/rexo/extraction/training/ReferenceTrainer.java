/**
 * Created on Feb 26, 2004
 * <p/>
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * author: asaunders
 */

package org.rexo.extraction.training;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.umass.cs.mallet.base.pipe.tsf.OffsetConjunctions;
import edu.umass.cs.mallet.base.pipe.tsf.RegexMatches;
import edu.umass.cs.mallet.base.pipe.tsf.TokenText;
import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;
import org.rexo.extraction.IEEvaluator;
import org.rexo.extraction.XML2TokenSequence;
import org.rexo.extraction.LineBreakPipe;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ReferenceTrainer {
  private static String CAPS = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ]";
  private static String ALPHA = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü]";
  private static String ALPHANUM = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜa-zàèìòùáéíóúçñïü0-9]";
  private static String PUNT = "[,\\.;:?!()]";

//  private static int[][] _offsetsOption = new int[][]{{-1}, {1}};
  private static int[][] _offsetsOption = new int[][]{{-2}, {-1}, {1}, {2}};

  public static final int TRAIN_HEADERS = 0;
  public static final int TRAIN_REFERENCES = 1;

  public CRF4 train(Reader trainingDataReader, String recordSeparator, int trainingType) throws IOException {

    System.setProperty( "java.util.logging.config.file", "config/logging.properties" );

    Pipe lineBreakPipe;
    switch (trainingType) {
      case TRAIN_HEADERS:
        lineBreakPipe = new LineBreakPipe ();
        break;

      case TRAIN_REFERENCES:
        lineBreakPipe = new Noop ();
        break;

      default:
        throw new IllegalArgumentException ("Unknown training type "+trainingType);
    }

    Pipe p = new SerialPipes( new Pipe[]{
      new Input2CharSequence(),
      new XML2TokenSequence(
          new CharSequenceLexer(
              Pattern.compile( "\\n|\\S+@\\S+|\\w+-\\w+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}" ) ),
          "O" ),
//      new PrintInputAndTarget (),
      new RegexMatches( "INITCAP", Pattern.compile( CAPS + ".*" ) ),
      new RegexMatches( "ALLCAPS", Pattern.compile( CAPS + "+" ) ),
      new RegexMatches( "CONTAINSDIGITS", Pattern.compile( ".*[0-9].*" ) ),
      new RegexMatches( "ALLDIGITS", Pattern.compile( "[0-9]+" ) ),
      new RegexMatches( "PHONEORZIP", Pattern.compile( "[0-9]+-[0-9]+" ) ), //added
      new RegexMatches( "CONTAINSDOTS", Pattern.compile( "[^\\.]*\\..*" ) ), //added
      new RegexMatches( "CONTAINSDASH", Pattern.compile( ALPHANUM + "+-" + ALPHANUM + "*" ) ), //added
      new RegexMatches( "ACRO", Pattern.compile( "[A-Z][A-Z\\.]*\\.[A-Z\\.]*" ) ), //added
      new RegexMatches( "LONELYINITIAL", Pattern.compile( CAPS + "\\." ) ), //added
      new RegexMatches( "SINGLECHAR", Pattern.compile( ALPHA ) ),
      new RegexMatches( "CAPLETTER", Pattern.compile( "[A-Z]" ) ),
      new RegexMatches( "PUNC", Pattern.compile( PUNT ) ),
      new RegexMatches( "URL", Pattern.compile( "www\\..*|http://.*|ftp\\..*" ) ),
      new RegexMatches( "EMAIL", Pattern.compile( "\\S+@\\S+|e-mail.*|email.*|Email.*" ) ), //added
      new TokenText( "WORD=" ),
      new OffsetConjunctions( _offsetsOption ),
      lineBreakPipe,
      new Target2LabelSequence(),
      new TokenSequence2FeatureVectorSequence( true, true ),
//      new PrintInputAndTarget (),
    } );


    { // BOOKMARK print out the read-in pipes, just for debugging purpose FOLD=true
      ArrayList pipes = ((SerialPipes)p).getPipes();
      System.out.println( "pipes" );
      for (int i = 0; i < pipes.size(); i++) {
        System.out.print( "Pipe: " + i + ": " );
        Pipe tempP = (Pipe)pipes.get( i );
        if (tempP == null) {
          System.out.println( "Pipe is null" );
        }
        else {
          String pipeName = tempP.getClass().getName();
          System.out.println( pipeName );
          if (tempP instanceof SerialPipes) {
            ArrayList pipes2 = ((SerialPipes)tempP).getPipes();

            for (int j = 0; j < pipes2.size(); j++) {
              System.out.print( "	Pipe: " + j + ": " );
              Pipe tempP2 = (Pipe)pipes2.get( j );
              if (tempP2 == null) {
                System.out.println( "	Pipe is null" );
              }
              else {
                String pipeName2 = tempP2.getClass().getName();
                System.out.println( pipeName2 );
              }
            }
          }
        }
      }
    }

    InstanceList allTrainingData = new InstanceList( p );

    allTrainingData.add( new LineGroupIterator( trainingDataReader, Pattern.compile( recordSeparator ), true ) );


    // TODO parameterize these data fields
    double trainingProportionOption = Double.parseDouble(
        System.getProperty( "rexo.crf.training.data.training.fraction", "0.7" ) );
    double testingProportionOption = Double.parseDouble(
        System.getProperty( "rexo.crf.training.data.testing.fraction", "0.3" ) );

    // Proportions below is: {training, testing, ignore}
    InstanceList[] trainingLists = allTrainingData.split(
        new double[]{trainingProportionOption,
                     testingProportionOption,
                     1 - trainingProportionOption - testingProportionOption} );

    InstanceList trainingData = trainingLists[0];
    InstanceList testingData = trainingLists[1];
    InstanceList validationData = trainingLists[2];


    System.out.println( "Fuchun: " + p.getTargetAlphabet() );
    // Print out all the target names
    Alphabet targets = p.getTargetAlphabet();

    assert(targets != null);
    System.out.println( "target size: " + targets.size() );

    System.out.print( "State labels:" );
    for (int i = 0; i < targets.size(); i++) {
      System.out.print( " " + targets.lookupObject( i ) );
    }


    // Print out some feature information
    System.out.println( "Number of features = " + p.getDataAlphabet().size() );

    CRF4 crf = new CRF4( p, null );
    System.out.println( "crf is : " + (crf.getClass()).getName() );

    crf.addStatesForLabelsConnectedAsIn( allTrainingData );

    String viterbiFilePrefix = "viterbi";
    int skipNum = 30;
    // int trainingIterations = 99999; // effectively infinite
    int trainingIterations = Integer.parseInt( System.getProperty( "rexo.crf.training.iterations", "99999" ) );
    crf.train( trainingData, validationData, testingData,
               new IEEvaluator( viterbiFilePrefix, skipNum ),
               trainingIterations );

    return crf;
  }

  public Reader printOut(InstanceList list, String tag) {
    PipedWriter pipeOut = new PipedWriter();
    PipedReader pipeIn = null;
    try {
      pipeIn = new PipedReader( pipeOut );
    }
    catch (IOException e) {
      // TODO handle exception
      e.printStackTrace();
    }
    PrintWriter printWriter = new PrintWriter( pipeOut );

    TokenSequence sourceTokenSequence = null;

    for (int i = 0; i < list.size(); i++) {
      printWriter.println( "### " + tag + " instance #" + i );
      Instance instance = list.getInstance( i );
      Sequence trueOutput = (Sequence)instance.getTarget();

      sourceTokenSequence = (TokenSequence)instance.getSource();
      for (int j = 0; j < trueOutput.size(); j++) {
        if (printWriter != null && sourceTokenSequence != null) {
          printWriter.println( sourceTokenSequence.getToken( j ).getText() + " :: "
                               + "S_" + trueOutput.get( j ).toString() );
        }
      }
    }

    return pipeIn;
  }

  public Reader printOut2(InstanceList list, String tag, Alphabet targets) {

    PipedWriter pipeOut = new PipedWriter();
    PipedReader pipeIn = null;
    try {
      pipeIn = new PipedReader( pipeOut );
    }
    catch (IOException e) {
      // TODO handle exception
      e.printStackTrace();
    }

    PrintWriter[] singleStateStream = new PrintWriter[targets.size()];
    for (int i = 0; i < targets.size(); i++) {
      singleStateStream[i] = new PrintWriter( pipeOut );
      singleStateStream[i].println( tag + "_" + targets.lookupObject( i ) );
    }

    TokenSequence sourceTokenSequence = null;

    for (int i = 0; i < list.size(); i++) {
      Instance instance = list.getInstance( i );
      Sequence trueOutput = (Sequence)instance.getTarget();

      sourceTokenSequence = (TokenSequence)instance.getSource();
      for (int j = 0; j < trueOutput.size(); j++) {
        singleStateStream[i].print( "S_" + trueOutput.get( j ).toString() + " " );

        int index = targets.lookupIndex( trueOutput.get( j ) );
        if (singleStateStream[index] != null && sourceTokenSequence != null) {
          singleStateStream[index].print( sourceTokenSequence.getToken( j ).getText() + " " );
        }
      }

      for (int j = 0; j < targets.size(); j++) {
        singleStateStream[j].println();
      }
    }

    for (int i = 0; i < targets.size(); i++) {
      singleStateStream[i].close();
    }
    return pipeIn;
  }

}



