/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Mar 17, 2004
 * author: asaunders
 */

package org.rexo.ui;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.rexo.pipeline.*;
import org.rexo.pipeline.components.RxDocumentDirectorySource;
import org.rexo.pipeline.components.RxDocumentSource;
import org.rexo.pipeline.components.RxPipeline;
import org.rexo.referencetagging.CitationContextFilter;
import org.rexo.referencetagging.SegmentationFilter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/* Note -- this modified version of the MetaTagger was previously used for
 * producing metatagged xml for hand labeling.  I modified it for testing the
 * new 'CitationContextFilter' class.  Previously 'ReferenceAnnotationFilter'
 * was used to extract references and produce the formatted output.  Now,
 * 'ReferenceExtractionFilter' is used and formatted output is produced at a
 * later stage, in 'WriteAnnotatedXMLFilter'.  'WriteAnnotatedXMLFilter'
 * currently does not do as good a job of formatting the output XML as
 * 'ReferenceAnnotationFilter' previously did, so if we need to generate more
 * data for hand-labeling then we may need to update 'WriteANnotatedXMLFilter'
 * with a more sophisticated formatter to fix this.
 * -APD
 */

public class TagToFile {
	private static Logger log = Logger.getLogger( TagToFile.class );

	/* Construct an RxDocumentQueue from the command line options */
	private static RxDocumentDirectorySource buildRxDocumentSource(HashMap argumentMap) {
		// create a new source queue
    String inputDirName = (String)argumentMap.get ( "input.directory" );
    String listFileName = (String)argumentMap.get ( "input.listfile" );
		//RxDocumentDirectorySource source = new RxDocumentDirectorySource (new File (inputDirName));
		RxDocumentDirectorySource source;
    if (listFileName == null)
      source = new RxDocumentDirectorySource (new File (inputDirName));
    else
      source = new RxDocumentDirectorySource (new File(listFileName), new File (inputDirName));

		// handle 'num-documents'
		if (argumentMap.get( "num.documents" ) != null) {
			long maxDocNum = ((Long)argumentMap.get( "num.documents" )).longValue();
			source.setMaxDocuments( maxDocNum );
			System.out.println( "Processing first " + maxDocNum + " documents only" );
		}
		else {
			System.out.println( "Processing all documents (default)" );
		}

		return source;
	}

	/* Construct the Metatagger pipeline from the given RxDocumentQueue and command
	 * line options */
	private static RxPipeline buildPipeline(HashMap argumentMap) {

		RxPipeline pipeline = new RxPipeline();

		File crfHeaderFile = null;
		File crfReferenceFile = null;

		// handle 'skip-crf'
		boolean skipCRF = argumentMap.get( "skip.crf" ) == null ? false : true;
		if (skipCRF) {
			System.out.println( "Skipping CRF extraction and meta output" );
		}
		// init CRF files and target directory
		else {
			String headSrc = (String)argumentMap.get( "crf.headers" );
			String refSrc = (String)argumentMap.get( "crf.references" );
			if (headSrc == null) {
				System.out.println( "No crf headers file specified.  Use --crf-headers [filename]." );
			}
			if (refSrc == null) {
				System.out.println( "No crf references file specified.  Use --crf-references [filename]." );
			}
			if (headSrc == null || refSrc == null) {
				System.exit( 0 );
			}
			crfHeaderFile = new File( headSrc );
			crfReferenceFile = new File( refSrc );
		}

    String outputDirName = (String) argumentMap.get ( "output.directory" );
    pipeline.getScope ( "session" ).put( "output.directory", new File (outputDirName) );

		// handle 'enable-log'
		boolean logp = argumentMap.get( "enable.log" ) == null ? false : true;
		pipeline.getScope( "session" ).put( "log.boolean", new Boolean( logp ) );
		pipeline.getScope( "session" ).put( "log.directory", new File( "./log" ) );
		// initialized in 'LogfileFactory'
		pipeline.getScope( "session" ).put( "sessionID.integer", new Integer( -1 ) );

		System.out.println( logp ? "Logging enabled" : "Logging disabled (default)" );

    boolean validatep = argumentMap.containsKey ("validate");
		// construct pipeline
		if (!skipCRF) {
			// standard pipeline
			pipeline
			    .addStandardFilters()
			    .add( new PipelineControlFilter( "tag" ) )
			    //.add( new MetatagPreconditionTestFilter() )
			    .add( new TimeoutFilter( new SegmentationFilter(), 4 ) );
      if (validatep) pipeline.add (new PartitioningValidationFilter ());
			pipeline
			    //          .add( new PrintFilter ())
			    .add( new ReferenceExtractionFilter( crfReferenceFile, crfHeaderFile ) )
			    .add( new CitationContextFilter() )
          ;
		}

		if (logp) {
			pipeline
			    // log document errors to '.list' and '.html'
			    .addErrorFilters()
			    .add( new ErrorLogFilter() )
			    //.add( new HtmlErrorLogFilter() )
			    // log document info to '.list' and create pipeline metrics file
			    .addEpilogueFilters()
			    .add( new InfoLogFilter() )
			    .add( new PipelineMetricsFilter() );
		}

		return pipeline;
	}

