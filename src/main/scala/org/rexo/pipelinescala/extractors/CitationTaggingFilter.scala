package org.rexo.pipelinescala.extractors

import java.io.PrintStream

import org.rexo.pipelinescala.extractors.CitationTypeInformation._
import org.rexo.ui.ScalaPipelineComponent
import scala.util.matching.Regex
import scala.util.matching.Regex.{Match, MatchIterator}
import scala.xml.{XML, Elem, NodeSeq, Node}
import org.slf4j.LoggerFactory


class CitationTaggingFilter extends ScalaPipelineComponent{
  val logger = LoggerFactory.getLogger(CitationTaggingFilter.this.getClass())

  override def apply(xmldata: Node, filename : String):Node= {
    logger.info("Citation Filter Running!")
    val newXML = run_filter(xmldata, filename);
    newXML;
  }

  // this is for running outside of MetaTag
  def run(infile: String) {
  }

  def run_filter(xmldata: Node, filename: String) : Node = {

    val biblioxml = xmldata \ "content" \ "biblio"
    val refList = Reference.getReferences(biblioxml)
    val referenceManager = new ReferenceManager(refList)
    var newBodyText = ""

    val infoFile = new PrintStream(filename + ".info")

    println("reference list:")
    refList.foreach(println(_))

    /* find citations in body of the xml */
    val bodyIterator : Iterator[String]= (xmldata \ "content" \ "body" text).linesWithSeparators

    var body = (xmldata \ "content" \ "body").toString()  // this keeps the xml tags in, very important!

    // get references
    val numReferences = refList.length

    ///////////////////////////////////////////////////////////////////////////////
    // Figure out which CitationType functions best for this file.
    //
    ///////////////////////////////////////////////////////////////////////////////

    val citationManager : Option[CitationManager] = CitationManager.getManager(numReferences, body)

    if (citationManager == NONE) {
      logger.info("No current citation type worked well for this file!")
      // TODO - figure out what to do here, maybe default to one of them? For now, return
      xmldata
    }

    infoFile.println("CitationType = " + citationManager.get.citationType)

    ///////////////////////////////////////////////////////////////////////////////
    // Now process the citations of the best matching Citation Type.
    // We already have the citation list, it's just a matter of walking through
    // and creating tags at it's location in the body
    ///////////////////////////////////////////////////////////////////////////////

    var index = 0;
    var tagCitList : List[Citation] = List()
    var tagRefList : List[Option[Reference]] = List()
    var offset : Int = 0 // how much text we have added this. Revamp to make cleaner
    for(citation <- citationManager.get.citationList) {
      index += 1
      logger.info(s"citation $index) " + citation.text)

      tagCitList ::= citation
      val reference = referenceManager.findReference(citation)
      tagRefList ::= reference


      if (citation.multi == -1 || citation.isLastMulti) {
        val str = body.substring(citation.startPos+offset, citation.endPos+offset)
        logger.info(s"str is $str")

        val newTag = citationManager.get.createCitationXMLTag(tagCitList, tagRefList)

        // as we add in text, we need to adjust how much the other position information needs to be adjusted by.
        val part1 = body.substring(0, citation.startPos + offset)
        val part2 = body.substring(citation.endPos+offset, body.length)

        //body = body.replace(str, newTag)
        body = part1 + newTag + part2

        tagCitList = tagCitList.drop(tagCitList.length)
        tagRefList = tagRefList.drop(tagRefList.length)
        val len = (citation.endPos - citation.startPos) /* + 1 */
        val strlen = str.length
        offset += (newTag.length - len) // for next time round
      }
    }

    val headers = xmldata \ "content" \ "headers"
    val grants = xmldata \"grants"

    import java.io._
    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
      val p = new java.io.PrintWriter(f)
      try { op(p) } finally { p.close() }
    }

    printToFile(new File("BETHexample.txt")) (p => { p.println(body) })
    var newBody = NodeSeq.Empty
    try {
      newBody = XML.loadString(body)
    } catch {
      case e: Exception => logger.info("caught exception loading new citation body text: " + e.getClass())
    }
    val newXML = <document><content>{headers +: newBody +: biblioxml}</content>{grants}</document>

    infoFile.close()

    newXML
  }
}

//////////////////////////////////////////////////////////////////////////

object Util {

