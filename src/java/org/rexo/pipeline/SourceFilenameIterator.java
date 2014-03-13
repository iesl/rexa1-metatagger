/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jun 25, 2004
 * author: adingle
 */

package org.rexo.pipeline;

import org.rexo.io.BufferedUnixReader;

import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/**
 * Iterates through a list of filenames contained in a file, sequentially or
 * randomly
 */
public class SourceFilenameIterator implements Iterator {
	private String _filename;
	private RandomAccessFile _infile;
	private BufferedUnixReader _inreader;
	private ArrayList _lineBreaks = new ArrayList();
	private Iterator _lineBreakIterator;

	/* Originally, each line was read sequentially with _infile,
	 * but due to lack of buffering this was unacceptably slow.  Unfortunately
	 * there doesn't seem to be any class built into Java that combines random
	 * access with buffered IO, so we need to use seperate IO mechanisms for
	 * initialization and iteration.
	 */

	/* Since we need to keep track of our absolute position in the file, we
	 * should use BufferedUnixReader since it guarantees that all strings are
	 * seperated by exactly one byte ('\n').
	 */
	   
	private void initLineBreaks(boolean isRandom, boolean withSeed, long randSeed) {
		System.out.println("Scanning source file " + _filename + "...");

		try {
			//long loc = _infile.getFilePointer();
			//String s = _infile.readLine();
			long loc = 0;
			String s = _inreader.readLine();

			int c = 0;
			while (s != null) {
				_lineBreaks.add(new Long(loc));
				//loc = _infile.getFilePointer();
				//s = _infile.readLine();
				loc += s.length() + 1;
				s = _inreader.readLine();
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		// If randomized, shuffle the list of line breaks
		if (isRandom) {
			if (!withSeed) {
				System.out.println("Randomizing....");
			}
			else {
				System.out.println("Randomizing with seed "+randSeed+"....");
			}
			Object[] arr = _lineBreaks.toArray();

			// initialize the random number generator
			Random rand; 
			if (withSeed) rand = new Random(randSeed);
			else rand = new Random();

			// swap random pairs until randomized
			for (int i = 0; i < arr.length; i++) {
				int swapWith = rand.nextInt(arr.length-i)+i;
				Object tmp = arr[i];
				arr[i] = arr[swapWith];
				arr[swapWith] = tmp;
			}
		    _lineBreaks = new ArrayList(Arrays.asList(arr));
		}

		System.out.println("done.");
	}

	public SourceFilenameIterator(String filename) throws IOException {
		this(filename, false);
	}

	public SourceFilenameIterator(String filename, boolean isRandom) throws IOException {
		this(filename, isRandom, false, 0);
	}

	public SourceFilenameIterator(String filename, boolean isRandom, long randSeed) throws IOException {
		this(filename, isRandom, true , randSeed);
	}

	private SourceFilenameIterator(String filename, boolean isRandom, boolean withSeed, long randSeed) throws IOException {
		_filename = filename;
		_inreader = new BufferedUnixReader(new FileReader(filename));
		initLineBreaks(isRandom, withSeed, randSeed);
		_lineBreakIterator = _lineBreaks.iterator();
		_inreader.close();
		_infile = new RandomAccessFile(filename, "r");
	}


	public boolean hasNext() {
		return _lineBreakIterator.hasNext();
	}

	public Object next() {
		try {
			Long lineOfs = (Long)_lineBreakIterator.next();
			_infile.seek(lineOfs.longValue());
			String ret = _infile.readLine();
			return ret;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void remove () {
		throw new UnsupportedOperationException();
	}

}
