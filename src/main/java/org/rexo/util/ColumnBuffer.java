/*
 * Copyright 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Jul 25, 2005 by atolopko
 */
package org.rexo.util;

/**
 * Generates a string comprised of columns (fields), where each column has a
 * fixed width and text is padded as necessary. Like StringBuffer, but appended
 * strings are column entries, each with a specified width. Text will be
 * truncated to fit column width. This class will be obsolete with Java5's
 * printf function.
 * @author atolopko
 */
public class ColumnBuffer
{
	public static final int JUSTIFY_LEFT = 0;
	public static final int JUSTIFY_RIGHT = 1;
	
	private StringBuffer _buf = new StringBuffer();
	
	public ColumnBuffer() {}
	
	public ColumnBuffer appendColumn( String text, 
	                                  int width, 
	                                  int justification ) 
	{
		String padding = 
			StringUtils.
			makeRepeatedString( " ",
			                    Math.max( 1, width - text.length() ) );
		if ( justification == JUSTIFY_RIGHT ) {
			_buf.append( padding );
		}
		_buf.append( text.substring( 0, 
		                             Math.min( width - 1, 
		                                       text.length() ) ) );
		if ( justification == JUSTIFY_LEFT ) {
			_buf.append( padding );
		}
		return this;
	}
	
	public ColumnBuffer append( String text, int width ) 
	{
		return appendColumn( text, width, JUSTIFY_LEFT );
	}
	
	public ColumnBuffer append( int val, int width )
	{
		return appendColumn( Integer.toString( val ), width, JUSTIFY_LEFT );
	}
	
	public ColumnBuffer append( Number val, int width )
	{
		return appendColumn( val.toString(), width, JUSTIFY_LEFT );
	}
	
	public ColumnBuffer append( String text, int width, int justification ) 
	{
		return appendColumn( text, width, justification );
	}
	
	public ColumnBuffer append( int val, int width, int justification )
	{
		return appendColumn( Integer.toString( val ), width, justification );
	}
	
	public ColumnBuffer append( Number val, int width, int justification )
	{
		return appendColumn( val.toString(), width, justification );
	}
	
	public String toString() 
	{
		return _buf.toString();
	}
}
