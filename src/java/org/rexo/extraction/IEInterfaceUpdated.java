/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Apr 29, 2004
 * author: asaunders
 */

package org.rexo.extraction;

import edu.umass.cs.mallet.base.fst.CRF;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;
import edu.umass.cs.mallet.base.types.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.io.*;

public class IEInterfaceUpdated {
  String seperator = "";

  private static Logger logger = Logger.getLogger( IEInterfaceUpdated.class.getName() );

  private File crfFile;
  private CRF crf = null;
  private SerialPipes pipe;
  private TokenSequence tokenSequence;
  private Sequence viterbiSequence;

  public IEInterfaceUpdated() {
    this.crfFile = null;
  }

  public IEInterfaceUpdated(File crfFile) {
    assert(crfFile != null);
    this.crfFile = crfFile;
  }

  public void setPipe(SerialPipes pipe) {
    this.pipe = pipe;
  }

  // load in CRF and its pipe from a trained crfFile
  public void loadCRF() {
    loadCRF( crfFile );
  }


  public void loadCRF(File crfFile) {

    assert(crfFile != null);

    CRF crf = null;
    try {
      ObjectInputStream ois = new ObjectInputStream( new FileInputStream( crfFile ) );
      crf = (CRF)ois.readObject();
      ois.close();
    }
    catch (IOException e) {
      System.err.println( "Exception reading crf file: " + e );
      crf = null;
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println( "Cound not find class reading in object: " + cnfe );
      crf = null;
    }


    //		crf = CRFIO.readCRF(crfFile.toString());

    if (crf == null) {
      System.err.println( "Read a null crf from file: " + crfFile );
      System.exit( 1 );
    }

    this.crf = crf;
    this.pipe = (SerialPipes)crf.getInputPipe();

    if (this.pipe == null) {
      System.err.println( "Get a null pipe from CRF" );
      System.exit( 1 );
    }


    //xxx print out the read-in pipes, just for debugging purpose
    ArrayList pipes1 = (this.pipe).getPipes();
    System.out.println( "pipes1" );
    for (int i = 0; i < pipes1.size(); i++) {
      System.out.print( "Pipe: " + i + ": " );
      Pipe tempP = (Pipe)pipes1.get( i );
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


    //		System.out.println("================= start of CRF ============");
    //		crf.print();
    //		System.out.println("==================end of crf ==============");

    //xxx

    logger.log( Level.INFO, "Load CRF successfully\n" );
  }

  public void loadCRF(CRF crf) {
    this.crf = crf;
    this.pipe = (SerialPipes)crf.getInputPipe();

    if (this.pipe == null) {
      System.err.println( "Get a null pipe from CRF" );
      System.exit( 1 );
    }
  }


  public String printResultInFormat(boolean sgml) {

    String viterbiStr = "";

    assert(tokenSequence != null);
    assert(viterbiSequence != null);
    assert(tokenSequence.size() == viterbiSequence.size());

    if (sgml) {
      String old_tag = null;
      String startTag, endTag;
      for (int i = 0; i < tokenSequence.size(); i++) {
        String word = (tokenSequence.getToken( i )).getText();
        String tag = viterbiSequence.get( i ).toString();

        if (tag != old_tag) {
          if (old_tag != null) {
            endTag = "</" + old_tag + ">";
            viterbiStr += endTag;
          }

          startTag = "<" + tag + ">";
          viterbiStr += startTag;

          old_tag = tag;
        }

        viterbiStr += word;
        viterbiStr += " ";
        if (i == tokenSequence.size() - 1) {
          endTag = "</" + tag + ">";
          viterbiStr += endTag;
        }
      }

    }
    else {

      for (int i = 0; i < tokenSequence.size(); i++) {
        viterbiStr += (tokenSequence.getToken( i )).getText();
        viterbiStr += ": ";
        viterbiStr += viterbiSequence.get( i ).toString();
        viterbiStr += "\n";
      }
    }

    return viterbiStr;

  }

  //given an input string, label it, and output in the format of inline SGML
  public String viterbiCRFString(String line, boolean sgml) {
    Instance lineCarrier = new Instance( line, null, null, null, pipe );

    assert(pipe != null);
    assert(crf != null);

    //		Instance featureCarrier = pipe.pipe(lineCarrier, 0);
    //		viterbiSequence = crf.viterbiPath((Sequence)featureCarrier.getData()).output();
    //		tokenSequence = (TokenSequence)featureCarrier.getSource();

    viterbiSequence = crf.viterbiPath( (Sequence)lineCarrier.getData() ).output();
    tokenSequence = (TokenSequence)lineCarrier.getSource();

    assert(viterbiSequence.size() == tokenSequence.size());

    return printResultInFormat( sgml );
  }

  //viterbi for a piped instance
  public String viterbiCRFInstance(Instance instance, boolean sgml) {

    assert(crf != null);

    viterbiSequence = crf.viterbiPath( (Sequence)instance.getData() ).output();

    tokenSequence = (TokenSequence)instance.getSource();
    assert(viterbiSequence.size() == tokenSequence.size());

    return printResultInFormat( sgml );

  }

  //given an input file, label it, and output in the format of inline SGML
  public void viterbiCRF(File inputFile, boolean sgml, String seperator) {

    assert(pipe != null);
    InstanceList instancelist = new InstanceList( pipe );

    Reader reader;
    try {
      reader = new FileReader( inputFile );
    }
    catch (Exception e) {
      throw new IllegalArgumentException( "Can't read file " + inputFile );
    }

    instancelist.add( new LineGroupIterator( reader, Pattern.compile( seperator ), true ) );

    String outputFileStr = inputFile.toString() + "_tagged";

    System.out.println( inputFile.toString() + " ---> " + outputFileStr );

    PrintStream taggedOut = null;
    try {
      FileOutputStream fos = new FileOutputStream( outputFileStr );
      taggedOut = new PrintStream( fos );
    }
    catch (IOException e) {
      logger.warning( "Couldn't open output file '" + outputFileStr + "'" );
    }

    if (taggedOut == null) {
      taggedOut = System.out;
    }

    String viterbiStr = "";
    //		taggedOut.println("testing instance number: " + instancelist.size() );
    for (int i = 0; i < instancelist.size(); i++) {
      //				taggedOut.println("\ntesting instance " + i);
      Instance instance = instancelist.getInstance( i );
      String crfStr = viterbiCRFInstance( instance, sgml );

      taggedOut.println( seperator );
      taggedOut.println( crfStr );

      viterbiStr += crfStr;
    }


    if (taggedOut != System.out) {
      taggedOut.close();
    }

  }

  //viterbi for all files under a given directory,
  //if the given directory is a plain file, viterbi for this file
  public void viterbiCRF(String inputDir, boolean sgml, String seperator) {


    // if inputDir is a plain file
    File file = new File( inputDir );
    if (file.isFile()) {
      viterbiCRF( file, sgml, seperator );
    }
    else {
      // continue if it is a directory
      FileIterator fileIter = new FileIterator( inputDir );
      ArrayList fileList = fileIter.getFileArray();

      for (int i = 0; i < fileList.size(); i++) {
        file = (File)fileList.get( i );
        viterbiCRF( file, sgml, seperator );
      }
    }
  }

  public void viterbiCRF(String inputDir) {
    viterbiCRF( inputDir, true );
  }

  public void viterbiCRF(String inputDir, boolean sgml) {
    viterbiCRF( inputDir, sgml, seperator );
  }
}
