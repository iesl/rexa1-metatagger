/*
 * Created on Feb 4, 2004
 * 
 */
package org.rexo.pipeline.components;

import org.apache.log4j.lf5.util.StreamUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.rexo.exceptions.InitializationException;
import org.rexo.extraction.NewHtmlTokenization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author asaunders
 */
public class RxDocument {
	private Map _scopeMap = new HashMap();

	public Element getSource() {
		throw new UnsupportedOperationException();
	}

	public Element getDocument() {
		Document document = (Document)getScope( "document" ).get( "document" );
		return (Element)document.getRootElement();
	}

	public RxDocument(Document document) {
		createScopes();
		getScope( "document" ).put( "document", document );
	}


	public RxDocument() {
		createScopes();
	}

	protected void createScopes() {
		setScope( "document", new HashMap() );
		setScope( "session", new HashMap() );
		setScope( "global", new HashMap() );
		getScope( "document" ).put( "collated.document", null );
		getScope( "document" ).put( "error.code.string", "none" );
	}

	public static RxDocument create() {
		return new RxDocument();
	}

	/**
	 * @param stringReader
	 * @return
	 */
	public static RxDocument create(StringReader stringReader) {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		try {
			doc = builder.build( stringReader );
		}
		catch (JDOMException e) {
			// TODO handle exception
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO handle exception
			e.printStackTrace();
		}

		return new RxDocument( doc );
	}

	/**
	 * @param scope
	 * @return
	 */
	public Map getScope(String scope) {
		return (Map)_scopeMap.get( scope );
	}

	/**
	 * @param scope
	 * @param scopeMap
	 */
	public void setScope(String scope, Map scopeMap) {
		_scopeMap.remove( scope );
		_scopeMap.put( scope, scopeMap );
	}

	//APD
	/**
	 * Adds a document error string, which may be processed later by an error filter.
	 */
	public void docErrorString(String error) {
		LinkedList errorList;

		errorList = (LinkedList)getScope( "document" ).get( "error.list" );
		if (errorList == null) {
			errorList = new LinkedList();
			getScope( "document" ).put( "error.list", errorList );
		}
		errorList.add( error );
	}

	//APD
	/**
	 * Adds a document info string, which may be processed later by another filter.
	 */
	public void docInfoString(String info) {
		LinkedList infoList;

		infoList = (LinkedList)getScope( "document" ).get( "info.list" );
		if (infoList == null) {
			infoList = new LinkedList();
			getScope( "document" ).put( "info.list", infoList );
		}
		infoList.add( info );
	}


	/**
	 * Returns a String which names this document.  This will be the SHA1 hash, if available, or the name of the document from the filesystem.
	 */
	public String getId() {
		String docFname = (String)getScope( "document" ).get( "corpus.fileSHA1" );
		if (docFname == null) {
			docFname = (String)getScope( "document" ).get( "file.basename" );
		}
		if (docFname == null) {
			docFname = "";
		}
		return docFname;
	}

	public NewHtmlTokenization getTokenization() {
		NewHtmlTokenization tokenization = (NewHtmlTokenization)getScope( "document" ).get( "tokenization" );
		return tokenization;
	}

	public void setTokenization(NewHtmlTokenization tokenization) {
		getScope( "document" ).put( "tokenization", tokenization );
	}

	public NewHtmlTokenization getTokenization(String whichTokenization) {
		NewHtmlTokenization tokenization = (NewHtmlTokenization)getScope( "document" ).get( whichTokenization + ".tokenization" );
		return tokenization;
	}

	public void setTokenization(String whichTokenization, NewHtmlTokenization tokenization) {
		getScope( "document" ).put( whichTokenization + ".tokenization", tokenization );
	}

}
