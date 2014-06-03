package org.rexo.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * This class translates between URLs and local filesystem paths.
 * <p>
 * A URL is converted to a filesystem path in the following manner. First, all
 * forward slashes are converted to backslashes to make the URL a valid name for
 * a file (on UNIX anyway). Then, the host part of the URL is extracted and a
 * directory path is constructed by using the first letter of the "significant"
 * part of the host name, and the full hostname. The significant part of a host
 * name is the portion that corresponds to the organization to which it is
 * registered; this is determined by a heuristic built into this class.
 * <p>
 * For example, the URL http://www.cs.umass.edu/~mccallum would be translated
 * into the filesystem path u/www.cs.umass.edu/http:\\www.cs.umass.edu\~mccallum
 * <p>
 * The reverse translation simply extracts the file name from the path, and
 * converts the backslashes back to forward slashes.
 * */
public class URLMangler
{
	private URLMangler()
	{
	}
	
	private static char getSignificantLetter(String host)
	{
		/* break the host part of the URL into an array {com, foobar, www} */
		StringTokenizer tokens = new StringTokenizer(host, ".");
		int length = tokens.countTokens();
		String bits[] = new String[length];
		
		for(int i = 0; i < length; i++)
			bits[length - i - 1] = tokens.nextToken();
		
		/* just a top-level domain?? */
		if(bits.length < 2)
			return '?';
		
		/* in the .edu domain, return the first char of the domain name */
		/* i.e. assume something of the form www.cs.umass.edu, we want "umass" */
		if("edu".equals(bits[0]))
			return bits[1].charAt(0);
		
		/* if we are in a country code (2 letter codes), go deeper */
		/* i.e. assume something of the form www.ox.ac.uk, we want "ox" */
		if(bits[0].length() == 2 && length > 2)
		{
			/* only use this if the length at least as large */
			if(bits[2].length() >= bits[1].length())
				return bits[2].charAt(0);
		}
		
		/* otherwise, take the first char of the domain name */
		/* i.e. assume something of the form www.research.com, we want "research" */
		return bits[1].charAt(0);
	}
	
	public static File mangle(URL url)
	{
		String host = url.getHost();
		char letter = getSignificantLetter(host);
		String file = url.toString().replace('/', '\\');
		
		return new File(letter + File.separator + host, file);
	}
	
	public static URL demangle(File file) throws MalformedURLException
	{
		String name = file.getName();
		return new URL(name.replace('\\', '/'));
	}
}
