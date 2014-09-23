package org.rexo.util

import java.io.{PrintStream, File}


import org.rexo.pipelinescala.extractors.{Author, Institution, Email, AuthorEmailTaggingFilter}

import scala.collection.immutable.{List,Map}
import scala.xml.{Node, NodeSeq, XML, Elem, Attribute, Text, Null}
import org.rexo.pipelinescala.extractors.Author
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
    println("   -f    optional filename specifying where to print results to. Default is stdout")
  }

  def main(args: Array[String]) {

		val arguments: scala.collection.mutable.Map[String, String] = ParseArgs.parseArgs("MetaTaggerAnalyzer", args, "d:i:r:f:", usage)
		val dir = arguments.getOrElse("-d", "")
		val csvFilename = arguments.getOrElse("-r", "")
		val outfile = arguments.getOrElse("-f", "")
		val dictFile = arguments.getOrElse("-i", "")

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

		Analyzer.addTest(new AnalyzeAuthorEmailTagging())

		val results =
			for (file <- fileList;
					 test <- testList) yield {
				val info = resultsMap.getOrElse(file.getName.stripSuffix(".meta.xml"), Map.empty[String,Map[String, String]]);
				logger.info("Looking at: " + test.getName)

				if (info.nonEmpty) {
					test.apply(file, info, dictFile)
				} else {
					val emptyResults = new AuthorEmailFilterResults(file.getName)
					emptyResults.registerErrorMsg("No expected information found for this file. Unable to compare.  Is this a good file?")
					emptyResults
				}
			}

		if (outfile.nonEmpty) {
		  val ps = new PrintStream(outfile)
			printSummary(results, ps)
			ps.close()
		} else {
		  printSummary(results, System.out)
	  }
  }

	def printSummary(results: Array[TestFilterResults], stream : PrintStream) = {
		val totalFiles = results.length

    /* revise this so one could add on information - so maybe it starts with nothing in it and
     * each filter adds on it's own stuff to it.  */
    var summaryMap = Map[String, Float](
      "totalSuccesses" -> 0,
      "totalFalseMatches" -> 0,
      "totalPartialEmail" -> 0,
      "totalPartialInst" -> 0,
      "totalNumberAuthorsFound" -> 0,
      "totalNumberAuthorsExpected" -> 0,
      "totalNumberEmailsFound" -> 0,
      "totalNumberEmailsExpected" -> 0,
      "totalNumberInstsFound" -> 0,
      "totalNumberInstsExpected" -> 0
    )

    summaryMap = results.foldLeft(summaryMap) { (i, filter) => filter.addToTally(i)}
    results.foreach(x => if (x.nonEmpty) x.prettyPrint(stream))

    val emailPercentage: Float = if (summaryMap("totalPartialEmail") > 0) (summaryMap("totalPartialEmail") / summaryMap("totalNumberAuthorsFound") * 100) else 0
    val instPercentage: Float = if (summaryMap("totalPartialInst") > 0) (summaryMap("totalPartialInst") / summaryMap("totalNumberAuthorsFound") * 100) else 0
    val matchPercentage: Float = if (summaryMap("totalSuccesses") > 0) (summaryMap("totalSuccesses") / summaryMap("totalNumberAuthorsFound") * 100) else 0

    var output =
      "\n--------------------------------------------------------\n" +
        s"\nTotal number of files analyzed: $totalFiles\n\n\n"
    if (totalFiles != 0) {
      output +=
        s"             Found:     Expected:\n" +
          "-----------------------------------------------\n" +
          s"Authors        ${summaryMap("totalNumberAuthorsFound")}       ${summaryMap("totalNumberAuthorsExpected")}\n" +
          s"Emails         ${summaryMap("totalNumberEmailsFound")}       ${summaryMap("totalNumberEmailsExpected")}\n" +
          s"Institutions   ${summaryMap("totalNumberInstsFound")}       ${summaryMap("totalNumberInstsExpected")}\n" +
          "-----------------------------------------------\n" +
          f"Complete Author/EMail/Institute Matches:    ${summaryMap("totalSuccesses")}%.2f     $matchPercentage%.2f%%\n" +
          "Average authors found per file:             " + summaryMap("totalNumberAuthorsFound") / totalFiles + "\n" +
          f"Email Only Match:                           ${summaryMap("totalPartialEmail")}%.2f    $emailPercentage%.2f%%\n" +
          f"Institution Only Match:                     ${summaryMap("totalPartialInst")}%.2f    $instPercentage%.2f%%\n"
    }
    output += "\n--------------------------------------------------------\n"

    stream.print(output)
	}

	def parseCSVData(csvFilename: String) : Map[String, Map[String,Map[String,String]]] = {
    val csvData = scala.io.Source.fromFile(csvFilename).getLines()

    val header = csvData.next() /* header line */
    val mapAssoc = header.split(",")
    val MAX_NUM_AUTHORS = 7
    // for the rest of the entries
    (for (data <- csvData) yield {
      val splitLine = data.split(",")
      var i : Int = 2 /* ignore 'filename' and 'applicable' for now */
      var infoMap : Map[String, Map[String, String]] = Map[String, Map[String,String]]()
      while (i < splitLine.length && splitLine.isDefinedAt(i+1) && splitLine.isDefinedAt(i+2)) {
        infoMap += (splitLine(i).replaceAll(";", ",") -> Map("Email" -> splitLine(i+1).replaceAll(";", ","), "Institute" -> splitLine(i+2).replaceAll(";", ",")))
        i += 3
      }
      splitLine(0) -> infoMap
    }).toMap
  }

  def getFileList(dir: File, suffix: String): Array[File] = {
    val these = dir.listFiles

    val reg = ("""^.*""" + suffix + """$""").r
    these.filter(f => reg.findFirstIn(f.getName).isDefined)
  }
}




