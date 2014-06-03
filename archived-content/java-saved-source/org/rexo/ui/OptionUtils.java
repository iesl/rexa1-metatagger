package org.rexo.ui;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.Map;

/**
 * Author: saunders Created Feb 14, 2006 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 * Utility class to construct/parse common sets of command line arguments
 */
public class OptionUtils {
	// Runtime lifecycle constants (used for jobs running in a parallel environment):
	public static final int UNKNOWN_STEP = 0;
	public static final int INIT_STEP = 1;
	public static final int RESET_STEP = 2;
	public static final int RESUME_STEP = 3;
	public static final int RUN_STEP = 4;
	public static final int CLEANUP_STEP = 5;

	public static int whichStage(Map initProperties) {
		final boolean doInitialize = ((Boolean)initProperties.get( "initialize" )).booleanValue();
		final boolean doReset = ((Boolean)initProperties.get( "reset" )).booleanValue();
		final boolean doResume = ((Boolean)initProperties.get( "resume" )).booleanValue();
		final boolean doCleanup = ((Boolean)initProperties.get( "cleanup" )).booleanValue();
		final boolean doRun = ((Boolean)initProperties.get( "run" )).booleanValue();
		return doInitialize ? INIT_STEP : doReset ? RESET_STEP : doResume ? RESUME_STEP : doCleanup ? CLEANUP_STEP : doRun ? RUN_STEP : UNKNOWN_STEP;
	}

	/**
	 */
	public static void createDBOptions(Options options) {
		options.addOption( OptionBuilder
			.withLongOpt( "use-db" )
			.hasArg()
			.withArgName( "database" )
			.withDescription( "database to use" )
			.isRequired()
			.create() );

		options.addOption( OptionBuilder
			.withLongOpt( "db-version" )
			.withDescription( "" )
			.isRequired()
			.hasArg()
			.withType( String.class )
			.create() );

	}
	public static void parseDBOptions(CommandLine commandLine, Map initProperties) {
		initProperties.put( "use.db", commandLine.getOptionValue( "use-db" ) );
		initProperties.put( "db.version", commandLine.getOptionValue( "db-version" ) );
	}

	/**
	 */
	public static void createLifecycleOptions(Options options) {
		options.addOption( OptionBuilder
			.withLongOpt( "initialize" )
			.create() );

		options.addOption( OptionBuilder
			.withLongOpt( "reset" )
			.create() );

		options.addOption( OptionBuilder
			.withLongOpt( "resume" )
			.create() );

		options.addOption( OptionBuilder
			.withLongOpt( "run" )
			.create() );

		options.addOption( OptionBuilder
			.withLongOpt( "cleanup" )
			.create() );

	}
	public static void parseLifecycleOptions(CommandLine commandLine, Map initProperties) {
		initProperties.put( "initialize", Boolean.valueOf( commandLine.hasOption( "initialize" ) ) );
		initProperties.put( "reset", Boolean.valueOf( commandLine.hasOption( "reset" ) ) );
		initProperties.put( "resume", Boolean.valueOf( commandLine.hasOption( "resume" ) ) );
		initProperties.put( "run", Boolean.valueOf( commandLine.hasOption( "run" ) ) );
		initProperties.put( "cleanup", Boolean.valueOf( commandLine.hasOption( "cleanup" ) ) );
	}

	public static void createVerbosityOptions(Options options) {
		options.addOption( OptionBuilder
			.withLongOpt( "verbose" )
			.create( "v" ) );
	}
	
	public static void parseVerbosityOptions(CommandLine commandLine, Map initProperties) {
		int verbosityLevel = 0;
		Option[] options = commandLine.getOptions();
		for (int i = 0; i < options.length; i++) {
			Option option = options[i];
			if ( "v".equals( option.getOpt() ) ) {
				verbosityLevel++;
			}
		}
		initProperties.put( "verbosity.level", new Integer( verbosityLevel ) );
	}

}
