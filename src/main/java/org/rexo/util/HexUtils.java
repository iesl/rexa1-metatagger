/* Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jul 28, 2004
 * author: mikem
 * */

package org.rexo.util;

public class HexUtils
{
	private static final char HEX_DIGITS[] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static void append(StringBuffer buffer, byte b)
	{
		buffer.append(HEX_DIGITS[(b >> 4) & 0xF]);
		buffer.append(HEX_DIGITS[b & 0xF]);
	}
	
	public static String toString(byte b)
	{
		StringBuffer buffer = new StringBuffer();
		append(buffer, b);
		return buffer.toString();
	}
	
	public static byte toByte(char hex[], int offset)
	{
		return (byte) (toNibble(hex[offset]) << 4 | toNibble(hex[offset + 1]));
	}
	
	public static byte toByte(String hex, int offset)
	{
		return (byte) (toNibble(hex.charAt(offset)) << 4 | toNibble(hex.charAt(offset + 1)));
	}
	
	public static byte toNibble(char c)
	{
		if('0' <= c && c <= '9')
			return (byte) (c - '0');
		if('A' <= c && c <= 'F')
			return (byte) (0xA + (c - 'A'));
		if('a' <= c && c <= 'f')
			return (byte) (0xA + (c - 'a'));
		throw new IllegalArgumentException("Not a hex character: " + c);
	}
}