class AnalyzeAuthorEmailTagging() extends TestFilter {

  val logger = LoggerFactory.getLogger(AnalyzeAuthorEmailTagging.this.getClass)

  override val getName = "AuthorEmailTaggingFilter"

  def apply(XMLFile : File, csvResults : Map[String, Map[String, String]], instDict: String) : TestFilterResults = {

    val authorEmailFilter = new AuthorEmailTaggingFilter(instDict)

    // open the XML and read in the header information:
    val xmlFile = XML.loadFile(XMLFile)
    val pdfName = XMLFile.getName.stripSuffix(".meta.xml")

    var expResults = csvResults // get this into var

    logger.info(s"Analyzing results for file: $pdfName")

    val results = new AuthorEmailFilterResults(XMLFile.getName)

    val headerXML = xmlFile \ "content" \ "headers"

    val authorList = Author.getAuthors(headerXML)
    val emailList = Email.getEmails(headerXML)
    val instList = Institution.getInstitutions(headerXML)

    results.registerExpectedResults(expResults)

    if (authorList.isEmpty) {
      results.registerErrorMsg("No authors found in pdf.meta.xml file.  No author tags in file?")
    }
    if (emailList.isEmpty) {
      results.registerErrorMsg("No email addresses found in pdf.meta.xml file.  No email tags in file?")
    }
    if (instList.isEmpty) {
      results.registerErrorMsg("No institution found in pdf.meta.xml file. No institution tags in file?")
    }

    results.registerFoundResults(authorList, emailList, instList)

    for ((author, index) <- authorList.zipWithIndex) {

      //results.upSampleCount()

      // get author's name, email and inst from the xml document.
      // compare them to the expected results.

      val xmlAuthor = author.getFullName
      var xmlEmail = ""
      var xmlInst = ""

      if (author.emailMeta != None) {
        // Get the email A
        val emailInfo = author.emailMeta.get.split("-")
        val id = emailInfo(0).toInt
        val elList = emailList.filter(e => e.id == id && e.tag == emailInfo(1))

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

      var matched = false

      // remove them from the expected set as we go.
      val resultSet : Map[String, String]= expResults.getOrElse(xmlAuthor, Map.empty[String, String])

      if (resultSet.nonEmpty) {
        expResults = expResults - xmlAuthor // take it out of the result set.

        results.registerSampleInfo(Map("AUTHOR"->xmlAuthor, "EMAIL"->xmlEmail,"INST"->xmlInst),
          Map("AUTHOR"->xmlAuthor, "EMAIL"->resultSet("Email"), "INST"->resultSet("Institute")))

        matched = true  // at least to some degree
				// this should be exact
        val emailMatch = (resultSet("Email").nonEmpty && resultSet("Email") == xmlEmail)
				// this should be pretty close (as in, one is a substring of the other
				val instMatch = resultSet("Institute").nonEmpty && xmlInst.nonEmpty &&
												(resultSet("Institute").toLowerCase.r.findFirstIn(xmlInst.toLowerCase).nonEmpty ||
												xmlInst.toLowerCase.r.findFirstMatchIn(resultSet("Institute").toLowerCase).nonEmpty)

        if (emailMatch && instMatch) {
          results.registerSuccess()
        } else if (emailMatch) {
          results.registerPartialSuccess("EMAIL")
        } else if (instMatch) {
          results.registerPartialSuccess("INST")
        }

        if (xmlEmail != ""  && !emailMatch)  {
          logger.info("Have email related to author, but it doesn't match expected results:")
          logger.info(s"\tAuthor: $xmlAuthor")
          logger.info(s"\txmlEmail: $xmlEmail : expected ${resultSet("Email")}")
          logger.info(s"\txmlInst: $xmlInst : expected ${resultSet("Institute")}")
          results.registerFalseMatch()
        }
      } else {
        results.registerNoFoundMatch(Map("AUTHOR"->xmlAuthor, "EMAIL"->xmlEmail, "INST"->xmlInst))
      }

      if (!matched)
        logger.info(s"Failed to find expected results for author: $xmlAuthor")
    }

    if (expResults.size > 0) {
      expResults.foreach(x => {
        results.registerNoExpectedMatch(Map("AUTHOR" -> x._1, "EMAIL" -> x._2("Email"), "INST" -> x._2("Institute")))
      })
    }

    results
  }
}

class AuthorEmailFilterResults(filename : String) extends TestFilterResults (filename, "AuthorEmailFilter") {
  var partialEmail: Int = 0
  var partialInst: Int = 0

