/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 6, 2004
 * author: asaunders
 */

package org.rexo.util;

import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

  static public File generateCorpusResourceFile(File corpusRoot, File corpusPath, String extension) {
    return new File( getCorpusFilename( new File( corpusRoot, getCorpusFilename( corpusPath ) ) ) + extension );
  }

  private static final String[] _extensions = new String[]{
    "xml",
    "refs",
    "meta",
    "index",
    "tag",
    "coref",
    "0",
    "1",
  };


  static public URI resolveResourceURIToStratumURI(URI resourceURI) {
    String pathString = resourceURI.getPath();
    String reString = "\\.([^\\./]+)$";
    Pattern pattern = Pattern.compile( reString );
    Matcher matcher = pattern.matcher( pathString );
    while (matcher.find()) {
      String ext = matcher.group( 1 );
      if (ArrayUtils.contains( _extensions, ext )) {
        pathString = pathString.substring( 0, pathString.length() - (ext.length() + 1) );
        matcher = pattern.matcher( pathString );
      }
    }
    URI resolvedURI = null;
    try {
      resolvedURI = new URI( resourceURI.getScheme(), //protocol
                             resourceURI.getUserInfo(), // user infor
                             resourceURI.getHost(), // host
                             resourceURI.getPort(), // port
                             pathString, // file
                             resourceURI.getQuery(), // query
                             resourceURI.getFragment() // ref
      );
    }
    catch (URISyntaxException e) {
    }

    return resolvedURI;
  }

  static public String getCorpusFilename(File metaFile) {
    File parentFile = metaFile.getParentFile();
    String targetFilename = metaFile.getName();
    String[] filenameParts = targetFilename.split( "\\." );
    String filenameBase = "";
    int end = filenameParts.length - 1;
    for (; ArrayUtils.contains( _extensions, filenameParts[end] ); end--) {
      // empty
    }
    for (int i = 0; i < end; i++) {
      filenameBase += filenameParts[i] + ".";
    }
    filenameBase += filenameParts[end];
    return new File( parentFile, filenameBase ).getPath();
  }

  static public File getResourceFile(Class c, String filePath) {
    File file = null;
    URL resourceURL = c.getResource( filePath );
    if (resourceURL != null) {
      String filename = resourceURL.getFile();
      file = new File( filename );
    }
    return file;
  }

  static String[] _schemeStrings = new String[]{
    "http",
    "ftp",
    "file",
    "jdbc",
    "mysql",
  };

  public static URI fileToURI(String filePath, String protocol) throws URISyntaxException {
    if (File.separatorChar == '\\') {
      filePath = filePath.replaceAll( "\\\\", "/" );
    }
    filePath = ((protocol != null && !filePath.startsWith( "/" )) ? "/" : "") + filePath;
    URI uri = new URI( protocol, //protocol
                       null, // user infor
                       protocol == null ? null : "", // host
                       -1, // port
                       filePath, // file
                       null, // query
                       null // ref
    );
    return uri;
  }


  public static URI appendToURIPath(URI uri, String append) throws URISyntaxException {
    String combinedPath = new File( uri.getPath() + append ).getPath();
    if (File.separatorChar == '\\') {
      combinedPath = combinedPath.replaceAll( "\\\\", "/" );
    }
    combinedPath = ((uri.isAbsolute() && !combinedPath.startsWith( "/" )) ? "/" : "") + combinedPath;
    URI combinedURI = new URI( uri.getScheme(), //protocol
                               uri.getUserInfo(), // user infor
                               uri.getHost(), // host
                               uri.getPort(), // port
                               combinedPath, // file
                               uri.getQuery(), // query
                               uri.getFragment() // ref
    );
    return combinedURI;
  }

  public static URI appendToURI(URI uri, File file) throws URISyntaxException {
    URI fileURI = fileToURI( file.getPath(), null );
    return appendToURI( uri, fileURI );
  }

  public static URI appendToURI(URI uri, URI uri2) throws URISyntaxException {
    URI combinedPathURI = fileToURI( new File( uri.getPath(), uri2.getPath() ).getPath(), uri.getScheme() );
    String combinedPath = combinedPathURI.getPath();
    if (File.separatorChar == '\\') {
      combinedPath = combinedPath.replaceAll( "\\\\", "/" );
    }
    combinedPath = ((combinedPathURI.isAbsolute() && !combinedPath.startsWith( "/" )) ? "/" : "") + combinedPath;
    URI combinedURI = new URI( uri.getScheme(), //protocol
                               uri.getUserInfo(), // user infor
                               uri.getHost(), // host
                               uri.getPort(), // port
                               combinedPathURI.getPath(), // file
                               uri.getQuery(), // query
                               uri.getFragment() // ref
    );
    return combinedURI;
  }

  public static URI combineURIs2(URI absoluteURI, URI relativeURI) throws URISyntaxException {
    String combinedPath = new File( absoluteURI.getPath(), relativeURI.getPath() ).getPath();
    if (File.separatorChar == '\\') {
      combinedPath = combinedPath.replaceAll( "\\\\", "/" );
    }

    URI combinedURI = null;
    combinedURI = new URI( absoluteURI.getScheme(), //protocol
                           absoluteURI.getUserInfo(), // user infor
                           absoluteURI.getHost(), // host
                           absoluteURI.getPort(), // port
                           combinedPath, // file
                           relativeURI.getQuery(), // query
                           relativeURI.getFragment() // ref
    );
    return combinedURI;
  }

  public static URI mangledFileToURI(File file, String scheme) {
    String name = file.getPath();
    name = name.replaceAll( "\\\\", "/" );

    boolean foundScheme = false;
    // fix the scheme encoding (e.g., http:/ should be http://)
    for (int i = 0; i < _schemeStrings.length; i++) {
      String schemeString = _schemeStrings[i];
      Matcher matcher = Pattern.compile( schemeString + "://?" ).matcher( name );
      foundScheme = matcher.find( 0 );
      if (foundScheme) {
        name = matcher.replaceFirst( schemeString + "://" );
        break;
      }
    }
    // If there is no scheme specified in the file, use the supplied one:
    if (!foundScheme && scheme != null) {
      name = scheme + ":/" + name;
    }

    boolean isValidURL = false;
    String[] fileParts = name.split( "/" );
    URL url = null;
    int i = 0;
    for (i = 0; !isValidURL && i < fileParts.length; i++) {
      try {
        url = new URL( name );
      }
      catch (MalformedURLException e) {
        name = name.substring( fileParts[i].length() + 1 );
        continue;
      }
      isValidURL = true;
    }

    URI uri = null;
    if (isValidURL) {
      try {
        uri =
        new URI( url.getProtocol(), null, url.getHost(), url.getPort(), url.getFile(), url.getQuery(), url.getRef() );
      }
      catch (URISyntaxException e) {
        return null;
      }
    }

    return uri;
  }


  /**
   * Deletes all files and subdirectories under dir
   * @param dir
   * @return true if all deletions were successful
   */
  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir( new File( dir, children[i] ) );
        if (!success) {
          return false;
        }
      }
    }

    // The directory is now empty so delete it
    return dir.delete();
  }
}
