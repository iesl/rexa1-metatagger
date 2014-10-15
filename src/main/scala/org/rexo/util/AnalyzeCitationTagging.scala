package org.rexo.util

import org.rexo.pipelinescala.extractors.CitationTypeInformation._
import org.rexo.pipelinescala.extractors._
import org.slf4j.LoggerFactory
import java.io.{FileNotFoundException, File, PrintStream, FileOutputStream}

import scala.xml.{Elem, NodeSeq, XML, Node}

class AnalyzeCitationTagging (directory : String) extends TestFilter {
  val logger = LoggerFactory.getLogger(AnalyzeCitationTagging.this.getClass)
  val sumName = "CitationSummary.txt"
  val summaryFilename = if (directory.last == '/') { directory + sumName } else { directory + "/" + sumName }

  override val getName = "CitationTaggingFilter"

  override def testFile(csvRecord: Map[String, Map[String,String]]) : Boolean = {
    // should we test this file? always run them! (for now)
    true
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

  def readInfoFile(infoFile : String) : Map[String,String] = {
    try {
      val linesList = scala.io.Source.fromFile(infoFile).getLines().toList
      (for (line <- linesList) yield {
        val spl = line.split("=")
        spl(0).trim -> spl(1).trim
      }).toMap
   }  catch {
      case e: FileNotFoundException => {
        logger.info(s"Unable to find info file: '$infoFile'")
        Map.empty[String, String]
      }
    }
  }

  def apply(XMLFile: File, directory: String, csvResults: Map[String, Map[String, String]], instDict: String): TestFilterResults = {

    val citationFilter = new CitationTaggingFilter()

    val results = new CitationTaggingFilterResults(XMLFile.getName)

    val xmlFile = XML.loadFile(XMLFile)
    val pdfName = XMLFile.getName.stripSuffix(".meta.xml")
    val pdfInfo = s"$XMLFile.info" //directory + (if(directory.last != '/') "/") +  s"$XMLFile.info"
    logger.info(s"pdfinfo found in file $pdfInfo")


    val metaInfo = readInfoFile(pdfInfo)

    if (metaInfo.isEmpty) {
      results // can't work on this file
    }

    /*
    val citationType : CitationType = metaInfo.getOrElse("CitationType", "") match {
      case c @ CitationType(t) => c
    }
    */

    // because I can't get unapply to work....
    val citationType = CitationTypeInformation.getCitationType(metaInfo.getOrElse("CitationType", ""))
    results.registerCitationType(citationType)

    val references : List[Reference] = Reference.getReferences(xmlFile \ "content" \ "biblio")
    val citations : List[Citation] = getCitationList(xmlFile \ "content" \ "body") ++ getCitationList(xmlFile \ "content" \ "headers" \"abstract")

    //val citationManager = CitationManager.getManager(citationType, references, (xmlFile \ "content" \ "body").toString)

    logger.info(s"Analyzing results for file: $pdfName")

    results.registerFoundResults(references, citations)
    results.registerSampleInfo(references, citations)

    // How many things did we match?
    citations.foreach ( c =>  {
      if (c.refID == "-1") {
        results.registerNoFoundMatch(Map("CITATION" -> c.toString()))
      } else {
        results.registerSuccess()
      }
    })

    results
  }



  def printSummary(results: List[TestFilterResults], stream: PrintStream) {
    val totalFiles = results.length

    /* revise this so one could add on information - so maybe it starts with nothing in it and
     * each filter adds on it's own stuff to it.  */

     var summaryMap = Map[String, Float](
      "totalSuccesses" -> 0,
      "totalFalseMatches" -> 0,
      "totalNumberReferencesFound" -> 0,
      //"totalNumberReferencesExpected" -> 0,
      "totalNumberCitationsFound" -> 0,
      //"totalNumberCitationsExpected" -> 0,
      "totalNumberCitationsMatched" -> 0,
      "totalNumberCitationsNotMatched" -> 0,
      "totalNumberCitationTypeNumericalBrackets" -> 0,
      "totalNumberCitationTypeNumericalParens" -> 0,
      "totalNumberCitationTypeAuthorLast" -> 0,
      "totalNumberCitationTypeNone" -> 0
    )

    summaryMap = results.foldLeft(summaryMap) { (i, filter) => filter.addToTally(i)}
    results.foreach(x => if (x.nonEmpty) {
      x.prettyPrint(stream)
      x.asInstanceOf[CitationTaggingFilterResults].updateSummaryFile(summaryFilename)
    })

/*
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

   */

   var perMatched : Float = summaryMap("totalNumberCitationsMatched") / summaryMap("totalNumberCitationsFound")  * 100
   var perNotMatched : Float = summaryMap("totalNumberCitationsNotMatched") / summaryMap("totalNumberCitationsFound") * 100

   var output =
    "\n--------------------------------------------------------\n" +
      s"\nTotal number of files analyzed:     $totalFiles\n\n\n" +
      s"References Found:                     " + summaryMap("totalNumberReferencesFound") + "\n" +
      s"Citations Found:                      " + summaryMap("totalNumberCitationsFound") + "\n" +
      //s"Citations Expected: " + summaryMap("totalNumberCitationsExpected") + "\n"
      f"Citations Matched:                    ${summaryMap("totalNumberCitationsMatched")}%.2f     $perMatched %%\n" +
      f"Citations Not Matched:                ${summaryMap("totalNumberCitationsNotMatched")}%.2f     $perNotMatched %%\n" +
      "\nCitation Type Summary: \n" +
      "\t Numerical [1] :             " + summaryMap("totalNumberCitationTypeNumericalBrackets").toInt + " file(s)\n" +
      "\t Numerical (1) :             " + summaryMap("totalNumberCitationTypeNumericalParens").toInt + " file(s)\n" +
      "\t Author Last (Seltan 1999) : " + summaryMap("totalNumberCitationTypeAuthorLast").toInt + " file(s)\n" +
    "\n\n\n" +
    "\n--------------------------------------------------------\n"

    stream.print(output)
  }

  def getCitationList(xml: NodeSeq) : List[Citation] = {
    // xml shold be html body, which has, currently, paragraph and section markers

    val citationXML = xml \\ "citation"
    (for (citTag @ <citation>{_*}</citation> <- citationXML) yield {
      citTag match {
        case c@Elem(prefix, "citation", metadata, scope, children@_*) => {
          val cit = new Citation(0, 0, citTag.text)
          val refID = metadata.get("refID")
          cit.refID = if (refID.nonEmpty) refID.get.text else "-1"
          cit
        }
      }
    }).toList

    /*
    def walkNodes (xml : NodeSeq) : List[Citation] = {
      val list = (for (subnode <- xml) yield {
        subnode match {
          case c@Elem(prefix, "citation", metadata, scope, children@_*) => {
            val cit = new Citation(0, 0, subnode.text)
            val refID = metadata.get("refID")
            cit.refID = if (refID.nonEmpty) refID.get.text else "-1"

            Some(cit) :: walkNodes(children)
          }
          case c@Elem(prefix, name, metadata, scope, children@_*) => {
            logger.info("found: " + subnode.label)
            None :: walkNodes(children)
          }
          case other => None
        }
      })

      val list = xml.flatMap(subnode => {
        subnode match {
          case c@Elem(prefix, "citation", metadata, scope, children@_*) => {
            val cit = new Citation(0, 0, subnode.text)
            val refID = metadata.get("refID")
            cit.refID = if (refID.nonEmpty) refID.get.text else "-1"

            (Some(cit) :: walkNodes(children)).flatten
          }
          case c@Elem(prefix, name, metadata, scope, children@_*) => {
            logger.info("found: " + subnode.label)
            (None :: walkNodes(children)).flatten
          }
          case other => None
        }
      })

      list.flatten.toList
    }

    val citations = walkNodes(xml)
    citations
    */
  }
}

class CitationTaggingFilterResults(pdfFilename : String) extends TestFilterResults (pdfFilename, "CitationTaggingFilter") {
  var numFoundReferences: Int = 0
  var numExpectedReferences: Int = 0
  var numCitationsLinked: Int = 0
  var numFoundCitations: Int = 0
  var numExpectedCitations: Int = 0
  var numReferencesCited: Int = 0

