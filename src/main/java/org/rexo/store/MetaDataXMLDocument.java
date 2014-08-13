/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Oct 18, 2004
 * author: saunders
 */

package org.rexo.store;

import edu.umass.cs.mallet.base.util.PropertyList;
import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rexo.exceptions.ForwardedException;
import org.rexo.exceptions.InitializationException;
import org.rexo.extraction.HSpans;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.util.SHA1Hash;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MetaDataXMLDocument {
	private static Logger log = Logger.getLogger( MetaDataXMLDocument.class );
	private Document _document = null;


	public Element getHeadersElement() throws NoSuchElementException {
		try {
			return _document.getRootElement()
					.getChild( "content" )
					.getChild( "headers" );
		}
		catch (NullPointerException e) {
			throw new NoSuchElementException( "headers" );
		}
	}

	public Element getBiblioElement() throws NoSuchElementException {
		try {
			return _document.getRootElement()
					.getChild( "content" )
					.getChild( "biblio" );
		}
		catch (NullPointerException e) {
			throw new NoSuchElementException( "biblio" );
		}
	}

	public Element getBodyElement() throws NoSuchElementException {
		try {
			return _document.getRootElement()
					.getChild( "content" )
					.getChild( "body" );
		}
		catch (NullPointerException e) {
			throw new NoSuchElementException( "body" );
		}
	}

	private List getGrantNumbers() {
		return getGrantNumbersCheesyMethod();
	}

	private List getGrantNumbersCheesyMethod() {
		try {
			Iterator descendants = _document.getRootElement()
					.getChild( "grants" ).getDescendants( new Filter() {
				public boolean matches(Object o) {
					if (o instanceof Element) {
						Element subElement = (Element)o;
						if (subElement.getName().equals( "grant-number" )) {
							return true;
						}
					}
					return false;
				}
			} );

			ArrayList nsfGrantNumbers = new ArrayList();

			while (descendants.hasNext()) {
				Element grantNumberElement = (Element)descendants.next();
				String text = grantNumberElement.getText();
				String grantNumber = cleanNSFGrantNumber( text );
				nsfGrantNumbers.add( grantNumber );
			}
			return nsfGrantNumbers;
		}
		catch (NullPointerException e) {
			return ListUtils.EMPTY_LIST;
		}
	}

	private List getGrantNumbersRobustMethod() {
		try {
			Iterator descendants = _document.getRootElement()
					.getChild( "grants" ).getDescendants( new Filter() {
				public boolean matches(Object o) {
					if (o instanceof Element) {
						Element subElement = (Element)o;
						if (subElement.getName().equals( "grant-number" )) {
							return true;
						}
					}
					return false;
				}
			} );

			boolean foundNSF = false;
			ArrayList nsfGrantNumbers = new ArrayList();

			while (descendants.hasNext()) {
				Element grantRecElement = (Element)descendants.next();
				String text = grantRecElement.getText();
				if (grantRecElement.getName().equals( "grant-institution" )) {
					if (isNSFInstitution( text )) {
						foundNSF = true;
					}
				}
				else {
					if (foundNSF) {
						String grantNumber = cleanNSFGrantNumber( text );
						if (isNSFGrantNumber( grantNumber )) {
							nsfGrantNumbers.add( grantNumber );
						}
					}
				}
			}
			return nsfGrantNumbers;
		}
		catch (NullPointerException e) {
			return ListUtils.EMPTY_LIST;
		}
	}

	private boolean isNSFGrantNumber(String text) {
		Pattern pattern = Pattern.compile( "\\d{7}" );
		return pattern.matcher( text ).find();
	}

	private String cleanNSFGrantNumber(String text) {
		String cleaned = text.replaceAll( "[^\\d]", "" );
		return cleaned;
	}

	private Pattern[] NSF_INST_PATTERNS = new Pattern[]{
			Pattern.compile( "NSF" ),
	    Pattern.compile( "National\\s+Science\\s+Foundation", Pattern.CASE_INSENSITIVE ),

	};

	private boolean isNSFInstitution(String text) {
		for (int i = 0; i < NSF_INST_PATTERNS.length; i++) {
			Pattern pattern = NSF_INST_PATTERNS[i];
			if (pattern.matcher( text ).find()) {
				return true;
			}
		}
		return false;
	}

	/**
	 */
	public String getBodyText() throws IncorrectXMLStructureException {
		try {
			Element bodyElement = getBodyElement();
			return getNormalizedChildText( bodyElement );
		}
		catch (NoSuchElementException e) {
			throw new IncorrectXMLStructureException( e );
		}
	}

	public MetaDataXMLDocument(Document document) {
		_document = document;
	}


	/**
	 * @param gzipCompressedBytes
	 */
	public MetaDataXMLDocument(byte[] gzipCompressedBytes) throws InitializationException {
		try {
			GZIPInputStream inputStream = null;
			inputStream = new GZIPInputStream( new ByteArrayInputStream( gzipCompressedBytes ) );
			SAXBuilder saxBuilder = new SAXBuilder();
			_document = saxBuilder.build( inputStream );
		}
		catch (IOException e) {
			throw new InitializationException( e );
		}
		catch (JDOMException e) {
			throw new InitializationException( e );
		}
	}

	/**
	 * @param inputStream
	 */
	public MetaDataXMLDocument(InputStream inputStream) throws InitializationException {
		try {
			SAXBuilder saxBuilder = new SAXBuilder();
			_document = saxBuilder.build( inputStream );
		}
		catch (IOException e) {
			throw new InitializationException( e );
		}
		catch (JDOMException e) {
			throw new InitializationException( e );
		}
	}


	public Document getDocument() throws IOException {
		return _document;
	}

	/**
	 */
	public byte[] toGZippedXML() throws IOException {
		XMLOutputter output = new XMLOutputter( Format.getRawFormat() );

		// gzip XML for storage
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream xmlOutputStream = new GZIPOutputStream( bos );
		try {
			output.output( _document, xmlOutputStream );
		}
		catch (IOException e) {
		}
		return bos.toByteArray();
	}

	private String getNormalizedChildText(Element parent) {
		StringBuffer stringBuffer = new StringBuffer();
		Iterator iterator = parent.getDescendants( new Filter() {
			public boolean matches(Object o) {
				return o instanceof Text;
			}
		} );

		while (iterator.hasNext()) {
			Text text = (Text)iterator.next();
			stringBuffer
					.append( text.getTextNormalize() )
					.append( " " );
		}
		return stringBuffer.toString();
	}

	/**
	 */
	public static class IncorrectXMLStructureException extends ForwardedException {
		public IncorrectXMLStructureException(String msg) {
			super( msg );
		}

		public IncorrectXMLStructureException(Throwable cause) {
			super( cause );
		}

		public IncorrectXMLStructureException(String msg, Throwable cause) {
			super( msg, cause );
		}
	}

	public static class NoSuchElementException extends Exception {
		public NoSuchElementException(String msg) {
			super( msg );
		}

		public NoSuchElementException(Throwable cause) {
			super( cause );
		}

		public NoSuchElementException(String msg, Throwable cause) {
			super( msg, cause );
		}
	}

	/**
	 * @param fileSHA1
	 */
	public static MetaDataXMLDocument createFromTokenization(SHA1Hash fileSHA1, Map segmentations) {
		// Reconstruct headers
		// xml structure:
		//  root
		//    headers
		//    body
		//    biblio
		//      reference
		//    grants
		//      grant-institution
		//      grant-number

		// write out:
		//    paper <xml><paper><headers/><body/><cic/><biblio/><grants/>...
		//     headers <headers><author>...
		//     body    <body>full text...</body>
		//     citations-in-context <cic refID="id">surrounding text<ref-marker>[..]</ref-marker> other surrounding text</cic>
		//     biblio  <biblio><reference refID=""/>...
		//     grants
		//       grant-institution
		//       grant-number
		//
		//    references <xml><reference refID=""><author>...

		List referenceElements = (List)segmentations.get( "referenceElements" ); // list of jdom.Element
		NewHtmlTokenization bodyTokenization = (NewHtmlTokenization)segmentations.get( "bodyTokenization" );
		List grantsList = (List)segmentations.get( "grantList" ); // list of Elements
		List citationList = (List)segmentations.get( "citationList" );

		Element contentElement = new Element( "content" );


		// Element headerElement = createJdomFromHSpan( headerSpans );
		Element headerElement = (Element)segmentations.get( "headerElement" );
		Element newHdr = (Element)headerElement.clone();
		contentElement.addContent( newHdr );


		//  reconstruct body

        //bodyElement is the one with sections' information
		Element bodyElement = (Element)segmentations.get("bodyElement");

        if(bodyElement != null) {
            Element newBdy = (Element) bodyElement.clone();
            contentElement.addContent(newBdy);
        }
        else
        {
            bodyElement = new Element( "body" );
            String bodyText = bodyTokenization.getFormattedText();
            bodyElement.setText( bodyText );
            contentElement.addContent( bodyElement );
        }

		// reconstruct biblio
		Element biblioElement = new Element( "biblio" );
		for (int i = 0; i < referenceElements.size(); i++) {
			Element referenceElement = (Element)referenceElements.get( i );
			biblioElement.addContent( (Element)referenceElement.clone() );
		}
		contentElement.addContent( biblioElement );

		// grants:
		Element grantElement = new Element( "grants" );
		if (grantsList != null) {
			for (int i = 0; i < grantsList.size(); i++) {
				Element grantContent = (Element)grantsList.get( i );
				grantElement.addContent( (Element)grantContent.clone() );
			}
		}

		// citations in context:
		Element citationsElement = new Element( "CitationContexts" );
		if (citationList != null) {
			for (int i = 0; i < citationList.size(); i++) {
				Element cicElement = (Element)citationList.get( i );
				citationsElement.addContent( (Element)cicElement.clone() );
			}
		}

		Element rootElement = new Element( "document" );
		rootElement.addContent( contentElement );
		rootElement.addContent( citationsElement );
		rootElement.addContent( grantElement );

		Document document = new Document( rootElement );

		MetaDataXMLDocument newMetaDocument = new MetaDataXMLDocument( document );
		return newMetaDocument;
	}

	/**
	 * @param span
	 */
	private static Element createJdomFromHSpan(HSpans span) {
		ArrayList spanChildren = span.getChildren();
		String spanName = span.getName();
		Element spanElement = new Element( spanName );
		PropertyList properties = span.getProperties();
		if (properties != null) {
			edu.umass.cs.mallet.base.util.PropertyList.Iterator propIter = properties.iterator();
			while (propIter.hasNext()) {
				propIter.next();
				String key = propIter.getKey();
				Object oValue = propIter.getObjectValue();
				if (oValue != null) {
					spanElement.setAttribute( key, oValue.toString() );
				}
			}
		}
		if (spanChildren.isEmpty()) {
			String spanText = span.getText();
			span.getMultiSpan();
			spanElement.setText( spanText );
		}
		else {
			Iterator iterator = spanChildren.iterator();
			while (iterator.hasNext()) {
				HSpans childSpan = (HSpans)iterator.next();
				spanElement.addContent( createJdomFromHSpan( childSpan ) );
			}
		}
		return spanElement;
	}

	private static void dumpDocument(MetaDataXMLDocument metaDataXMLDocument) {
		try {

			Element headersElement = metaDataXMLDocument.getHeadersElement();
			printElement( headersElement );

			Element biblioElement = metaDataXMLDocument.getBiblioElement();
			printElement( biblioElement );

			Element bodyElement = metaDataXMLDocument.getBodyElement();
			printElement( bodyElement );

			String bodyText = metaDataXMLDocument.getBodyText();
			System.out.println( "bodyText: ========================" );
			System.out.println( bodyText );

			System.out.println( "++++++++++++++++++++++++++" );
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printElement(Element element) throws IOException {
		XMLOutputter xmlOutputter = new XMLOutputter( org.jdom.output.Format.getCompactFormat() );
		xmlOutputter.output( element, System.out );
	}
}
