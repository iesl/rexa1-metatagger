/*
 * Created on Feb 4, 2004
 *
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * 
 */
package org.rexo.pipeline.components;

import org.rexo.io.LazyFileLister;

import java.io.*;
import java.net.URI;
import java.util.Iterator;

/**
 *
 */
public class RxDocumentQueue implements RxDocumentSource {
	private File _basePath= null;
	private String[] _extensions = new String[]{".xml"};
	private long _maxDocuments = 0;

	public RxDocumentQueue(File basePath) {
		_basePath = basePath;
	}

	public void setSource(URI sourceURI) {
		_basePath = new File( sourceURI.getPath() );
	}

	public void setMaxDocuments(long max) {
		_maxDocuments = max;
	}

	public void closeDocument(RxDocument rdoc) {

	}

	public void closeSource(RxDocument lastDoc) {

	}

	public static class DocumentIterator implements Iterator {
		private Iterator _iterator;

		public DocumentIterator(File root, final String[] extensions) {
			try {
				_iterator = LazyFileLister.iterator( root, new FilenameFilter() {
					public boolean accept(File dir, String name) {
						for (int i = 0; i < extensions.length; i++) {
							String extension = extensions[i];
							if (name.endsWith( extension )) {
								return true;
							}
						}
						return false;
					}
				} );
			}
			catch (IOException e) {

				// TODO handle exception
				e.printStackTrace();
			}
		}

		//
		public boolean hasNext() {
			return _iterator.hasNext();
		}

		//
		public Object next() {
			File nextFile = (File)_iterator.next();
			if (nextFile != null) {
				return makeRxDocument( nextFile, 0 );
			}
			return null;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	};


	public Iterator iterator() {
		return new DocumentIterator( _basePath, _extensions );
	}

	public void setExtensions(String[] extensions) {
		_extensions = extensions;
	}

	public String toString() {
		StringBuffer returnBuffer = new StringBuffer();
		returnBuffer.append( _basePath.getPath() );
		returnBuffer.append( "[readable=" + new File( _basePath.getPath() ).canRead() + "]" );
		return returnBuffer.toString();
	}

	public URI getBaseURI() {
		return _basePath.toURI();
	}

	private static RxDocument makeRxDocument(File docfile, int docnum) {
		RxDocument rdoc = new RxDocument();
		rdoc.getScope( "document" ).put( "corpus.id", new Long( docnum ) );
		rdoc.getScope( "document" ).put( "file.name", docfile.getPath() );
		rdoc.getScope( "document" ).put( "file.basename", docfile.getName() );
		byte[] inputBytes = new byte[(int)docfile.length()];
		try {
			InputStream in = new FileInputStream( docfile );
			int i = in.read( inputBytes );
		}
		catch (IOException e) {
			System.out.println( "error unzipping input xml bytes: " + e );
			return null;
		}
		rdoc.getScope( "document" ).put( "docBytes", inputBytes );

		return rdoc;
	}

}