  var numFoundAuthors = 0
  var numFoundEmails = 0
  var numFoundInst = 0
  var numExpectedAuthors = 0
  var numExpectedEmails = 0
  var numExpectedInst = 0

  var foundRecordList : List[Map[String,String]] = List[Map[String, String]]()
  var expectedRecordList : List[Map[String, String]] = List[Map[String,String]]()
  var noMatchFoundList : List[Map[String, String]] = List[Map[String,String]]()
  var noMatchExpList : List[Map[String, String]] = List[Map[String,String]]()
  var errorMsgs : List[String] = List[String]()

  def nonEmpty : Boolean = {
    foundRecordList.nonEmpty || expectedRecordList.nonEmpty
  }


  def registerPartialSuccess(kind: String) = {
    if (kind == "EMAIL") {
      partialEmail += 1
    } else if (kind == "INST") {
      partialInst += 1
    }
  }

  def registerFoundResults(authors : List[Author], emails: List[Email], insts: List[Institution]) {
    numFoundAuthors += authors.length
    numFoundEmails += emails.length
    numFoundInst += insts.length

  }
  def registerExpectedResults(expected: Map[String, Map[String,String]]) {
    numExpectedAuthors += expected.size // mapping is Author -> email, inst

    var InstList : List[String] = List()

    expected.foreach(r => {
      if (r._2("Email").nonEmpty) {
        numExpectedEmails += 1
      }
      if (r._2("Institute").nonEmpty) {
        if (!InstList.contains(r._2("Institute"))) {
          InstList ::= r._2("Institute")
        }
      }
    })

    numExpectedInst += InstList.size
  }

  def registerSampleInfo(found : Map[String,String], expected: Map[String,String]) {
    foundRecordList ::= found
    expectedRecordList ::= expected
  }

  /* there was not match for the found data */
  def registerNoFoundMatch(found : Map[String, String]) {
    noMatchFoundList ::= found
  }

  /* there was no match for the expected data */
  def registerNoExpectedMatch(exp : Map[String, String]) {
    noMatchExpList ::= exp
  }

  def registerErrorMsg(msg : String) {
    errorMsgs ::= msg
  }

  def machine_summary() : String = {
    s"$filename;$name;$numFoundAuthors;$numExpectedAuthors;$numFoundEmails;$numExpectedEmails;$numFoundInst;$numExpectedInst;$fullSuccesses;$partialEmail;$partialInst;$falseMatches\n"
  }

