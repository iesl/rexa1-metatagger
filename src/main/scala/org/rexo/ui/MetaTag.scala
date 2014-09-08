package org.rexo.ui

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.util.{Map,HashMap}
import java.util.zip.GZIPInputStream

import org.slf4j.{Logger, LoggerFactory}

import org.rexo.extraction.NewHtmlTokenization
import org.rexo.pipeline.ErrorLogFilter
import org.rexo.pipeline.GrantExtractionFilter
import org.rexo.pipeline.InfoLogFilter
import org.rexo.pipeline.PipelineMetricsFilter
import org.rexo.pipeline.BodyExtractionFilter
import org.rexo.pipeline.ReferenceExtractionFilter
import org.rexo.pipeline.components.{RxPipeline, RxDocument}

import org.rexo.store.MetaDataXMLDocument

import org.rexo.util.EnglishDictionary


import edu.umass.cs.mallet.base.fst.CRF4
import edu.umass.cs.rexo.ghuang.segmentation.CRFBibliographySegmentor

import org.jdom.input.SAXBuilder
import org.jdom.Document
import org.jdom.output.XMLOutputter
import org.jdom.output.Format

//object MetaTag extends Logger ("MetaTag"){
object MetaTag {

  val HEADER_CRF = "extractors/Hdrs.crf.dat.gz"
  val REFERENCE_CRF = "extractors/Refs.crf.dat.gz"
  val BIBLIO_SEG_CRF = "extractors/Seg.crf.dat.gz"
  val DICT_FILE = "words.txt"
  val logger = LoggerFactory.getLogger(MetaTag.getClass())

	val dataDir: File = new java.io.File("data")

	/* Construct the Metatagger pipeline from the given RxDocumentQueue and command
	 * line options */
	def buildJavaPipeline(): RxPipeline = {
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

		logger.info( "loading biblio-segmentation crf" )
		val ois = new ObjectInputStream( new GZIPInputStream(new BufferedInputStream( new FileInputStream( bibCrf ))))
		val crf = ois.readObject().asInstanceOf[CRF4]
		ois.close()
		val crfBibSegmentor = new CRFBibliographySegmentor( crf )

		pipeline.add( new edu.umass.cs.rexo.ghuang.segmentation.SegmentationFilter( crfBibSegmentor ) )

		pipeline
		.add( new GrantExtractionFilter() )
		.add( new ReferenceExtractionFilter( refCrf, hdrCrf ))
		.add( new BodyExtractionFilter())

		// .add( new CitationContextFilter() )
		// .add( new WriteAnnotatedXMLFilter() )
		// .add( new MetatagPostconditionTestFilter() )
		

		if (logp) {
			// log document errors to '.list' and '.html'
			pipeline
			.addErrorFilters()
			.add( new ErrorLogFilter() )
			.addEpilogueFilters()
			.add( new InfoLogFilter() )
			.add( new PipelineMetricsFilter() )
		}

		return pipeline
	}

  def buildScalaPipeline() : ScalaPipeline = {

    logger.info ("creating new scala component pipeline")

    return new ScalaPipeline(List(new AuthorEmailTaggingFilter))
  }


