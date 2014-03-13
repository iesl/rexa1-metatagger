/*
 * Created on Feb 11, 2004
 *
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * 
 */
package org.rexo.pipeline;

import edu.umass.cs.mallet.base.extract.CRFExtractor;
import edu.umass.cs.mallet.base.extract.Extraction;
import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.rexo.pipeline.components.RxDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls a CRF to annotate the header and references of a document, then inserts
 *  the predicted tags as XML into the document text, saving the automatically
 *  annotated text in the "annotated-text" property in the document's scope.
 */
public class ReferenceAnnotationFilter extends AbstractFilter {
  private static Logger log = Logger.getLogger( ReferenceAnnotationFilter.class );

  private CRFExtractor _referencesExtractor;
  private CRFExtractor _headersExtractor;

  public ReferenceAnnotationFilter(File referenceCrfFile, File headerCrfFile) {
    initCrfs( referenceCrfFile, headerCrfFile );
  }

  private void initCrfs(File referenceCrfFile, File headerCrfFile) {

    try {
      _referencesExtractor = loadCrfExtor (referenceCrfFile);
    } catch (Exception e) {
      e.printStackTrace ();
      System.out.println( "couldn't init crf '" + headerCrfFile.getPath() + "', continuing..." );
      _referencesExtractor = null;
    }

    try {
      _headersExtractor = loadCrfExtor (headerCrfFile);
    } catch (Exception e) {
      e.printStackTrace ();
      System.err.println( "couldn't init crf '" + referenceCrfFile.getPath() + "', continuing..." );
      _referencesExtractor = null;
    }

  }


  private CRFExtractor loadCrfExtor (File crfFile) throws IOException, ClassNotFoundException
  {
    return (CRFExtractor) new ObjectInputStream (new FileInputStream (crfFile)).readObject();
  }


  /* (non-Javadoc)
   * @see org.rexo.pipeline.components.RxFilter#accept(org.rexo.pipeline.components.RxDocument)
   */
  public int accept(RxDocument rdoc) {

    log.info( "crf classification started");
    int errorCode = ReturnCode.OK;
    try {
      errorCode = doExtraction( rdoc ) ? ReturnCode.OK : ReturnCode.ABORT_PAPER;
    }
    catch (Exception e) {
      errorCode = ReturnCode.ABORT_PAPER;
    }
    return errorCode;
  }


  /**
   * @param rdoc
   * @return
   */
  private boolean doExtraction(RxDocument rdoc) {
    Element contentElement = (Element)rdoc.getScope( "document" ).get( "content.element" );

    if (contentElement == null) {
      getLogger( rdoc ).error( "Partitioner found nothing to partition..." );
      rdoc.docErrorString("Partitioner found nothing to partition");
      return false;
    }

    String headersString = null;
    if (_headersExtractor != null) {
      Element headersElement = contentElement.getChild( "headers" );
      if (headersElement == null) {
        getLogger( rdoc ).error( "no headers to extract" );
        rdoc.docErrorString("no headers to extract");
        return false;
      }
      else {
        String contextString = descendent2text( headersElement );
        Extraction extraction = _headersExtractor.extract (contextString);
        headersString = extraction.getDocumentExtraction (0).toXmlString ();
      }
    }

    String refsString = null;
    if (_referencesExtractor != null) {
      StringBuffer refsBuf = new StringBuffer ();
      List referenceList = contentElement.getChildren( "reference" );
      Iterator referenceIterator = referenceList.iterator();
      while (referenceIterator.hasNext()) {
        Element referenceElement = (Element)referenceIterator.next();
        String contextString = descendent2text( referenceElement );

        String leadingString = "";
        int idx = findLeadBracket (contextString);
        if (idx > 0) {
          leadingString = contextString.substring (0, idx);
          contextString = contextString.substring (idx, contextString.length());
        }

        Extraction extraction = _referencesExtractor.extract (contextString);

        refsBuf.append ("\n    <reference>\n");
        refsBuf.append (leadingString);
        refsBuf.append (extraction.getDocumentExtraction(0).toXmlString ());
        refsBuf.append ("\n    </reference>\n");

//        contextString = cleanUpReferenceString(contextString);
      }
      refsString = refsBuf.toString ();
    }

    // Now reconstruct the document
    boolean shownBib = false;
    XMLOutputter outputter = new XMLOutputter ();
    StringBuffer outbuf = new StringBuffer();
    for (Iterator it = contentElement.getContent ().iterator(); it.hasNext();) {
      Content child = (Content) it.next();
      if (!(child instanceof Element)) {
        outbuf.append (outputContent (outputter, child));
        continue;
      }

      Element element = (Element) child;
      if (element.getName ().equals ("reference")) {
        if (!shownBib) {
          shownBib = true;
          outbuf.append ("\n  <biblio>\n");
          outbuf.append (refsString);
          outbuf.append ("\n  </biblio>\n");
        }
      } else if (element.getName().equals ("headers")) {
        outbuf.append ("\n<headers>\n");
        outbuf.append (headersString);
        outbuf.append ("\n</headers>\n");
      } else {
        outbuf.append (outputter.outputString (element));
      }
    }

    rdoc.getScope ( "document" ).put ("annotated-xml", outbuf.toString());

    return true;
  }


  private String outputContent (XMLOutputter outputter, Content child)
  {
    List l = new ArrayList();
    l.add (child);
    return outputter.outputString (l);
  }


  private String prettify (String refsString)
  {
    SAXBuilder builder = new SAXBuilder ();
    try {
      Document doc = builder.build (new StringReader (refsString));
      XMLOutputter outputter = new XMLOutputter ();
      return outputter.outputString (doc.getRootElement ());
    } catch (JDOMException e) {
      e.printStackTrace ();
    } catch (IOException e) {
      e.printStackTrace ();
    }
    return null;
  }


  /**
   * @param contentElement
   * @return 
   */
  private String descendent2text (Element contentElement) {
    XMLOutputter outputter = new XMLOutputter ();
    return outputter.outputString (contentElement.getContent());
  }

  private static Pattern leadBracketRegex = Pattern.compile ("^(?:\\s|<[^>]*>)*\\[[^\\]]+\\](\\s+)?");
  private static Pattern leadNumberRegex = Pattern.compile ("^\\s*(\\d{1,3})\\.\\s+");

  private int findLeadBracket (String reference)
  {
     Matcher m1 = leadBracketRegex.matcher (reference);
    if (m1.find()) {
      return m1.end ();
    }

    Matcher m2 = leadNumberRegex.matcher (reference);
    if (m2.find()) {
      return m2.end ();
    }

    return -1;
  }



}