  var citationType: CitationType = NONE

  //var foundReferenceList : List[Map[String,String]] = List[Map[String, String]]()
  var foundReferenceList : List[Reference] = List[Reference]()
  var expectedReferenceList : List[Map[String, String]] = List[Map[String,String]]()

  //var foundCitationList : List[Map[String, String]] = List[Map[String, String]] ()
  var foundCitationList : List[Citation] = List[Citation]()
  var expectedCitationList : List[Map[String, String]] = List[Map[String, String]] ()

  var noMatchFoundList : List[Map[String, String]] = List[Map[String,String]]()
  var noMatchExpList : List[Map[String, String]] = List[Map[String,String]]()


  def nonEmpty : Boolean = {
    foundReferenceList.nonEmpty || expectedReferenceList.nonEmpty || foundCitationList.nonEmpty || expectedCitationList.nonEmpty
  }

  override def registerSuccess() {
    numCitationsLinked += 1
  }

  def registerPartialSuccess(kind: String) = {
  }

  def registerFoundResults(references: List[Reference], citations : List[Citation]) = {
    numFoundReferences += references.length
    numFoundCitations += citations.length
  }

  def registerExpectedResults(expected: Map[String, Map[String,String]]) {
  }

  def registerSampleInfo(references : List[Reference], citations: List[Citation]) {
    foundReferenceList = foundReferenceList ::: references
    foundCitationList = foundCitationList ::: citations

    foundReferenceList.foreach(ref =>
      if (foundCitationList.filter(_.refID== ref.id).nonEmpty) {
        numReferencesCited += 1;
      }
    )
  }

