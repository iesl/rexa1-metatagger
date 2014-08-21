/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Mar 17, 2004
 * author: asaunders
 */

package org.rexo.ui;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rexo.referencetagging.NewHtmlTokenization;
import org.rexo.pipeline.ErrorLogFilter;
import org.rexo.pipeline.GrantExtractionFilter;
import org.rexo.pipeline.InfoLogFilter;
import org.rexo.pipeline.PipelineMetricsFilter;
import org.rexo.pipeline.ReferenceExtractionFilter;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxPipeline;
import org.rexo.store.MetaDataXMLDocument;
import org.rexo.util.EnglishDictionary;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.rexo.ghuang.segmentation.CRFBibliographySegmentor;

public class MetaTagger {
	private static Logger log = Logger.getLogger( MetaTagger.class );
	
	
	public static String HEADER_CRF = "extractors/Hdrs.crf.dat.gz";
	public static String REFERENCE_CRF = "extractors/Refs.crf.dat.gz";
	public static String BIBLIO_SEG_CRF = "extractors/Seg.crf.dat.gz";
	public static String DICT_FILE = "words.txt";
	
	private static File dataDir = null;

	private static boolean crfsExist() {
		boolean b = new File( dataDir, HEADER_CRF ).exists();
		b &= new File( dataDir, REFERENCE_CRF).exists();
		b &= new File( dataDir, BIBLIO_SEG_CRF ).exists();
		b &= new File( dataDir, BIBLIO_SEG_CRF ).exists();
		b &= new File( dataDir, DICT_FILE ).exists();
		return b; 
	}
	/* Construct the Metatagger pipeline from the given RxDocumentQueue and command
	 * line options */
	private static RxPipeline buildPipeline(Map argumentMap) throws IOException, ClassNotFoundException {
		RxPipeline pipeline = new RxPipeline();

		File hdrCrf = new File( dataDir, HEADER_CRF );
		File refCrf = new File( dataDir, REFERENCE_CRF  );
		File bibCrf = new File( dataDir, BIBLIO_SEG_CRF );

		// handle 'enable-log'
		boolean logp = argumentMap.get( "enable.log" ) != null;
		pipeline.getScope( "session" ).put( "log.boolean", Boolean.valueOf( logp ) );
		pipeline.getScope( "session" ).put( "log.directory", new File( "./log" ) );
		pipeline.getScope( "session" ).put( "sessionID.integer", new Integer( -1 ) );

		log.info( logp ? "Statistics logging enabled" : "Statistics logging disabled (default)" );

		pipeline
		.addStandardFilters();

		log.info( "loading biblio-segmentation crf" );
		ObjectInputStream ois = new ObjectInputStream( new GZIPInputStream(new BufferedInputStream( new FileInputStream( bibCrf ))));
		CRF4 crf = (CRF4)ois.readObject();
		ois.close();
		CRFBibliographySegmentor crfBibSegmentor = new CRFBibliographySegmentor( crf );

		pipeline.add( new edu.umass.cs.rexo.ghuang.segmentation.SegmentationFilter( crfBibSegmentor ) );

		pipeline
		.add( new GrantExtractionFilter() )
		.add( new ReferenceExtractionFilter( refCrf, hdrCrf ) )

		// .add( new CitationContextFilter() )
		// .add( new WriteAnnotatedXMLFilter() )
		// .add( new MetatagPostconditionTestFilter() )
		;

		if (logp) {
			pipeline
			// log document errors to '.list' and '.html'
			.addErrorFilters()
			.add( new ErrorLogFilter() )
			.addEpilogueFilters()
			.add( new InfoLogFilter() )
			.add( new PipelineMetricsFilter() );
		}

		return pipeline;
	}

	/** Run the meta-tagger pipeline */
	public static void main(String[] args) {

		try {
			LoggerConfigurator.configure( MetaTagger.class );
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
							.withLongOpt( "enable-log" )
							.withDescription( "enable logging" )
							.create() );

					getOptions().addOption( OptionBuilder
							.withLongOpt( "data-dir" )
							.withDescription( "path to rexa-textmill/data/ directory" )
							.hasArg()
							.isRequired()
							.create() );
				}