	/**
	 * Run the meta-tagger pipeline
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		HashMap argumentMap = new HashMap();
		try {
			CommandLine commandLine = _options.getCommandLine( args );
			_options.parseCommandLine( commandLine, argumentMap );
		}
		catch (ParseException e) {
			// generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "Rexo MetaTagger command line", _options.getOptions() );
			return;
		}

		// Init logger:
		BasicConfigurator.configure();

		// Create the pipeline
		RxPipeline pipeline = buildPipeline( argumentMap );

		// Create a source queue
		//RxDocumentQueue queue = buildSourceQueue(argumentMap);
		RxDocumentSource source = buildRxDocumentSource( argumentMap );

		pipeline.setInputSource( source );

    long start = System.currentTimeMillis ();
		System.out.print( "Starting pipeline: " );
		System.out.println( pipeline.toString() );
		pipeline.execute();

    long end = System.currentTimeMillis ();
    System.out.println ("Time used (ms) = "+(end-start));

	}

	 private static CommandLineOptions _options = new CommandLineOptions() {
 		protected void createOptions() {
 			getOptions().addOption( OptionBuilder
 			                        .withLongOpt( "validate" )
 			                        .create() );

 			getOptions().addOption( OptionBuilder
 			                        .withLongOpt( "input-directory" )
 			                        .hasArg()
 			                        .create() );

 			getOptions().addOption( OptionBuilder
 			                        .withLongOpt( "input-list" )
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

			getOptions().addOption( OptionBuilder
			                        .withLongOpt( "enable-log" )
			                        .withDescription( "enable logging" )
			                        .create() );
		} 

// 	private static CommandLineOptions _options = new CommandLineOptions() {
// 		protected void createOptions() {
// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "continuous" )
// 			                        .withDescription( "re-run on input directory until stop file is created" )
// 			                        .create() );

// 			// getOptions().addOption( OptionBuilder
// // 			                        .withLongOpt( "connection-url" )
// // 			                        .withDescription( "db connection (e.g., 'jdbc:mysql://localhost/rexo?user=***')" )
// // 			                        .hasArg()
// // 			                        .isRequired()
// // 			                        .create() ); 

// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "input-directory" )
// 			                        .hasArg()
// 						.withDescription( "input directory" )
// 			                        .create() );

// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "input-list" )
// 			                        .hasArg()
// 						.withDescription( "file containing list of file names to process" )
// 			                        .create() );

// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "crf-headers" )
// 			                        //.isRequired()
// 			                        .hasArg()
// 						.withDescription( "header crf file" )
// 			                        .create() );

// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "crf-references" )
// 			                        //.isRequired()
// 			                        .hasArg()
// 						.withDescription( "references crf file" )
// 			                        .create() );

// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "enable-log" )
// 						.withDescription( "enable logging" )
// 			                        .create() );

// 			getOptions().addOption( OptionBuilder
// 			                        .withLongOpt( "num-documents" )
// 			                        .hasArg()
// 			                        .withDescription( "Process only the specified number of documents before finishing" )
// 			                        .create() );

// 			// getOptions().addOption( OptionBuilder
// // 			                        .withLongOpt( "clean" )
// // 			                        .withDescription( "clear out all paper/reference/author objects in database" )
// // 			                        .create() ); 

// 		} conference

		public void parseCommandLine(CommandLine commandLine, Map argumentMap) {
			argumentMap.put( "crf.headers", commandLine.getOptionValue( "crf-headers" ) );
			argumentMap.put( "crf.references", commandLine.getOptionValue( "crf-references" ) );
			argumentMap.put( "input.directory", commandLine.getOptionValue( "input-directory" ) );
			argumentMap.put( "input.listfile", commandLine.getOptionValue( "input-list" ) );
			argumentMap.put( "output.directory", commandLine.getOptionValue( "output-directory" ) );
			if (commandLine.hasOption( "validate" )) {
				argumentMap.put( "validate", "true" );
			}
			if (commandLine.hasOption( "enable-log" )) { argumentMap.put( "enable.log", "true" ); }
		}
	};
}
