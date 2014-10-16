package org.rexo.util;

import java.io.*;
import java.util.HashSet;

/**
 * Author: saunders Created Nov 15, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class EnglishDictionary {
	private HashSet _words = new HashSet();
	private static File _defaultWords = new File( "data/words.txt" );

	public static void setDefaultWordfile(File f) {
		_defaultWords = f;
	}

    public static EnglishDictionary create(File words) {
        try {
            return create(new FileInputStream(words));
        } catch (FileNotFoundException e) {
            throw new RuntimeException( e );
        }
    }
	public static EnglishDictionary create(InputStream is) {
		EnglishDictionary dict = new EnglishDictionary();
		try {
			BufferedReader in = new BufferedReader( new InputStreamReader(is) );
			String line;
			while ((line = in.readLine()) != null) {
				dict._words.add( line.trim().toLowerCase() );
			}
		}
		catch (IOException e) {
            throw new RuntimeException( e );
		}
		return dict;
	}

	public static EnglishDictionary createDefault() {
		return create( _defaultWords );
	}

	public boolean contains(String word) {
		return _words.contains( word );
	}
}