				protected Object parseOptions(CommandLine commandLine, Object object) {
					Map initProperties = (Map)object;

					if (commandLine.hasOption( "enable-log" )) {
						initProperties.put( "enable.log", "true" );
					}
					initProperties.put( "data.dir", commandLine.getOptionValue( "data-dir" ));
					return initProperties;
				}
			};

			initProperties = (Map)cli.parse( args, MetaTagger.class );

			dataDir = new File( (String)initProperties.get( "data.dir" ) );

			if ( ! crfsExist() ) {
				log.error( "Some resource files missing; check crfs/dictionary files; exiting..." );
				return;
			}
		}
		catch (Exception e) {
			log.error( e.getClass().getName() + ": " + e.getMessage() );
			if (cli != null) {
				log.info( "\n" + cli.getHelpString( MetaTagger.class ) );
			}
			return;
		}

		EnglishDictionary dictionary = EnglishDictionary.create( new File( dataDir, DICT_FILE ) );
		RxPipeline pipeline;
    try {
	    pipeline = buildPipeline( initProperties );
    } catch (Exception e1) {
	    log.error( e1.getClass().getName() + ": " + e1.getMessage() );
	    return;
    }

		BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
		String line;
		log.info( "begin" );
			
		try {

			while ((line=reader.readLine()) != null ) try {
				// format is input-filename -> output-filename
				String[] files = line.split( "->" );
				File infile = new File( files[0].trim() );
				File outfile = new File( files[1].trim() );
				log.info( infile.getPath() + " -> " + outfile.getPath()  );
				if ( infile.exists() ) {
					Document document = readInputDocument( infile );
					NewHtmlTokenization tokenization = NewHtmlTokenization.createNewHtmlTokenization( document, dictionary );
					RxDocument rdoc = new RxDocument(); 
					rdoc.setTokenization( tokenization );
					pipeline.execute( rdoc );
					log.info( "writing output file" );
					writeOutput( outfile, rdoc );
				}
				else {
					log.error( "File not found: " + infile.getPath() );
				}
			}
			catch (Throwable e) {
				log.error( e.getClass().getName() + ": " + e.getMessage() );
			}
		}
		catch (IOException e) {
			log.error( e.getClass().getName() + ": " + e.getMessage() );			
		}
	}
	
	
	private static Document readInputDocument(File infile) throws IOException {
		SAXBuilder saxBuilder = new SAXBuilder();
		BufferedInputStream is = new BufferedInputStream( new FileInputStream( infile ) );
		try {
			return saxBuilder.build( is );
		}
		catch (Exception e) {
			throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
		}
		finally {
			is.close();
		}
  }
	private static void writeOutput(File outputFile, RxDocument rdoc) {
		NewHtmlTokenization tokenization = rdoc.getTokenization();
		Map segmentations = (Map)rdoc.getScope( "document" ).get( "segmentation" );


		if (tokenization == null || segmentations == null) {
			log.error( "No xml content available for document " + rdoc.getId() );
			return;
		}

		FileOutputStream xmlOutputStream = null;

		try {
			MetaDataXMLDocument metaDocument = MetaDataXMLDocument.createFromTokenization( null, segmentations );
			Document document = metaDocument.getDocument();
			XMLOutputter output = new XMLOutputter( Format.getPrettyFormat() );
			xmlOutputStream = new FileOutputStream( outputFile );
			output.output( document, xmlOutputStream );
		}
		catch (IOException e) {
			log.info( "(xml writer) " + e.getClass().getName() + ": " + e.getMessage() );
		}
		finally {
			if (xmlOutputStream != null) {
				try {
					xmlOutputStream.close();
				}
				catch (IOException e) {
					log.error( e.getMessage() );
				}
			}
		}
	}

}