  // get rid of prefix or trailing junk/white space
  def stripPrefixSuffix(str: String): String = {
    val ignorable = ",;. \t\r\n()" // maybe make this a parameter
    str.dropWhile(c => ignorable.indexOf(c) >= 0).reverse.dropWhile(c => ignorable.indexOf(c) >= 0).reverse
  }

  def cleanString(str: String) : String = {
    str.replaceAll("[,.;\\[\\]]", "")
  }
}


//
// Citation Management Section
//

object CitationManager {
  val logger = LoggerFactory.getLogger(CitationManager.this.getClass)

  def getManager(numReferences : Int, data : String)  : Option[CitationManager] = {
    // get and idea of how each regex does on the data
    val regexChoices = List(NUMERICAL, AUTHOR_LAST)
    val citationManagers: Map[CitationType, CitationManager] = (for (r <- regexChoices) yield r -> new CitationManager(r.getRegex.findAllMatchIn(data), r)).toMap

    // compare the results - for now just who finds the most citations. Add More Later!
    val evaluations: Map[CitationType, Int] = for ((m) <- citationManagers) yield {
      val diff = m._2.citationCount() - numReferences
      m._1 -> diff
    }

    val threshold = 10

    // find the one that found the most citations
    var max = -100 // hmm, make this better
    var theWinner: Option[(CitationType, Int)] = None

    evaluations.foreach((t) => {
      val typeName = t._1.getName
      logger.info(s"Type $typeName:  ${t._2} ")
      if (t._2 > max) {
        max = t._2
        theWinner = Some(t)
      }
    })

    if (theWinner != None) {
      Some(citationManagers(theWinner.get._1))
    } else {
      None
    }
  }

  def getManager(citationType : CitationType, references: List[Reference], data: String) : CitationManager = {
    new CitationManager(citationType.getRegex.findAllMatchIn(data), citationType)
  }
}

class CitationManager (citations: List[Citation], cType: CitationType) {
  val citationList : List[Citation] = citations.flatMap(cit => splitCitations(cit, cType))
  val citationType = cType

  def this(iterator: Iterator[Match], cType: CitationType) = {
    this ((for(m <- iterator) yield {
      new Citation(m.start, m.end, m.toString())
    }).toList, cType)
  }

  def citationCount() : Int = {citationList.length}

  def createCitationXMLTag(citations: List[Citation], references: List[Option[Reference]]) : String = {

    // check ordering here to see that it gets put back in in the proper order.
    val list =
      for ((cit, i) <- citations.zipWithIndex) yield {
        val ref = references(i)
        val id = if(ref.nonEmpty)ref.get.id else -1
        "<citation refID=\"%s\">%s</citation>".format(id, cit.text)
    }

    val str = citationType.getPrefix + list.mkString("") + citationType.getSuffix
    str
  }

  import scala.util.parsing.combinator.RegexParsers

  object CitationParser extends RegexParsers {
    def separator : Parser[String] = """[,;]""".r
    var namePart : Parser[String] = """[-A-Za-z, .]+ [0-9]{4}""".r
    def name : Parser[List[String]] = repsep(namePart, separator)

    def apply(input: String, regex: Regex) = {namePart = regex; parseAll(name, input)}
  }

  // take a citation and split it into multiple ones, if applicable
  def splitCitations(cit: Citation, cType: CitationType) : List[Citation] = {
    // when cleaning the string here, only trim the front and back off, don't strip the commas
    // and semi-colons (yet)

    val citStrings = CitationParser(Util.stripPrefixSuffix(cit.text), cType.getSimple.r).getOrElse(List[String]())

    if (citStrings.length == 1) {
      // there is only one; deal with it slightly differently
      List(new Citation(cit.startPos, cit.endPos, Util.stripPrefixSuffix(cit.text), -1, false))
    } else {
      for ((str, i) <- citStrings.zipWithIndex) yield {
        new Citation(cit.startPos, cit.endPos, str, i, citStrings.length - 1 == i)
      }
    }
  }
}


/*
 * start      location in the body string
 * end        end location in the body string
 * multiple   answers the questions as to whether or not this citation sits in a place with multiple ones. (grouping)
 * multiPos   position in said set
 * text       text of the citation
 *
 */
case class Citation (start: Int, end: Int, citation : String, multi: Int = -1, multiLast: Boolean = false)  {
  val startPos = start
  val endPos = end
  var refID = "-1"
  val multiple = multi  // position in citation
  def isLastMulti = multiLast
  val text = citation

  override def toString = {
    val isMultiple = multiple != -1
    s"""|
       | Location:  (start: $startPos,   end: $endPos)
       | Grouped?   $isMultiple ($multiple) Last: $isLastMulti
       | Text:      $text
       """.stripMargin
  }
}

