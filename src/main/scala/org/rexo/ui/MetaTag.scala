package org.rexo.ui

import java.io._
import java.net.URL
import java.util.{Map,HashMap}
import java.util.zip.GZIPInputStream

import org.apache.commons.cli.OptionBuilder
import org.rexo.ui.MetaTag._
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
	val INST_LOOKUP_FILE = "institution.dict"
  val logger = LoggerFactory.getLogger(MetaTag.getClass())

  var dataDir: File = new java.io.File("data")

  /* Construct the Metatagger pipeline from the given RxDocumentQueue and command
   * line options */
  def buildJavaPipeline(): RxPipeline = {
    val pipeline = new RxPipeline()

    // handle 'enable-log'2
    val logp = true

    // val sessionScope: Map[String, ] = pipeline.getScope("session")
    // sessionScope.put( "log.boolean", logp)
    // sessionScope.put( "log.directory", new File( "./log" ) )
    // sessionScope.put( "sessionID.integer", new Integer( -1 ) )

    pipeline.addStandardFilters()

    logger.info("loading biblio-segmentation crf")

    val ois = new ObjectInputStream( new GZIPInputStream(new BufferedInputStream(classpathInputStream("data/" + BIBLIO_SEG_CRF))))
    val crf = ois.readObject().asInstanceOf[CRF4]
    ois.close()
    val crfBibSegmentor = new CRFBibliographySegmentor( crf )

    pipeline.add( new edu.umass.cs.rexo.ghuang.segmentation.SegmentationFilter( crfBibSegmentor ) )

    pipeline
    .add(new GrantExtractionFilter())
    .add(new ReferenceExtractionFilter(classpathInputStream("data/" + REFERENCE_CRF), classpathInputStream("data/" + HEADER_CRF)))
    .add(new BodyExtractionFilter())

    // .add( new CitationContextFilter() )
    // .add( new WriteAnnotatedXMLFilter() )
    // .add( new MetatagPostconditionTestFilter() )


    if (logp) {
      // log document errors to '.list' and '.html'
      pipeline
      .addErrorFilters()
      .add(new ErrorLogFilter())
      .addEpilogueFilters()
      .add(new InfoLogFilter())
      .add(new PipelineMetricsFilter())
    }

    pipeline
  }

  def buildScalaPipeline() : ScalaPipeline = {
    logger.info ("creating new scala component pipeline. Institution Dictionary: " + dataDir.getAbsoluteFile + "/" + INST_LOOKUP_FILE)
    new ScalaPipeline(List(new AuthorEmailTaggingFilter(Some(classpathInputStream("data/"+INST_LOOKUP_FILE)))))
  }

  def commandLineOptions : CommandLineOptions = {
    new CommandLineOptions {
      def addOpt(longOpt : String, description : String, hasArgument : Boolean = false, isReq : Boolean = false) {
        // note: this looks funny, because commons-cli uses a private static instance variable which is updated
        // in the process of these static calls, and is returned and flushed with the call to create()
        OptionBuilder.withLongOpt(longOpt)
        OptionBuilder.withDescription(description)
        if (hasArgument) OptionBuilder.hasArg
        OptionBuilder.isRequired(isReq)
        getOptions.addOption(OptionBuilder.create)
      }
      override protected def createOptions() {
        addOpt("enable-log", "enable logging")
        addOpt("data-dir", "path to rexa-textmill/data/ directory", hasArgument = true, isReq = true)
        addOpt("input", "filename (instead of STDIN) for 'input_file -> output_file' pairs", hasArgument = true)
      }
    }
  }

  /** Run the meta-tagger pipeline */
  def main(args: Array[String]) {
    // val initProperties: Map = null
    val commandLine = commandLineOptions.getCommandLine(args)
    dataDir = new File(commandLine.getOptionValue("data-dir"))
    val metatag = new MetaTag()
    try {
      val currentDirectory = new File(new File(".").getAbsolutePath());
      logger.debug("Current Directory Is: " + currentDirectory.getAbsolutePath())
      val dictionary = EnglishDictionary.create(classpathInputStream("data/" + DICT_FILE))

      val javaPipeline = buildJavaPipeline()
      val scalaPipeline = buildScalaPipeline()

      val inputStream = if (commandLine.hasOption("input")) {
        new InputStreamReader(new FileInputStream(new File(commandLine.getOptionValue("input"))))
      } else {
        new InputStreamReader(System.in)
      }
      val reader = new BufferedReader(inputStream)

      var line: String = null
      logger.info( "begin" )

      while ({line = reader.readLine(); line != null}) {
        // format is input-filename -> output-filename
        val files = line.split( "->" )
        val infile = new File( files(0).trim() )
        val outfile = new File( files(1).trim() )

        logger.info( infile.getPath() + " -> " + outfile.getPath()  )
        if ( infile.exists() ) {
          try {
            val document = readInputDocument(new FileInputStream(infile))
            val newDoc = metatag.processFile(document)
            writeOutput( outfile, newDoc )
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

  def classpathInputStream(path : String) : InputStream = {
    getClass.getClassLoader.getResourceAsStream(path)
  }

  @throws[java.io.IOException]("If SAXBuilder is unable to write to infile")
  def readInputDocument(inputStream : InputStream) : Document = {
    val saxBuilder = new SAXBuilder()
    val is = new BufferedInputStream( inputStream )
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

  def writeOutput(outputFile: File, doc: Document) {
    var xmlOutputStream: FileOutputStream = null

    logger.info("writing xml file ({}) now", outputFile.toString)
    try {
      xmlOutputStream = new FileOutputStream(outputFile)
      val output = new XMLOutputter(Format.getPrettyFormat()) // XMLOutputter
      output.output(doc, xmlOutputStream)
      logger.info("just wrote file ({})!", outputFile.toString)
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
      logger.info("writing file {} now", outputFile.toString)
      val document = MetaDataXMLDocument.createFromTokenization( null, segmentations ).getDocument()
      xmlOutputStream = new FileOutputStream( outputFile )
      val output =  new XMLOutputter( Format.getPrettyFormat() ) // XMLOutputter
      output.output( document, xmlOutputStream )
      logger.info("just wrote file {}!", outputFile.toString)
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

class MetaTag {
  val dictionary : EnglishDictionary = EnglishDictionary.create(classpathInputStream("data/" + DICT_FILE))

  lazy val javaPipeline = buildJavaPipeline()
  lazy val scalaPipeline = buildScalaPipeline()

  def processFile(xmlDoc : Document) : Document = {
    val tokenization = NewHtmlTokenization.createNewHtmlTokenization(xmlDoc, dictionary)
    val rdoc = new RxDocument()
    rdoc.setTokenization( tokenization )
    logger.info("exectuting java pipeline")
    javaPipeline.execute( rdoc )

    logger.info("exectuting scala pipeline")

    //val tokenization = rdoc.getTokenization()
    val segmentations = rdoc.getScope("document").get("segmentation").asInstanceOf[Map[String, HashMap[Object, Object]]]
    try {
      val doc = MetaDataXMLDocument.createFromTokenization(null, segmentations).getDocument()

      // run it!
      scalaPipeline(doc)
    } catch {
      case npe: NullPointerException => {
        logger.info("unable to create MetaDataXMLDocument")
        null
      }
    }
  }
}

object SingleFileMetaTag {
  /**
   * @param args: inputFileName outputFileName
   */
  def main(args: Array[String]) {
    val metaTag = new MetaTag()
    val xmlDoc = readInputDocument(new FileInputStream(args(0)))
    val processedDoc = metaTag.processFile(xmlDoc)
    writeOutput(new File(args(1)), processedDoc)
  }
}