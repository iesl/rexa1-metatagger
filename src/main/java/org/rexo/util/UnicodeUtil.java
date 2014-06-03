package org.rexo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: saunders Created Nov 7, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class UnicodeUtil {

	public static Map create2CharSubstitutionTable() {
		HashMap hashMap = new HashMap();
		for (int i = 0; i < bigramReplacements.length; i++) {
			String[] br = bigramReplacements[i];
			String ascii = br[0];
			String mods = br[1];
			String subs = br[2];
			for (int j = 0; j < mods.length(); j++) {
				char c = mods.charAt( j );
				hashMap.put( ascii + c, subs.substring( j, j+1 ) );
				hashMap.put( c + ascii, subs.substring( j, j+1 ) );
			}
		}
		return hashMap;
	}


	static String[][] bigramReplacements = new String[][]{
	    { "A", "`^~¨´º", "ÀÂÃÄÁÅ" },
	    { "a", "`^~¨´º", "àâãäáå" },

	    { "E", "`^¨´", "ÈÊËÉ" },
	    { "e", "`^¨´", "èêëé" },

	    { "I", "`^¨´", "ÌÎÏÍ" },
	    { "i", "`^¨´", "ìîïí" },

	    { "O", "`^~¨´", "ÒÔÕÖÓ" },
	    { "o", "`^~¨´", "òôõöó" },

	    { "U", "`^¨´", "ÚÛÜÙ" },
	    { "u", "`^¨´", "úûüù" },

	    { "C", "¸", "Ç" },
	    { "c", "¸", "ç" },
	    { "N", "~", "Ñ" },
	    { "n", "~", "ñ" },

	    { "Y", "¨´", "YÝ" }, // TODO: is there a representation of capital Y-umlaut? It's not in Latin-1 extended.
	    { "y", "¨´", "ÿý" },
	};

	// {char, decimal-code, hex-code, entity}
	static String[][] unicodeChars = new String[][]{
			{"¢", "162", "a2", "&cent;"},
	    {"¤", "164", "a4", "&curren;"},
	    {"¦", "166", "a6", "&brvbar;"},
	    {"¨", "168", "a8", "&uml;"},
	    {"ª", "170", "aa", "&ordf;"},
	    {"¬", "172", "ac", "&not;"},
	    {"®", "174", "ae", "&reg;"},
	    {"·", "176", "b0", "&deg;"},
	    {"²", "178", "b2", "&sup2;"},
	    {"´", "180", "b4", "&acute;"},
	    {"¶", "182", "b6", "&para;"},
	    {"¸", "184", "b8", "&cedil;"},
	    {"º", "186", "ba", "&ordm;"},
	    {"¼", "188", "bc", "&frac14;"},
	    {"¾", "190", "be", "&frac34;"},
	    {"À", "192", "c0", "&Agrave;"},
	    {"Â", "194", "c2", "&Acirc;"},
	    {"Ä", "196", "c4", "&Auml;"},

	    {"Æ", "198", "c6", "&AElig;"},
	    {"È", "200", "c8", "&Egrave;"},
	    {"Ê", "202", "ca", "&Ecirc;"},
	    {"Ì", "204", "cc", "&Igrave;"},
	    {"Î", "206", "ce", "&Icirc;"},
	    {"Ð", "208", "d0", "&ETH;"},
	    {"Ò", "210", "d2", "&Ograve;"},
	    {"Ô", "212", "d4", "&Ocirc;"},
	    {"Ö", "214", "d6", "&Ouml;"},
	    {"Ø", "216", "d8", "&Oslash;"},
	    {"Ú", "218", "da", "&Uacute;"},
	    {"Ü", "220", "dc", "&Uuml;"},
	    {"Þ", "222", "de", "&THORN;"},
	    {"à", "224", "e0", "&agrave;"},
	    {"â", "226", "e2", "&acirc;"},
	    {"ä", "228", "e4", "&auml;"},
	    {"æ", "230", "e6", "&aelig;"},
	    {"è", "232", "e8", "&egrave;"},
	    {"ê", "234", "ea", "&ecirc;"},
	    {"ì", "236", "ec", "&igrave;"},
	    {"î", "238", "ee", "&icirc;"},
	    {"ð", "240", "f0", "&eth;"},
	    {"ò", "242", "f2", "&ograve;"},
	    {"ô", "244", "f4", "&ocirc;"},
	    {"ö", "246", "f6", "&ouml;"},
	    {"ø", "248", "f8", "&oslash;"},
	    {"ú", "250", "fa", "&uacute;"},
	    {"ü", "252", "fc", "&uuml;"},
	    {"þ", "254", "fe", "&thorn;"},
	    {"¡", "161", "a1", "&iexcl;"},
	    {"£", "163", "a3", "&pound;"},
	    {"¥", "165", "a5", "&yen;"},
	    {"§", "167", "a7", "&sect;"},
	    {"©", "169", "a9", "&copy;"},
	    {"«", "171", "ab", "&laqno;"},
	    {"­", "173", "ad", "&shy;"},
	    {"¯", "175", "af", "&macr;"},
	    {"±", "177", "b1", "&plusmn;"},
	    {"³", "179", "b3", "&sup3;"},
	    {"µ", "181", "b5", "&micro;"},
	    {"·", "183", "b7", "&middot;"},
	    {"¹", "185", "b9", "&sup1;"},
	    {"»", "187", "bb", "&raquo;"},
	    {"½", "189", "bd", "&frac12;"},
	    {"¿", "191", "bf", "&iquest;"},
	    {"Á", "193", "c1", "&Aacute;"},
	    {"Ã", "195", "c3", "&Atilde;"},
	    {"Å", "197", "c5", "&Aring;"},
	    {"Ç", "199", "c7", "&Ccedil;"},
	    {"É", "201", "c9", "&Eacute;"},
	    {"Ë", "203", "cb", "&Euml;"},
	    {"Í", "205", "cd", "&Iacute;"},
	    {"Ï", "207", "cf", "&Iuml;"},
	    {"Ñ", "209", "d1", "&Ntilde;"},
	    {"Ó", "211", "d3", "&Oacute;"},
	    {"Õ", "213", "d5", "&Otilde;"},
	    {"×", "215", "d7", "&times;"},
	    {"Ù", "217", "d9", "&Ugrave;"},
	    {"Û", "219", "db", "&Ucirc;"},
	    {"Ý", "221", "dd", "&Yacute;"},
	    {"ß", "223", "df", "&szlig;"},
	    {"á", "225", "e1", "&aacute;"},
	    {"ã", "227", "e3", "&atilde;"},
	    {"å", "229", "e5", "&aring;"},
	    {"ç", "231", "e7", "&ccedil;"},
	    {"é", "233", "e9", "&eacute;"},
	    {"ë", "235", "eb", "&euml;"},
	    {"í", "237", "ed", "&iacute;"},
	    {"ï", "239", "ef", "&iuml;"},
	    {"ñ", "241", "f1", "&ntilde;"},
	    {"ó", "243", "f3", "&oacute;"},
	    {"õ", "245", "f5", "&otilde;"},
	    {"÷", "247", "f7", "&divide;"},
	    {"ù", "249", "f9", "&ugrave;"},
	    {"û", "251", "fb", "&ucirc;"},
	    {"ý", "253", "fd", "&yacute;"},
	    {"ÿ", "255", "ff", "&yuml;"},
	};

}