object CitationTypeInformation {

  val Author = """[-A-Za-z, .]+ [0-9]{4}"""
  val Number = """\[[0-9]+\]"""


  sealed abstract class CitationType (regex: Regex, name: String, simple: String, prefix : String, suffix : String) extends Ordered[CitationType] {
    def compare (that: CitationType) = 1
    def getRegex : Regex = regex
    def getSimple: String = simple
    def getName : String =  name
    def getPrefix : String = prefix
    def getSuffix : String = suffix

  }

  object CitationType {
    def unapply(str : String) : Option[CitationType] = {
      str match {
        case "None"  => Some(NONE)
        case "Numerical" => Some(NUMERICAL)
        case "Author Last" => Some(AUTHOR_LAST)
        case _ => None
      }
    }
  }

  case object NONE extends CitationType       ("".r, "None", "", "", "")
  case object NUMERICAL extends CitationType  (("""(""" + Number + """)""").r, "Numerical", Number, "", "")
  case object AUTHOR_LAST extends CitationType(("""(\((""" + Author + """)([ ]*[,;]{1}""" + Author + """)*\))+""").r, "Author Last", Author, "(", ")")

  val citationTypes  : Set[CitationType] = Set(NONE, NUMERICAL, AUTHOR_LAST)
  def getCitationType(name : String) : CitationType ={
    name match {
      case "None" => NONE
      case "Numerical" => NUMERICAL
      case "Author Last" => AUTHOR_LAST
      case _ => NONE
    }
  }
}

//
// Reference Management Section
//
class ReferenceManager (references : List[Reference]) {
  val logger = LoggerFactory.getLogger(ReferenceManager.this.getClass)
  val refMap = createMap(references)

  def createMap(references : List[Reference]) : Map[String, Reference] = {
    if(references.length == 0) {
      Map.empty[String,Reference]
    }
    (for(ref <- references) yield {
      val key = createKey(ref)
      key -> ref
    }).toMap
  }

  def findReference(citation : Citation) : Option[Reference] = {
    // take the citation and parse it into something we recognize as a key
    val key = Util.cleanString(citation.text)
    refMap.get(key)
  }


  private def createKey(reference : Reference) : String = {
    if (reference.refmarker.nonEmpty) {
      Util.cleanString(reference.refmarker)
    } else {
      // key off of the first listed author's last name and year the document was published.
      val first = reference.authorList.head // this could die, but every reference has at least author.
      Util.cleanString(first.name_last + " " + reference.date.headOption.getOrElse(""))
    }
  }
}


class Reference (xmldata: Node, defaultID: Int = -1) {
  val id : String = if (xmldata \ "@refID" != NodeSeq.Empty) (xmldata \ "@refID").text
  else defaultID.toString
  val refmarker = (xmldata \ "ref-marker").text
  val authorList = Author.getAuthors(xmldata)
  val title = (xmldata \ "title").text
  val conference = (xmldata \ "conference").text
  val journal = (xmldata \ "journal").text
  val volume = (xmldata \ "volume").text
  val number = (xmldata \ "number").text
  val pages = (xmldata \ "pages").text
  val date = getDates(xmldata)  // might be more then one listed

  override def toString : String = {

    val authorInfo = for(a <- authorList) yield {a.getFullName(AuthorType.Reference) + ","}

    s"""|
       |$refmarker  $title, $conference
       |$journal, $volume, $number, $pages
       |Authors:   \t$authorInfo
       """.stripMargin

  }

  def getDates(xmldata: Node) : List[String] = {
    val datesXML= xmldata \ "date"

    (for (dateTag @ <date>{_*}</date>  <- datesXML) yield {
      dateTag.label match {
        case date =>
          dateTag.text
      }
    }).toList
  }
}

object Reference {
  var nextID = 0

  def getReferences(xml: NodeSeq) : List[Reference] = {
    val referenceXML = xml \ "reference"

    (for (refTag @ <reference>{_*}</reference> <- referenceXML) yield {
      refTag.label match {
        case reference => {
          val ref = new Reference(refTag, nextID)
          if (ref.id == nextID.toString)
            nextID += 1
          ref
        }
      }
    }).toList
  }
}