/* Copyright (C) 2003 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package org.rexo.pipeline.components;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Document source that retrieves documents from a directory. Mainly useful for debugging Created: Oct 12, 2004
 *
 * @author <A HREF="mailto:casutton@cs.umass.edu>casutton@cs.umass.edu</A>
 * @version $Id: RxDocumentDirectorySource.java,v 1.1.1.1 2006/04/19 17:27:30 saunders Exp $
 */
public class RxDocumentDirectorySource implements RxDocumentSource {
	private static Logger log = Logger.getLogger( RxDocumentDirectorySource.class );

	private long _maxDocs = Long.MAX_VALUE;
	private File _directory = null;
	private File _listFile = null;

	public RxDocumentDirectorySource(File listFile, File dir) {
		_listFile = listFile;
		_directory = dir;
	}

	public RxDocumentDirectorySource(File dir) {
		_directory = dir;
	}

	public void setMaxDocuments(long max) {
		_maxDocs = max;
	}


	protected static class DirectoryIterator implements Iterator {
		private Iterator _docfilesIterator;
		private int _numDocsReturned;
		private int _maxDocs = Integer.MAX_VALUE;

		public DirectoryIterator(File listFile, File directory) {
			List docList;
			if (listFile == null) {
				File[] docs = directory.listFiles( new FileFilter() {
					public boolean accept(File file) {
						return !file.isDirectory();
					}
				} );
				docList = Arrays.asList( docs );
			}
			else {
				docList = new LinkedList();
				BufferedReader in = null;
				try {
					in = new BufferedReader( new FileReader( listFile ) );
					String next;
					while ((next = in.readLine()) != null) {
						docList.add( new File( directory, next ) );
					}
				}
				catch (IOException e) {
					System.out.println( "Error reading source file: " + e );
				}
			}
			_docfilesIterator = docList.iterator();
			_numDocsReturned = 0;
		}

		public boolean hasNext() {
			return (_numDocsReturned < _maxDocs) && _docfilesIterator.hasNext();
		}


		public Object next() {
			File docfile = (File)_docfilesIterator.next();
			RxDocument rdoc = makeRxDocument( docfile, _numDocsReturned );
			_numDocsReturned++;
			return rdoc;
		}


		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Iterator iterator() {
		log.info( "directory: " + _directory );
		DirectoryIterator directoryIterator = new DirectoryIterator( _listFile, _directory );
		return directoryIterator;
	}

	private static RxDocument makeRxDocument(File docfile, int docnum) {
		RxDocument rdoc = new RxDocument();
		rdoc.getScope( "document" ).put( "corpus.id", new Long( docnum ) );
		rdoc.getScope( "document" ).put( "file.name", docfile.toString() );
		rdoc.getScope( "document" ).put( "file.basename", docfile.getName() );
		byte[] inputBytes = new byte[(int)docfile.length()];
		try {
			InputStream in = new FileInputStream( docfile );
			in.read( inputBytes );
		}
		catch (IOException e) {
			System.out.println( "error unzipping input xml bytes: " + e );
			return null;
		}
		rdoc.getScope( "document" ).put( "docBytes", inputBytes );

		return rdoc;
	}

	public void closeDocument(RxDocument rdoc) { }

	public void closeSource(RxDocument lastDoc) { }
}