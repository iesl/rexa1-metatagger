/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Feb 15, 2004
 * author: asaunders, ghuang
 */

package org.rexo.extraction;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.rexo.pipeline.*;
import org.rexo.pipeline.components.RxDocumentDirectorySource;
import org.rexo.pipeline.components.RxDocumentSource;
import org.rexo.pipeline.components.RxPipeline;
import org.rexo.referencetagging.SegmentationFilter;

import java.io.File;

public class GrantExtractorTest
{
	private static Logger log = Logger.getLogger( GrantExtractorTest.class );


	/* Construct an RxDocumentQueue from the command line options */
	private static RxDocumentDirectorySource buildRxDocumentSource (String inputDir)
	{
		// create a new source queue
		System.out.println( "inputDir: " + inputDir );
		RxDocumentDirectorySource source = new RxDocumentDirectorySource( new File( inputDir ) );

		return source;
	}

	/* Construct the Metatagger pipeline from the given RxDocumentQueue and command
	 * line options */
	private static RxPipeline buildPipeline (String outputDir)
	{
		RxPipeline pipeline = new RxPipeline();

		pipeline.getScope ( "session" ).put( "output.directory", new File (outputDir) );

		// handle 'enable-log'
		pipeline.getScope( "session" ).put( "log.boolean", new Boolean( true ) );
		pipeline.getScope( "session" ).put( "log.directory", new File( outputDir ) );
		// initialized in 'LogfileFactory'
		pipeline.getScope( "session" ).put( "sessionID.integer", new Integer( -1 ) );

		// construct pipeline
		pipeline
			.addStandardFilters()
			.add( new PipelineControlFilter( "tag" ) )
			.add( new TimeoutFilter( new SegmentationFilter(), 4 ) )
			.add( new GrantExtractionFilter() )
			.addErrorFilters()
			.add( new ErrorLogFilter() )
			.addEpilogueFilters()
			.add( new InfoLogFilter() )
			.add( new PipelineMetricsFilter() )
			;

		return pipeline;
	}

	/**
	 * Run the grant extractor on files in the given directory
	 */
	public static void main(String[] argv)
	{
		if (argv.length != 2) {
			System.out.println("usage: GrantExtractorTest inputDir outputDir");
			System.exit(0);
		}

		// Init logger:
		BasicConfigurator.configure();

		// Create the pipeline
		RxPipeline pipeline = buildPipeline( argv[1] );

		// Create a source queue
		RxDocumentSource source = buildRxDocumentSource( argv[0] );

		pipeline.setInputSource( source );

		System.out.print( "Starting grant extraction: " );
		System.out.println( pipeline.toString() );
		pipeline.execute();
	}
}