  def registerCitationType(citType: CitationType) {
    citationType = citType
  }

  /* there was no match for the found data */
  def registerNoFoundMatch(found : Map[String, String]) {
    noMatchFoundList ::= found
  }

  /* there was no match for the expected data */
  def registerNoExpectedMatch(exp : Map[String, String]) {
    noMatchExpList ::= exp
  }

  def machine_summary() : String = {
    s"$pdfFilename;$name;$citationType;$numFoundReferences;$numExpectedReferences;$numFoundCitations;$numExpectedCitations;$numCitationsLinked;$numReferencesCited"// TODO ADD ON HERE
  }

  override def addToTally(tally: Map[String, Float]) : Map[String, Float] = {
    val parent = super.addToTally(tally)

    val map = Map[String, Float](
      "totalNumberReferencesFound" -> (tally.getOrElse("totalNumberReferencesFound", 0F) + numFoundReferences),
      //"totalNumberReferencesExpected" -> (tally.getOrElse("totalNumberReferencesExpected", 0F) + numExpectedReferences),
      "totalNumberCitationsFound" -> (tally.getOrElse("totalNumberCitationsFound", 0F) + numFoundCitations),
      //"totalNumberCitationsExpected" -> (tally.getOrElse("totalNumberCitationsExpected", 0F) + numExpectedCitations),
      "totalNumberCitationsMatched" -> (tally.getOrElse("totalNumberCitationsMatched", 0F) + numCitationsLinked),
      "totalNumberCitationsNotMatched" -> (tally.getOrElse("totalNumberCitationsNotMatched", 0F) + noMatchFoundList.length),
      "totalNumberCitationTypeNumericalBrackets" -> (tally.getOrElse("totalNumberCitationTypeNumericalBrackets", 0F) + (if (citationType == NUMERICAL_BRACKETS) 1 else 0)),
      "totalNumberCitationTypeNumericalParens" -> (tally.getOrElse("totalNumberCitationTypeNumericalParens", 0F) + (if (citationType == NUMERICAL_PARENS) 1 else 0)),
      "totalNumberCitationTypeAuthorLast" -> (tally.getOrElse("totalNumberCitationTypeAuthorLast", 0F) + (if (citationType == AUTHOR_LAST) 1 else 0)),
      "totalNumberCitationTypeNone" -> (tally.getOrElse("totalNumberCitationTypeNone", 0F) + (if (citationType == NONE) 1 else 0)))

    parent ++ map
  }

