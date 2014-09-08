package org.rexo.util

import java.io.{PrintStream, File}

import scala.collection.immutable.List
import scala.xml.{Node, NodeSeq, XML, Elem, Attribute, Text, Null}
import org.rexo.ui.AuthorEmailTaggingFilter
import org.slf4j.{Logger, LoggerFactory}

object Analyzer {
  var testList : List[TestFilter] = List[TestFilter]()
  val logger = LoggerFactory.getLogger(Analyzer.getClass)

  def addTest(test : TestFilter) = {
    testList ::= test
  }

  def usage() {
    println("Program Usage:")
    println("\tFilterAnalyzer -d <directory> -r <csv results file> [-f <outfilename>]")
    println("   -d    directory where processed files are. Will only operate on *.meta.xml files")
    println("   -r    CSV results file. One row per file")
    println("   -f    optional filename specifing where to print results to. Default is stdout")
  }

  def main(args: Array[String]) {

    val arguments: scala.collection.mutable.Map[String,String] = ParseArgs.parseArgs("MetaTaggerAnalyzer", args, "d:r:f:", usage)
    val dir = arguments.getOrElse("-d", "")
    val csvFilename = arguments.getOrElse("-r", "")
    val outfile = arguments.getOrElse("-f", "")

    if (dir == "" || csvFilename == "") {
      println("Missing arguments, unable to proceed.")
      usage()
      sys.exit()
    }

    // get pdf.meta.xml list
    val directory = new File(dir)
    val fileList = getFileList(directory, "pdf.meta.xml")

    // open csv file
    val resultsMap = parseCSVData(csvFilename)
    logger.info("results map: " + resultsMap)

    Analyzer.addTest(new AnalyzeAuthorEmailTagging())

    // 23 elements in header
    val results =
      //for ((filename, info) <- resultsMap;
       //   test <- testList) yield {
      for (file <- fileList;
           test <- testList)  yield {
        val info = resultsMap.getOrElse(file.getName().stripSuffix(".meta.xml"), Map.empty[String,String]);
        logger.info("Looking at: " + test.getName)

        if (info.nonEmpty)
          test.apply(file, info)
        else
          new TestFilterResults("AuthorEmailTaggingFilter: " + file.getName() + ". No expected results for this file.")
      }

    results.foreach (_.prettyPrint(System.out))
  }

  def parseCSVData(csvFilename: String) : Map [String, Map[String,String]] = {
    val csvData = scala.io.Source.fromFile(csvFilename).getLines()

    val header = csvData.next() /* header line */
    val mapAssoc = header.split(",")

    // for the rest of the entries
    (for (data <- csvData) yield {
      val splitLine = data.split(",")
      val infoMap : Map[String, String]= (for ((part, index) <- splitLine.zipWithIndex) yield {
        mapAssoc(index) -> part
      }).toMap

      splitLine(0) -> infoMap
    }).toMap
  }

  def getFileList(dir: File, suffix: String): Array[File] = {
    val these = dir.listFiles
    val reg = ("""^.*""" + suffix + """$""").r
    these.filter(f => reg.findFirstIn(f.getName).isDefined)
  }
}

class TestFilterResults(filterName: String) {
  var total_samples = 0
  var full_successes: Int = 0
  var partial_email: Int = 0
  var partial_inst: Int = 0
  // filter found a match, but it wasn't the expected results
  var false_matches: Int = 0
  //val time_ms: Double = 0 // unused

  def upFileCount() = {total_samples += 1}

  def registerSuccess() = {full_successes += 1}

  def registerPartialSuccess (kind: String) =  {
    if (kind == "EMAIL") {
      partial_email +=1
    } else if (kind == "INST") {
      partial_inst += 1
    }
  }

  def registerFalseMatch() = {false_matches += 1}

