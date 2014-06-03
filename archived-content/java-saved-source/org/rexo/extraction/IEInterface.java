/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet

   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/**
 @author Fuchun Peng <a href="mailto:fuchun@cs.umass.edu">fuchun@cs.umass.edu</a>
 July 2003

 This class provides information extraction interface to other applications
 */

package org.rexo.extraction;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.pipe.iterator.FileIterator;
import edu.umass.cs.mallet.base.pipe.iterator.LineGroupIterator;
import edu.umass.cs.mallet.base.types.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class IEInterface {
  String separator = "";

  private static Logger logger = Logger.getLogger( IEInterface.class.getName() );

  private File crfFile;
  private CRF4 crf = null;
  private SerialPipes pipe;
  private TokenSequence tokenSequence;
  private Sequence viterbiSequence;

  public IEInterface() {
    this.crfFile = null;
  }

  public IEInterface(File crfFile) {
    assert(crfFile != null);
    this.crfFile = crfFile;
  }

  public void setPipe(SerialPipes pipe) {
    this.pipe = pipe;
  }

  // load in CRF4 and its pipe from a trained crfFile
  public boolean loadCRF() {
    return loadCRF( crfFile );
  }


  public boolean loadCRF(File crfFile) {

    assert(crfFile != null);

    try {
      ObjectInputStream ois = new ObjectInputStream( new FileInputStream( crfFile ) );
      this.crf = (CRF4)ois.readObject();
      ois.close();
      this.pipe = (SerialPipes)crf.getInputPipe();
    }
    catch (Exception e) {
      System.err.println( e.getMessage() );
      return false;
    }

    if (this.pipe == null) {
      System.err.println( "Got a null pipe from CRF" );
      return false;
    }
    logger.log( Level.INFO, "Load CRF successfully" );
    return true;
  }

  public void loadCRF(CRF4 crf) {
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
  public void viterbiCRF(File inputFile, boolean sgml, String separator) {

    assert(pipe != null);
    InstanceList instancelist = new InstanceList( pipe );

    Reader reader;
    try {
      reader = new FileReader( inputFile );
    }
    catch (Exception e) {
      throw new IllegalArgumentException( "Can't read file " + inputFile );
    }

    instancelist.add( new LineGroupIterator( reader, Pattern.compile( separator ), true ) );

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

      taggedOut.println( separator );
      taggedOut.println( crfStr );

      viterbiStr += crfStr;
    }


    if (taggedOut != System.out) {
      taggedOut.close();
    }

  }

  //viterbi for all files under a given directory,
  //if the given directory is a plain file, viterbi for this file
  public void viterbiCRF(String inputDir, boolean sgml, String separator) {


    // if inputDir is a plain file
    File file = new File( inputDir );
    if (file.isFile()) {
      viterbiCRF( file, sgml, separator );
    }
    else {
      // continue if it is a directory
      FileIterator fileIter = new FileIterator( inputDir );
      ArrayList fileList = fileIter.getFileArray();

      for (int i = 0; i < fileList.size(); i++) {
        file = (File)fileList.get( i );
        viterbiCRF( file, sgml, separator );
      }
    }
  }

  public void viterbiCRF(String inputDir) {
    viterbiCRF( inputDir, true );
  }

  public void viterbiCRF(String inputDir, boolean sgml) {
    viterbiCRF( inputDir, sgml, separator );
  }


  public File getCrfFile() {
    return crfFile;
  }

  public CRF4 getCrf() {
    return crf;
  }

  public SerialPipes getPipe() {
    return pipe;
  }

  public TokenSequence getTokenSequence() {
    return tokenSequence;
  }

  public Sequence getViterbiSequence() {
    return viterbiSequence;
  }
  public void setTokenSequence(TokenSequence tokenSequence) {
    this.tokenSequence = tokenSequence;
  }

  public void setViterbiSequence(Sequence viterbiSequence) {
    this.viterbiSequence = viterbiSequence;
  }

  public void setCrfFile(File crfFile) {
    this.crfFile = crfFile;
  }

  public void setCrf(CRF4 crf) {
    this.crf = crf;
  }

}



