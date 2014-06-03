package org.rexo.ui

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.pipeline.ErrorLogFilter;
import org.rexo.pipeline.GrantExtractionFilter;
import org.rexo.pipeline.InfoLogFilter;
import org.rexo.pipeline.PipelineMetricsFilter;
import org.rexo.pipeline.ReferenceExtractionFilter;
import org.rexo.pipeline.components.{RxPipeline, RxDocument}

import org.rexo.util.EnglishDictionary;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.rexo.ghuang.segmentation.CRFBibliographySegmentor;




object MetaTag extends Logger("MetaTag") {

	val HEADER_CRF = "extractors/Hdrs.crf.dat.gz"
	val REFERENCE_CRF = "extractors/Refs.crf.dat.gz"
	val BIBLIO_SEG_CRF = "extractors/Seg.crf.dat.gz"
	val DICT_FILE = "words.txt"
	
	val dataDir: File = new java.io.File("data")

	/* Construct the Metatagger pipeline from the given RxDocumentQueue and command
	 * line options */
	def buildPipeline(): RxPipeline = {
		val pipeline = new RxPipeline()

		val hdrCrf = new File( dataDir, HEADER_CRF )
		val refCrf = new File( dataDir, REFERENCE_CRF  )
		val bibCrf = new File( dataDir, BIBLIO_SEG_CRF )

		// handle 'enable-log'2
    val logp = true
    import scala.collection.JavaConverters._

    // val sessionScope: Map[String, ] = pipeline.getScope("session")
    // sessionScope.put( "log.boolean", logp)
    // sessionScope.put( "log.directory", new File( "./log" ) )
    // sessionScope.put( "sessionID.integer", new Integer( -1 ) )

		pipeline.addStandardFilters()


		info( "loading biblio-segmentation crf" )
		val ois = new ObjectInputStream( new GZIPInputStream(new BufferedInputStream( new FileInputStream( bibCrf ))))
		val crf = ois.readObject().asInstanceOf[CRF4]
		ois.close()
		val crfBibSegmentor = new CRFBibliographySegmentor( crf )

		pipeline.add( new edu.umass.cs.rexo.ghuang.segmentation.SegmentationFilter( crfBibSegmentor ) )

		pipeline
		.add( new GrantExtractionFilter() )
		.add( new ReferenceExtractionFilter( refCrf, hdrCrf ) )
		// .add( new CitationContextFilter() )
		// .add( new WriteAnnotatedXMLFilter() )
		// .add( new MetatagPostconditionTestFilter() )
		

		if (logp) {
			pipeline
			// log document errors to '.list' and '.html'
			.addErrorFilters()
			.add( new ErrorLogFilter() )
			.addEpilogueFilters()
			.add( new InfoLogFilter() )
			.add( new PipelineMetricsFilter() )
		}

		return pipeline
	}

	/** Run the meta-tagger pipeline */
	def main(args: Array[String]) {
		// val initProperties: Map = null
    // val cli: CommandLineOptions = null

		try {
			EnglishDictionary.setDefaultWordfile( new File( dataDir, DICT_FILE ) )
			val dictionary = EnglishDictionary.createDefault()
			
			val pipeline = buildPipeline()
			
			val reader = new BufferedReader( new InputStreamReader( System.in ) )
			var line: String = null
			info( "begin" )
			while ((line=reader.readLine()) != null ) {
				// format is input-filename -> output-filename
				val files = line.split( "->" )
				val infile = new File( files(0) )
				val outfile = new File( files(1).trim() )
				info( infile.getPath() + " -> " + outfile.getPath()  )
				if ( infile.exists() ) {
					val document = readInputDocument( infile )
					val tokenization = NewHtmlTokenization.createNewHtmlTokenization( document, dictionary )
					val rdoc = new RxDocument() 
					rdoc.setTokenization( tokenization )

					try {
						pipeline.execute( rdoc )
						info( "writing output file" )
				writeOutput( outfile, rdoc )
					}
					catch (Exception e) {
						error( e.getClass().getName() + ": " + e.getMessage() )						
					}
				}
				else {
					error( "File not found: " + infile.getPath() )
				}
			}
		}
		catch (Exception e) {
			error( e.getClass().getName() + ": " + e.getMessage() )
    }
	}
	
	
	//private static Document readInputDocument(File infile) throws IOException {
	//  SAXBuilder saxBuilder = new SAXBuilder()
	//  BufferedInputStream is = new BufferedInputStream( new FileInputStream( infile ) )
	//  try {
	//  	return saxBuilder.build( is )
	//  }
	//  catch (Exception e) {
	//  	throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage())
	//  }
	//  finally {
	//  	is.close()
	//  }
  //}
	//private static void writeOutput(File outputFile, RxDocument rdoc) {
	//  NewHtmlTokenization tokenization = rdoc.getTokenization()
	//  Map segmentations = (Map)rdoc.getScope( "document" ).get( "segmentation" )
  // 
  // 
	//  if (tokenization == null) {
	//  	error( "No xml content available for document " + rdoc )
	//  	return
	//  }
  // 
	//  FileOutputStream xmlOutputStream = null
  // 
	//  try {
	//  	MetaDataXMLDocument metaDocument = MetaDataXMLDocument.createFromTokenization( null, segmentations )
	//  	Document document = metaDocument.getDocument()
	//  	XMLOutputter output = new XMLOutputter( Format.getPrettyFormat() )
	//  	xmlOutputStream = new FileOutputStream( outputFile )
	//  	output.output( document, xmlOutputStream )
	//  }
	//  catch (IOException e) {
	//  	info( "(xml writer) " + e.getClass().getName() + ": " + e.getMessage() )
	//  }
	//  finally {
	//  	if (xmlOutputStream != null) {
	//  		try {
	//  			xmlOutputStream.close()
	//  		}
	//  		catch (IOException e) {
	//  			error( e.getMessage() )
	//  		}
	//  	}
	//  }
	//}


}


