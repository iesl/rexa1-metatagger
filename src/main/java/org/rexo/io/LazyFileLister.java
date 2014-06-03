/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Oct 15, 2004
 * author: saunders
 */

package org.rexo.io;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * iterate over a set of files in a directory, only getting a single file at a time
 */
public class LazyFileLister {
	private LazyFileLister() {

	}

	public static Iterator iterator(File root, FilenameFilter filter) throws IOException {
		return new LazyFileListerIterator( root, filter, false );
	}

	public static Iterator destructiveIterator(File root, FilenameFilter filter) throws IOException {
		return new LazyFileListerIterator( root, filter, true );
	}

	/**
	 *
	 */
	protected static class LazyFileListerIterator implements Iterator {
		private static Logger log = Logger.getLogger( LazyFileListerIterator.class );

		private FilenameFilter _fileFilter;
		private boolean _rmEmptyDirectories = false;

		private List _dirList = new LinkedList();
		private List _fileList = new LinkedList();

		public LazyFileListerIterator(File root, FilenameFilter filter, boolean rmEmptyDirectories) throws IOException {
			_fileFilter = filter;
			_rmEmptyDirectories = rmEmptyDirectories;
			if (root.isFile()) {
				_fileList.add( root );
			}
			else if (root.isDirectory()) {
				_dirList.add( root );
			}
			else {
				throw new IOException( "root file '" + root.getName() + "' is neither plain-file nor a directory" );
			}
		}


		/**
		 * are there more files?
		 *
		 */
		public boolean hasNext() {
			return hasNextFile();
		}

		/**
		 * get next file
		 *
		 */
		public Object next() {
			return getNextFile();
		}


		/**
		 */
		private boolean hasNextFile() {
			File nextFile = getNextFile();
			// find and 'push back' the file
			if (nextFile != null) {
				_fileList.add( 0, nextFile );
				return true;
			}
			return false;
		}

		/**
		 */
		private File getNextFile() {
			while (!_fileList.isEmpty() || !_dirList.isEmpty()) {
				if (_fileList.isEmpty()) {
					if (!_dirList.isEmpty()) {
						File nextDir = (File)_dirList.remove( 0 );
						String[] subdirs = nextDir.list( new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return new File( dir, name ).isDirectory();
							}
						} );

						for (int i = 0; i < subdirs.length; i++) {
							String subdir = subdirs[i];
							_dirList.add( 0, new File( nextDir, subdir ) );
						}

						String[] files = nextDir.list( _fileFilter );
						for (int i = 0; i < files.length; i++) {
							String filename = files[i];
							_fileList.add( new File( nextDir, filename ) );
						}

						if ( _rmEmptyDirectories && subdirs.length==0 && files.length==0) {
							// remove empty subdirectory
							try {
								FileUtils.deleteDirectory( nextDir );
							}
							catch (IOException e) {
								log.error( e.getMessage() );
							}
						}
					}
				}
				if (!_fileList.isEmpty()) {
					return (File)_fileList.remove( 0 );
				}
			}
			return null;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