  override def addToTally(tally: Map[String, Float]) : Map[String, Float] = {
    val parent = super.addToTally(tally)

    val map = Map[String, Float](
      "totalNumberAuthorsFound" -> (tally("totalNumberAuthorsFound") + numFoundAuthors),
      "totalNumberAuthorsExpected" -> (tally("totalNumberAuthorsExpected") + numExpectedAuthors),
      "totalNumberEmailsFound" -> (tally("totalNumberEmailsFound") + numFoundEmails),
      "totalNumberEmailsExpected" -> (tally("totalNumberEmailsExpected") + numExpectedEmails),
      "totalNumberInstsFound" -> (tally("totalNumberInstsFound") + numFoundInst),
      "totalNumberInstsExpected" -> (tally("totalNumberInstsExpected") + numExpectedInst),
      "totalPartialEmail" -> (tally("totalPartialEmail") + partialEmail),
      "totalPartialInst" -> (tally("totalPartialInst") + partialInst) )

    parent ++ map
  }

  override def prettyPrint(stream: PrintStream) = {
    var info =  "\n\n##" + machine_summary() +
      s"\nFilename: $filename\n" +
      s"\tFilter: $name\n" +
      s"\tNumber of authors looked at:  $numFoundAuthors,    expected: $numExpectedAuthors\n" +
      s"\tNumber of emails found:       $numFoundEmails,    expected: $numExpectedEmails\n" +
      s"\tNumber of institutions found: $numFoundInst,    expected: $numExpectedInst\n" +
      s"\tNumber A-E-I matches: $fullSuccesses    " + (if (fullSuccesses > 0) (fullSuccesses/numFoundAuthors * 100) else 0) +"%" + "\n" +
      s"\tNumber A-E matches:   $partialEmail\n" +
      s"\tNumber A-I matches:   $partialInst\n" +
      s"\tFalse Matches:        $falseMatches\n" +
      s"\n\n\tExamined Records:"

    if (foundRecordList.nonEmpty && expectedRecordList.nonEmpty) {

      var str = (for((frecord,index) <- foundRecordList.zipWithIndex) yield {
        val erecord = expectedRecordList(index)

        "\t\t%-50s  %-50s\n\t\t%-50s  %-50s\n\t\t%-50s  %-50s\n\n".format(frecord("AUTHOR"), erecord("AUTHOR"),
          frecord("EMAIL"), erecord("EMAIL"), frecord("INST"), erecord("INST"))

      }).mkString

      if (expectedRecordList.length > foundRecordList.length) {
        var i = 0
        while (foundRecordList.length + i < expectedRecordList.length) {
          val erecord = expectedRecordList(foundRecordList.length)
          str = str.concat("\t\t\t\t  %-50s\n\t\t\t\t  %-50s\n\t\t\t\t  %-50s\n\n".format(erecord("AUTHOR"), erecord("EMAIL"), erecord("INST")))
          i += 1
        }
      }

      info = info.concat(s"\n\tFilter Found:\t\t\t\t\tExpected:\n\n$str")
    }

    if (noMatchFoundList.nonEmpty) {

      val str = noMatchFoundList.map(frecord =>
        s"\t\t" + frecord("AUTHOR") + "\n" +
          s"\t\t" + frecord("EMAIL") + "\n" +
          s"\t\t" + frecord("INST") + "\n"
      ).mkString

      info = info.concat(s"\tNo CSV data for these found entries:\n $str\n\n")
    }
    if (noMatchExpList.nonEmpty) {

      val str = noMatchExpList.map(frecord =>
        s"\t\t" + frecord("AUTHOR") + "\n" +
          s"\t\t" + frecord("EMAIL") + "\n" +
          s"\t\t" + frecord("INST") + "\n"
      ).mkString

      info = info.concat(s"\tNo found data for these CSV entries:\n $str\n\n")
    }

    if (errorMsgs.nonEmpty) {
      val str = (errorMsgs.map(x => s"\t\t$x\n")).mkString
      info = info.concat(s"\tError Messages: \n$str")
    }

    info = info + "\n##\n"
    stream.print(info)

  }
}