/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 25, 2004
 * author: asaunders
 */

package org.rexo.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	private static String REMOVAL_PLACEHOLER = "@~@";

	public static String removeLigatures(final String string) {
		String woLigs = string;
		woLigs = woLigs.replaceAll( "ffi", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( "fi", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( "fl", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( "ff", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( "ae", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( "oe", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( "ll", REMOVAL_PLACEHOLER );
		woLigs = woLigs.replaceAll( REMOVAL_PLACEHOLER, "" );
		return woLigs;
	}


	private static int min(int a, int b, int c) {
		int mi;
		mi = a;
		if (b < mi) {
			mi = b;
		}
		if (c < mi) {
			mi = c;
		}
		return mi;
	}

	/**
	 * @param s
	 * @param t
	 * @return
	 */
	public static int LevenshteinDistance(String s, String t) {
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1
		n = s.length();
		m = t.length();
		if (n == 0) {
			return m;
		}
		if (m == 0) {
			return n;
		}
		d = new int[n + 1][m + 1];

		// Step 2
		for (i = 0; i <= n; i++) {
			d[i][0] = i;
		}

		for (j = 0; j <= m; j++) {
			d[0][j] = j;
		}

		// Step 3
		for (i = 1; i <= n; i++) {
			s_i = s.charAt( i - 1 );
			// Step 4
			for (j = 1; j <= m; j++) {
				t_j = t.charAt( j - 1 );
				// Step 5
				if (s_i == t_j) {
					cost = 0;
				}
				else {
					cost = 1;
				}
				// Step 6
				d[i][j] = min( d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost );
			}
		}

		// Step 7
		return d[n][m];
	}

	/**
   * Makes a delimited list of items from a Collection, just like Perl's join()
   * function.
   * @param items a <code>Collection</code> of <code>Object</code>s with
   *          appropriate <code>toString</code> methods
   * @param delimiter a <code>String</code>
   * @return a <code>String</code> containing the string representation of the
   *         Collection elements, delimited by <code>delimiter</code>
   */
	/* TODO: provide escaping of specified characters */
	/* TODO: overload method signature for default parameters */
	public static String makeListString( Collection items,
	                                     String delimiter )
	{
		boolean isFirst = true;
		StringBuffer buf = new StringBuffer();
		for ( Iterator iter = items.iterator(); iter.hasNext(); ) {
			String item = iter.next().toString();
			if ( isFirst ) {
				isFirst = false;
			} else {
				buf.append( delimiter );
			}
			buf.append( item );
		}
		return buf.toString();
	}


	private static NumberFormat timeSegmentFormat = NumberFormat.getIntegerInstance();
	private static StringBuffer timeBuf = new StringBuffer();
	static {
		timeSegmentFormat.setMinimumIntegerDigits( 2 );
	}
	
	/**
	 * Convert a millisecond value to an elapsed time string, 
	 * @param milliseconds
	 * @return
	 */
	public static String formatElapsedTime( long milliseconds,
	                                        boolean withMillisecPrecision )
	{
		long hours =          milliseconds                        / ( 1000 * 60 * 60 );
		long minutes =      ( milliseconds % ( 1000 * 60 * 60 ) ) / ( 1000 * 60 );
		long seconds =      ( milliseconds % ( 1000 * 60 ) )      /   1000;
		milliseconds =        milliseconds %   1000;
		timeBuf.setLength( 0 );
		timeBuf.append( hours ).append( ":" );
		timeBuf.append( timeSegmentFormat.format( minutes ) ).append( ":" );
		timeBuf.append( timeSegmentFormat.format( seconds ) );
		if ( withMillisecPrecision ) {
			timeBuf.append( "." );
			timeBuf.append( timeSegmentFormat.format( milliseconds ) );
		}
		return timeBuf.toString();
	}
	
	public static String makeRepeatedString( String segment,
	                                         int count )
	{
		int targetLength = segment.length() * count;
		StringBuffer buf = new StringBuffer( targetLength );
		for ( int i = 0; buf.length() < targetLength; ++i ) { 
			// for efficiency, we'll double the size of our repeated string, if
      // possible
			if ( buf.length() > 0 && buf.length() * 2 <= targetLength ) {
				buf.append( buf );
			} else {
				buf.append( segment );
			}
		}
		return buf.toString();
	}


	public static List wrapStrings( List elements, String left, String right )
	{
		StringBuffer buf = new StringBuffer();
		List result = new ArrayList( elements.size() );
		for ( Iterator iter = elements.iterator(); iter.hasNext(); ) {
			buf.append( left ).append( iter.next() ).append( right );
			result.add( buf.toString() );
			buf.setLength( 0 );
		}
		return result;
	}

  /**
   * Returns string with initial non-alphanumeric characters replaced with spaces. Need to trim in calling function.
   */
  public static String cleanString(String str) {
    if (str == null) {
      return null;
    }
    char strChars[] = str.toCharArray();
    int strLen = strChars.length;

    if (str.startsWith( "fl " )) {
      strChars[0] = ' ';
      strChars[1] = ' ';
    }
    if (str.endsWith( " fl" )) {
      strChars[strLen - 1] = ' ';
      strChars[strLen - 2] = ' ';
    }

    for (int i = 0; i < strChars.length; i++) {
      if (strChars[i] >= '0' && strChars[i] <= '9') {
        break;
      }
      else if (strChars[i] >= 'a' && strChars[i] <= 'z') {
        break;
      }
      else if (strChars[i] >= 'A' && strChars[i] <= 'Z') {
        break;
      }
      else {
        strChars[i] = ' ';
      }
    }

    for (int i = strChars.length - 1; i > -1; i--) {
      if (strChars[i] >= '0' && strChars[i] <= '9') {
        break;
      }
      else if (strChars[i] >= 'a' && strChars[i] <= 'z') {
        break;
      }
      else if (strChars[i] >= 'A' && strChars[i] <= 'Z') {
        break;
      }
      else {
        strChars[i] = ' ';
      }
    }

    return new String( strChars ).trim();
  }
  
  public static Pattern leadingPunctPattern = Pattern.compile( "^[\\p{Punct}]" );
  public static Pattern trailingPunctPattern = Pattern.compile( "([\\p{Punct}])$" );

  public static String trimPunctuationChars(String str) {
    Matcher matcher = leadingPunctPattern.matcher( str );
    String trimmedStr = str;
    if (trimmedStr != null && trimmedStr.length() > 0 && trimmedStr.charAt( 0 ) != '(') {
      trimmedStr = matcher.replaceFirst( "" );
    }
    matcher = trailingPunctPattern.matcher( trimmedStr );
    if (trimmedStr != null && trimmedStr.length() > 0 && trimmedStr.charAt( trimmedStr.length() - 1 ) != ')') {
      trimmedStr = matcher.replaceFirst( "" );
    }
    return trimmedStr;
  }

  /**
   * Return a 4-digit year even if year is a 2-digit year (w/some qualifying prefix)
   */ 
  public static String getNormalizedYear(String str) {
    if (str.charAt( 0 ) == '-' || str.charAt( 0 ) == '\'') {
      String toreplace = "19";

      if (str.charAt( 1 ) == '0') {
        toreplace = "20";
      }

      str = str.replaceAll( "-", toreplace );
      str = str.replaceAll( "'", toreplace );

      return str;
    }
    else {
      // remove non-digits
      str = str.replaceAll( "[^\\d]+", "" );
      return str;
    }
  }
  
}
