package org.rexo.referencetagging;

import org.apache.log4j.Logger;
import org.rexo.util.UnicodeUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: saunders Created Nov 9, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class PstotextOutputFixer {
	private static Logger log = Logger.getLogger( PstotextOutputFixer.class );

	private Map charSubstitutionTable = null;

	/**
	 * Combining Diacritical marks: Combining grave accent = U+0300 Combining acute accent = U+0301 Modifier letter acute accent = U+02CA Modifier letter grave accent = U+02CB Acute Accent = U+00B4 grave
	 * = 0060 cedilla = 00C8 masculine indicator (circle above) 00BA diuresis 00A8
	 * <p/>
	 * special cases:
	 * <p/>
	 * url ../~smith/... ^TM
	 */

	public String cleanPsTotextOutput(String lineText1) {
		try {
			byte[] bytes = lineText1.getBytes( "ISO-8859-1" );
			String lineText = new String( bytes, "ISO-8859-1" );
			ArrayList replacements = new ArrayList();
			int ltLen = lineText.length();
			Pattern loneCharModifierP = Pattern.compile( "([\\^\\`\\¨\\¯\\´\\¸\\~\\º\\'])" );
			Matcher matcher = loneCharModifierP.matcher( lineText );

			int findFrom = 0;
			boolean foundMatch = false;
			// get a 3-char window of text around the unicode character (excluding whitespace)
			while (matcher.find( findFrom )) {
				foundMatch = true;
				int s = matcher.start();
				int e = matcher.end();
				findFrom = e;
				String found = matcher.group();
				// some special cases:
				if ("~".equals( found ) && s > 0 && '/' == lineText.charAt( s - 1 )) {
					// probably a url like "../~smith/...
				}
				else if ("^".equals( found ) && s + 2 < ltLen && 'T' == lineText.charAt( s + 1 ) && 'M' == lineText.charAt( s + 2 )) {
					// U+2122 Trade Mark Sign:
					// replacements.add( new Object[]{new int[]{s, s + 3}, "\u2122"} );
					findFrom += 2;
				}
				else if ("`".equals( found ) && s + 1 < ltLen && '`' == lineText.charAt( s + 1 )) {
					// dquote
					replacements.add( new Object[]{new int[]{s, s + 2}, "\""} );
					findFrom += 1;
				}
				else if ("'".equals( found )) {
					if (s + 1 < ltLen && '\'' == lineText.charAt( s + 1 )) {
						// dquote
						replacements.add( new Object[]{new int[]{s, s + 2}, "\""} );
						findFrom += 1;
					}
				}
				else {
					// 'back up' over matched text to find last non-space character
					// while (--s > 0 && ' ' == lineText.charAt( s )) ;
					// 'back up' over matched text to find last character
					s = s > 0 ? s - 1 : s;
					// look forward at most 2 chars over matched text to find next non-space character
					e = e < ltLen ? e + 1 : e;
					e = e < ltLen && ' ' == lineText.charAt( e ) ? e + 1 : e;
					String r = getReplacement( lineText.substring( s, e ) );
					if (r != null) {
						replacements.add( new Object[]{new int[]{s, e}, r} );
					}
				}
			}

			int last = 0;
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < replacements.size(); i++) {
				Object[] repl = (Object[])replacements.get( i );
				int[] se = (int[])repl[0];
				String r = (String)repl[1];
				if (last < se[0]) {
					stringBuffer.append( lineText.substring( last, se[0] ) );
					stringBuffer.append( r );
					last = se[1];
				}
			}
			stringBuffer.append( lineText.substring( last, ltLen ) );

			// if (foundMatch) {
			// 	log.debug( "Cleaning pstotext (" + replacements.size() + " replacements): " + lineText + " ---> " + stringBuffer.toString() );
			// }

			return stringBuffer.toString();
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 *
	 * @param inputText
	 */
	private String getReplacement(String inputText) {
		if (charSubstitutionTable == null) {
			charSubstitutionTable = UnicodeUtil.create2CharSubstitutionTable();
		}
		// strip whitespace
		String replText = inputText.replaceAll( "\\s", "" );
		if (replText.length() == 3) {
			String s = (String)charSubstitutionTable.get( replText.substring( 1, 3 ) );
			if (s == null) {
				s = (String)charSubstitutionTable.get( replText.substring( 0, 2 ) );
				if (s != null) {
					s = s + replText.substring( 2, 3 );
				}
			}
			else {
				s = replText.substring( 0, 1 ) + s;
			}
			return s;
		}
		return null;
	}

}
