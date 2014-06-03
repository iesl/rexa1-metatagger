package org.rexo.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * A simple InputStream wrapper class that removes '\r' characters.
 * */
public class DOSInputStream extends InputStream
{
	private InputStream wrap;
	
	public DOSInputStream(InputStream wrap)
	{
		this.wrap = wrap;
	}
	
	public int read() throws IOException
	{
		int value = wrap.read();
		while(value == '\r')
			value = wrap.read();
		return value;
	}
	
	public void close() throws IOException
	{
		wrap.close();
	}
	
	/* Overriding the available() method would be nice, but there is no way
	 * to predict what upcoming bytes will be skipped because they are \r.
	 * Thus to do it right, we'd have to read wrap.available() bytes into a
	 * buffer and find out, which could be very expensive. */
}