	/** Run the meta-tagger pipeline */
	def main(args: Array[String]) {
	  // val initProperties: Map = null
    // val cli: CommandLineOptions = null

	  try {
      val currentDirectory = new File(new File(".").getAbsolutePath());
      println("Current Directory Is: " + currentDirectory.getAbsolutePath())
	  	EnglishDictionary.setDefaultWordfile( new File( dataDir, DICT_FILE ) )
	  	val dictionary = EnglishDictionary.createDefault()

	  	val javaPipeline = buildJavaPipeline()
      val scalaPipeline = buildScalaPipeline()

      var reader = new BufferedReader( new InputStreamReader( System.in ) )

	  	var line: String = null
	  	logger.info( "begin" )

	  	while ({line = reader.readLine(); line != null}) {
	  		// format is input-filename -> output-filename
	  		val files = line.split( "->" )
	  		val infile = new File( files(0).trim() )
	  		val outfile = new File( files(1).trim() )

	  		logger.info( infile.getPath() + " -> " + outfile.getPath()  )
	  		if ( infile.exists() ) {
	  			val document = readInputDocument( infile )
	  			val tokenization = NewHtmlTokenization.createNewHtmlTokenization( document, dictionary )
	  			val rdoc = new RxDocument()
	  			rdoc.setTokenization( tokenization )
	  			try {
					logger.info("exectuting java pipeline")
					javaPipeline.execute( rdoc )
	/*
					SCALA pipeline turned off for now. 

					logger.info("exectuting scala pipeline")

					val tokenization = rdoc.getTokenization()
					val segmentations : Map[String, HashMap[Object, Object]] =  rdoc.getScope( "document" ).get( "segmentation" ).asInstanceOf[Map[String, HashMap[Object, Object]]]
					val doc = MetaDataXMLDocument.createFromTokenization( null, segmentations).getDocument()

					// run it!
					val newDoc = scalaPipeline(doc)
					writeOutput( outfile, newDoc )
	*/
					writeOutput( outfile, rdoc )
	  			}
	  			catch {
                    case e: Exception => {
                        logger.error(e.getClass().getName() + ": " + e.getMessage())
                    }
                }
	  		}
	  		else {
	  			logger.error( "File not found: " + infile.getPath() )
	  		}
	  	}
	  }
	  catch {
      case e:Exception => {
        logger.error( e.getClass().getName() + ": " + e.getMessage() )
      }
    }
	}
	

  @throws[java.io.IOException]("If SAXBuilder is unable to write to infile")
	private def readInputDocument(infile: File) : Document = {
	  val saxBuilder = new SAXBuilder()
	  val is = new BufferedInputStream( new FileInputStream( infile ) )
	  try {
	  	saxBuilder.build( is )
	  }
	  catch {
	  	case e:Exception => throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage())
	  }
	  finally {
	  	is.close()
	  }
  }

  private def writeOutput(outputFile: File, doc: Document) {
    var xmlOutputStream: FileOutputStream = null;

    logger.info("writing xml file now")
    try {
      xmlOutputStream = new FileOutputStream(outputFile)
      val output = new XMLOutputter(Format.getPrettyFormat()) // XMLOutputter
      output.output(doc, xmlOutputStream)
      logger.info("just wrote file!")
    } catch {
      case e: java.io.IOException => {
        logger.error( "xml writer " + e.getClass().getName() + ": " + e.getMessage())
      }
    } finally {
      if (xmlOutputStream != null)
        xmlOutputStream.close()
    }
  }
	private def writeOutput(outputFile: File, rdoc: RxDocument) {
	  val tokenization = rdoc.getTokenization()
	  val segmentations : Map[String, HashMap[Object, Object]] =  rdoc.getScope( "document" ).get( "segmentation" ).asInstanceOf[Map[String, HashMap[Object, Object]]]


	  if (tokenization == null) {
	  	logger.error( "No xml content available for document " + rdoc )
	  	return
	  }

    var xmlOutputStream : FileOutputStream = null;

	  try {
      logger.info("writing file now")
	  	val document = MetaDataXMLDocument.createFromTokenization( null, segmentations ).getDocument()
      xmlOutputStream = new FileOutputStream( outputFile )
	  	val output =  new XMLOutputter( Format.getPrettyFormat() ) // XMLOutputter
	  	output.output( document, xmlOutputStream )
      logger.info("just wrote file!")
	  }
	  catch {
      case e: java.io.IOException => {
        logger.info( "(xml writer) " + e.getClass().getName() + ": " + e.getMessage() )
      }
    }
	  finally {
		if (xmlOutputStream != null) {
	  		try {
	  			xmlOutputStream.close()
	  		}
	  		catch {
              case e: java.io.IOException =>  {
                logger.error("IO Exception: " + e.getMessage() )
              }
            }
	  	 }
	  }
	}
}


