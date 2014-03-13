package org.rexo.ui;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.rexo.pipeline.AuthorEMailTaggingFilter;
import org.rexo.pipeline.components.RxDocumentQueue;
import org.rexo.pipeline.components.RxDocumentSource;
import org.rexo.pipeline.components.RxPipeline;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/** Author: saunders Created Apr 19, 2006 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept. */
public class AuthorEMailTagger {
	private static Logger log = Logger.getLogger( AuthorEMailTagger.class );

	/* Construct an RxDocumentQueue from the command line options */
	private static RxDocumentSource buildRxDocumentSource(Map argumentMap) {
		String inputDirName = (String)argumentMap.get( "input.directory" );
		log.info( "dirName: " + inputDirName );
		RxDocumentQueue documentQueue = new RxDocumentQueue( new File( inputDirName ) );
		documentQueue.setExtensions( new String[]{".crf.xml"} );
		return documentQueue;
	}


	private static RxPipeline buildPipeline(Map argumentMap) {
		RxPipeline pipeline = new RxPipeline();

		if (argumentMap.containsKey( "output.directory" )) {
			String outputDirName = (String)argumentMap.get( "output.directory" );
			pipeline.getScope( "session" ).put( "output.directory", new File( outputDirName ) );
		}

		if (argumentMap.containsKey( "input.directory" )) {
			String dirName = (String)argumentMap.get( "input.directory" );
			pipeline.getScope( "session" ).put( "input.directory", new File( dirName ) );
		}

		// construct pipeline
		// standard pipeline
		pipeline
				.addStandardFilters()
				.add( new AuthorEMailTaggingFilter() )
				;

		return pipeline;
	}

	/** Run the meta-tagger pipeline */
	public static void main(String[] args) {

		try {
			LoggerConfigurator.configure( AuthorEMailTagger.class );
		}
		catch (IOException e) {
			System.out.println( e.getMessage() );
		}

		Map initProperties;
		CommandLineOptions cli = null;

		try {
			cli = new CommandLineOptions() {
				protected void createOptions() {
					getOptions().addOption( OptionBuilder
							.withLongOpt( "input-directory" )
							.hasArg()
							.create() );

					getOptions().addOption( OptionBuilder
							.withLongOpt( "output-directory" )
							.hasArg()
							.create() );

					getOptions().addOption( OptionBuilder
							.withLongOpt( "crf-headers" )
							.hasArg()
							.create() );

					getOptions().addOption( OptionBuilder
							.withLongOpt( "crf-references" )
							.hasArg()
							.create() );
				}

				protected Object parseOptions(CommandLine commandLine, Object object) {
					Map initProperties = new HashedMap();

					initProperties.put( "crf.headers", commandLine.getOptionValue( "crf-headers" ) );
					initProperties.put( "crf.references", commandLine.getOptionValue( "crf-references" ) );
					initProperties.put( "input.directory", commandLine.getOptionValue( "input-directory" ) );
					initProperties.put( "output.directory", commandLine.getOptionValue( "output-directory" ) );

					return initProperties;
				}
			};

			initProperties = (Map)cli.parse( args, AuthorEMailTagger.class );
		}
		catch (Exception e) {
			log.error( e.getClass().getName() + ": " + e.getMessage() );
			if (cli != null) {
				log.info( "\n" + cli.getHelpString( AuthorEMailTagger.class ) );
			}
			return;
		}

		// Create the pipeline
		RxPipeline pipeline = buildPipeline( initProperties );

		// Create a source queue
		//RxDocumentQueue queue = buildSourceQueue(argumentMap);
		RxDocumentSource source = buildRxDocumentSource( initProperties );

		pipeline.setInputSource( source );

		System.out.print( "Starting pipeline: " );
		System.out.println( pipeline.toString() );
		pipeline.execute();
	}
}
