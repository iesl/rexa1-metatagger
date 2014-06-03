package org.rexo.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A replacement for {@link BufferedReader} that reads Unix-style lines.
 * 
 * The normal <code>BufferedReader.readLine()</code> method will break lines on
 * three different types of line endings: a carriage return, a line feeed, or a
 * carriage return followed immediately be a line feed (CR, LF, or CRLF).
 * 
 * This may not always be the desired behavior, for instance if carraige returns
 * are used as control characters. This class is a drop-in replacement for
 * {@link BufferedReader} that overrides the <code>readLine()</code> method to
 * break lines only on line feeds. Carriage returns are treated as any other
 * character and will be included in the returned {@link String} if encountered.
 * */

public class BufferedUnixReader extends BufferedReader
{
	public BufferedUnixReader(Reader in)
	{
		super(in);
	}
	
	public BufferedUnixReader(Reader in, int sz)
	{
		super(in, sz);
	}
	
	/**
	 * Read a line of text.
	 * 
	 * A line is considered to be terminated by a line feed ('\n').
	 * 
	 * @return A {@link String} containing the contents of the line, not
	 *         including the line feed, or <code>null</code> if the end of
	 *         the stream has been reached.
	 * */
	public String readLine() throws IOException
	{
		StringBuffer line = new StringBuffer();
		int ch = read();
		
		while(ch != '\n' && ch != -1)
		{
			line.append((char) ch);
			ch = read();
		}
		
		if(line.length() > 0 || ch == '\n')
			return line.toString();
		
		return null;
	}
}