  def prettyPrint(stream: PrintStream) = {

    val info = s"Filter: $filterName\n" +
      s"\tNumber of files compared: $total_samples\n" +
      s"\tNumber fully matched: $full_successes\n " +
      s"\tPartial matches:\n" +
      s"\t\t(email only): $partial_email\n" +
      s"\t\t(inst only): $partial_inst\n" +
      s"\tFalse Matches: $false_matches"

    stream.print(info)
  }
}

abstract class TestFilter() {

  var results: TestFilterResults
  val filterName : String

  def apply(XMLfile : File, expectedResults : Map[String,String]) : TestFilterResults

  def getName : String = filterName

}

class AnalyzeAuthorEmailTagging() extends TestFilter {
  val logger = LoggerFactory.getLogger(AnalyzeAuthorEmailTagging.this.getClass)
  val filterName = "AuthorEmailTagginFilter"
  var results: TestFilterResults = new TestFilterResults("AuthorEmailTagging")

  def apply(XMLFile : File, expResults : Map[String,String]) : TestFilterResults = {

    val authorEmailFilter = new AuthorEmailTaggingFilter()

    // open the XML and read in the header information:
    val xmlFile = XML.loadFile(XMLFile)
    val pdfName = XMLFile.getName.stripSuffix(".meta.xml")

    logger.info(s"Analyzing results for file: $pdfName")

    val results = new TestFilterResults("AuthorEmailTaggingFilter: " + XMLFile.getName())

    val headerXML = xmlFile \ "content" \ "headers"

    val authorList = authorEmailFilter.getAuthors(headerXML)
    val emailList = authorEmailFilter.getEmails(headerXML)
    val instList = authorEmailFilter.getInstitutions(headerXML)

    for ((author, index) <- authorList.zipWithIndex) {

      results.upFileCount()

      // get author's name, email and inst from the xml document.
      // compare them to the expected results.

      val xmlAuthor = author.getFullName
      var xmlEmail = ""
      var xmlInst = ""

      if (author.emailMeta != None) {
        // Get the email A
        val emailInfo = author.emailMeta.get.split("-")
        val id = emailInfo(0).toInt
        val elList = emailList.filter(_.id == id)

        if (elList.length == 1) {
          logger.info("found matching email for " + xmlAuthor)
          xmlEmail = elList(0).toString()
        } else {
          if (elList.length == 0) {
            logger.info("no email for author " + xmlAuthor)
          } else {
            logger.info("TROUBLE: filtered email list has more then one matching email!")
          }
        }
      }

      if (author.instMeta != None) {
        val instID = author.instMeta.get.toInt
        val filteredInstList = instList.filter(_.id == instID)

        if (filteredInstList.length == 1) {
          logger.info("Found matching institution for " + xmlAuthor)
          xmlInst = filteredInstList(0).name
        } else {
          if (filteredInstList.length == 0) {
            logger.info("no institution found for author " + xmlAuthor)
          }
          logger.info("")
        }
      }

      // now compare!
      val csvIterator = expResults.iterator
      var matched = false
      while (csvIterator.hasNext && !matched) {
        val set = csvIterator.next()
        if (set._2 == xmlAuthor) {
          // found author, now compare email, etc
          matched = true

          val csvEmail = csvIterator.next()._2
          val csvInst = csvIterator.next()._2

          val emailMatch = csvEmail == xmlEmail
          val instMatch = csvInst == xmlInst

          if (emailMatch && instMatch) {
            results.registerSuccess()
          } else if (emailMatch) {
            results.registerPartialSuccess("EMAIL")
          } else if (instMatch) {
            results.registerPartialSuccess("INST")
          }

          if (xmlEmail != ""  && !emailMatch)  {
            logger.info("Have email related to author, but it doesn't match expected results:")
            logger.info(s"\txmlAuthor: $xmlAuthor  : expected ${set._2}")
            logger.info(s"\txmlEmail: $xmlEmail : expected $csvEmail")
            logger.info(s"\txmlInst: $xmlInst : expected $csvInst")
            results.registerFalseMatch()
          }
        }
      }

      if (!matched)
        logger.info(s"Failed to find expected results for author: $xmlAuthor")
    }
    results
  }
}
