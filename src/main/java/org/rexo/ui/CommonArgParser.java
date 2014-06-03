/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 27, 2005
 * author: saunders
 */

package org.rexo.ui;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommonArgParser {

	

	public static void main(String[] args) {

		Map initProperties = new HashMap();

		try {
			new CommandLineOptions() {
				protected void createOptions() {

					getOptions().addOption( OptionBuilder
					                        .withLongOpt( "db-version" )
					                        .withDescription( "table version" )
					                        .withArgName( "version-string" )
					                        .withDescription( "specify database version" )
					                        .hasArg()
					                        .isRequired()
					                        .create() );
				}

				protected Object parseOptions(CommandLine commandLine, Object object) {
					Map initProperties = (Map)object;
					initProperties.put( "db-version", commandLine.getOptionValue( "db-version" ) );
					initProperties.put( "cluster.count", Boolean.valueOf( commandLine.hasOption( "cluster-count" ) ) );
					initProperties.put( "file", Boolean.valueOf( commandLine.hasOption( "file" ) ) );
					initProperties.put( "filename", commandLine.getOptionValue( "file" ) );
					return null;
				}
			}.parse( args, initProperties );
		}
		catch (Exception e) {
			return;
		}

		BasicConfigurator.resetConfiguration();

		Properties log4jProperties = new Properties();
		try {
			log4jProperties.load( CommonArgParser.class.getResourceAsStream( "log4j.properties" ) );
			log4jProperties.remove( "log4j.appender.R.File" );
			log4jProperties.setProperty( "log4j.appender.R.File", "CommonArgParser.log" );
			PropertyConfigurator.configure( log4jProperties );
		}
		catch (IOException e) {
		}
	}
}
