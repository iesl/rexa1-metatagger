/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Mar 17, 2004
 * author: asaunders
 */

package org.rexo.ui;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

public abstract class CommandLineOptions {
	private static Logger log = Logger.getLogger( CommandLineOptions.class );

	private Options _options = new Options();

	/**
	 *
	 */
	protected CommandLineOptions() {
		createOptions();
	}

	/**
	 * @return
	 */
	public Options getOptions() {
		return _options;
	}

	/**
	 *
	 */
	protected abstract void createOptions();

	public CommandLine getCommandLine(String[] args) throws ParseException {
		return new PosixParser().parse( _options, args );
	}

	/**
	 *
	 * @return
	 */
	public String getHelpString(Class clazz) {
		String classname = "<Classname>";
		if ( clazz != null ) {
			classname = clazz.getName();
		}
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter( stringWriter, true );
		formatter.printHelp( /* PrintWriter pw */ printWriter,
		                                          /* int width,             */ 250,
		                                          /* String cmdLineSyntax,  */ classname,
		                                          /* String header,         */ "----- Options: ",
		                                          /* Options options,       */ _options,
		                                          /* int leftPad,           */ 5,
		                                          /* int descPad,           */ 5,
		                                          /* String footer,         */ "---------------",
		                                          /* boolean autoUsage      */ true );
		return stringWriter.toString();
	}

	/**
	 *
	 * @param args
	 * @param o
	 * @return
	 * @throws ParseException
	 */
	public Object parse(String[] args, Object o) throws ParseException {
		CommandLine commandLine = getCommandLine( args );
		return parseOptions( commandLine, o );
	}

	/**
	 * parse options, taking them from the environment first, then the command line
	 */
	public Object parse(String[] args, Class clazz) throws ParseException {
		String[] envArgs = getEnvArgs( clazz );
		String[] combinedArgs = new String[args.length + envArgs.length];
		int i = 0;
		for (; i < envArgs.length; i++) {
			combinedArgs[i] = envArgs[i];
		}
		for (int j = 0; j < args.length; j++) {
			combinedArgs[i + j] = args[j];
		}

		CommandLine commandLine = getCommandLine( combinedArgs );
		Map argMap = new HashMap();
		return parseOptions( commandLine, argMap );
	}

	/**
	 *
	 * @param commandLine
	 * @param object
	 * @return
	 */
	protected Object parseOptions(CommandLine commandLine, Object object) {
		return null;
	}

	public void parseCommandLine(CommandLine commandLine, Map argumentMap) throws ParseException {

	}

	private static final String ARG_RE = "\\.arg\\.";
	private static Pattern ARG_SEP_PATTERN = Pattern.compile( ARG_RE );


	/**
	 * Get arguments from the system environment. Arguments must follow a naming convention to be recognized as arguments
	 * to a particular class. Convention is:
	 * <p/>
	 * <i>key = qualified.package.or.classname.arg.N.param.string </i><br/> <i>value = any string...</i>
	 * <p/>
	 * Package name may be specified, rather than fully qualified classname, to apply arguments to any class in the
	 * specified package. Multiple args with the same number order <b>N</b> are given to the class on the command line in
	 * an unspecifier order
	 * <p/>
	 * Examples:
	 * <p/>
	 * Environment<br/> <code>org.rexo.ui.MyClass.arg.1.db.version=v011</code> <br/> would translate to <br/>
	 * <code>org.rexo.ui.MyClass --db-version v011</code>
	 * <p/>
	 * Environment<br/>  <code>org.rexo.arg.0.do.init=''</code> <br/> would translate to <br/>
	 * <code>org.rexo.any.package.MyClass --do-init</code>
	 */
	public static String[] getEnvArgs(Class clazz) {
		String className = clazz.getName();
		ArrayList argList = new ArrayList();
		Properties properties = System.getProperties();
		Enumeration keyEnum = properties.keys();

		while (keyEnum.hasMoreElements()) {
			String propKey = (String)keyEnum.nextElement();

			if (ARG_SEP_PATTERN.matcher( propKey ).find()) {
				String[] packageArgSpec = propKey.split( ARG_RE );
				String packageSpec = packageArgSpec[0]; // e.g. 'org.rexo'
				String argSpec = packageArgSpec[1]; // e.g., 1.db.version
				if (className.startsWith( packageSpec )) {
					String[] orderedArg = argSpec.split( "\\.", 2 );
					Integer order = new Integer( orderedArg[0] );
					String argName = "--" + orderedArg[1].replaceAll( "\\.", "-" );
					String argValue = System.getProperty( propKey );
					log.info( "env arg: " + argName + " " + argValue );
					Object[] argRec = new Object[]{order, argName, argValue};
					argList.add( argRec );
				}
			}
		}

		// Sort args into order specified in property
		Collections.sort( argList, new Comparator() {
			public int compare(Object o1, Object o2) {
				Comparable c1 = (Comparable)((Object[])o1)[0];
				Object c2 = ((Object[])o2)[0];
				return c1.compareTo( c2 );
			}
		} );

		ArrayList args = new ArrayList();
		for (int i = 0; i < argList.size(); i++) {
			Object[] argRec = (Object[])argList.get( i );
			args.add( argRec[1] );
			if (((String)argRec[2]).length() > 0) {
				args.add( argRec[2] );
			}
		}
		return (String[])args.toArray( new String[args.size()] );
	}

	public static String[] getEnvArgsOldVersion(Class clazz) {
		String className = clazz.getName();
		ArrayList argList = new ArrayList();
		Properties properties = System.getProperties();
		Enumeration keyEnum = properties.keys();

		while (keyEnum.hasMoreElements()) {
			String key = (String)keyEnum.nextElement();
			if (key.startsWith( className )) {
				String argSpec = key.substring( className.length() );
				if (argSpec.length() > 0 && argSpec.startsWith( ".arg." )) {
					argSpec = argSpec.substring( ".arg.".length() );
					String[] orderedArg = argSpec.split( "\\.", 2 );
					Integer order = new Integer( orderedArg[0] );
					String argName = "--" + orderedArg[1].replaceAll( "\\.", "-" );
					String argValue = System.getProperty( key );
					Object[] argRec = new Object[]{order, argName, argValue};
					argList.add( argRec );
				}
			}
		}

		// Sort args into order specified in property
		Collections.sort( argList, new Comparator() {
			public int compare(Object o1, Object o2) {
				Comparable c1 = (Comparable)((Object[])o1)[0];
				Object c2 = ((Object[])o2)[0];
				return c1.compareTo( c2 );
			}
		} );

		ArrayList args = new ArrayList();
		for (int i = 0; i < argList.size(); i++) {
			Object[] argRec = (Object[])argList.get( i );
			args.add( argRec[1] );
			if (((String)argRec[2]).length() > 0) {
				args.add( argRec[2] );
			}
		}
		return (String[])args.toArray( new String[args.size()] );
	}

	public static class IllegalOptionsException extends Exception {
		public IllegalOptionsException(String msg) {
			super( msg );
		}

		public IllegalOptionsException(Throwable cause) {
			super( cause );
		}

		public IllegalOptionsException(String msg, Throwable cause) {
			super( msg, cause );
		}

		public void setUsage(String usage) {

		}
	}

}