  def updateSummaryFile(file : String) {
    // this seems a bit convoluted, opening it every time, but keeping it open throughout run time seems
    // odd too, since there's no destructor in scala and a call to a method named cleanup() seems
    // un-scala like. Leaving it for now.
    val summaryFile : PrintStream = new PrintStream(new FileOutputStream(file, true))
    summaryFile.println (machine_summary())
    summaryFile.close()
  }

  override def prettyPrint(stream: PrintStream) = {

   val perCitationsLinked : Float = numCitationsLinked.toFloat / numFoundCitations.toFloat * 100

   // now gather the summary for the file information
   var info =  "\n\n##" + machine_summary() +
      s"\nFilename: $pdfFilename\n" +
      s"\tFilter: $name\n" +
      s"\tNumber of citations found:  $numFoundCitations,    expected: $numExpectedCitations\n" +
      s"\tNumber of references found: $numFoundReferences,    expected: $numExpectedCitations\n" +
      s"\tNumer of uncited references: " + (numFoundReferences-numReferencesCited) + "\n" +
      s"\tNumber of citations matched to a reference:  $numCitationsLinked    $perCitationsLinked%\n" +
      s"\tNumber of citations _not_ matched: " +  (numFoundCitations - numCitationsLinked) + "\n" +
      s"\n"

    if (foundReferenceList.nonEmpty) {
      val refInfo = foundReferenceList.map(r => {
          val aInfo = r.authorList.map(_.getFullName(AuthorType.Reference)).mkString(", ")
          val dates = r.date.mkString(" ") // might be one or more
          s"${r.id} : ${r.refmarker} : $dates : $aInfo"
      }).mkString("\n")

      info = info.concat("\nReferences:\n" + refInfo + "\nReferences End\n")
    }


    if (foundCitationList.nonEmpty && expectedCitationList.nonEmpty) {

      var str = (for((frecord,index) <- foundCitationList.zipWithIndex) yield {
        //val erecord = expectedCitationList(index)

        "\t\t%-50s \n".format(frecord.toString)

      }).mkString
/*
      if (expectedList.length > foundRecordList.length) {
        var i = 0
        while (foundRecordList.length + i < expectedRecordList.length) {
          val erecord = expectedRecordList(foundRecordList.length)
          str = str.concat("\t\t\t\t  %-50s\n\t\t\t\t  %-50s\n\t\t\t\t  %-50s\n\n".format(erecord("AUTHOR"), erecord("EMAIL"), erecord("INST")))
          i += 1
        }
      }
*/
      //info = info.concat(s"\n\tFilter Found:\t\t\t\t\tExpected:\n\n$str")
      info = info.concat(s"\n\tFilter Found:\t\t$str")
    }

    if (noMatchFoundList.nonEmpty) {

      val str = noMatchFoundList.map(frecord =>
        s"\t\t" + frecord("CITATION") + "\n"
      ).mkString

      info = info.concat(s"\tNo reference data for these found entries:\n $str\n\n")
    }
    /*
    if (noMatchExpList.nonEmpty) {

      val str = noMatchExpList.map(frecord =>
        s"\t\t" + frecord("AUTHOR") + "\n" +
          s"\t\t" + frecord("EMAIL") + "\n" +
          s"\t\t" + frecord("INST") + "\n"
      ).mkString

      info = info.concat(s"\tNo found data for these CSV entries:\n $str\n\n")
    }
*/
    if (errorMsgs.nonEmpty) {
      val str = (errorMsgs.map(x => s"\t\t$x\n")).mkString
      info = info.concat(s"\tError Messages: \n$str")
    }

    info = info + "\n##\n"
    stream.print(info)

  }
}
