package org.rexo.pipelinescala.extractors

import java.io.PrintStream

import org.rexo.pipelinescala.extractors.CitationTypeInformation._
import org.rexo.ui.ScalaPipelineComponent
import scala.Null
import scala.util.matching.Regex
import scala.util.matching.Regex.{Match, MatchIterator}
import scala.xml._
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

    val infoFile = new PrintStream(filename + ".info")

    println("reference list:")
    refList.foreach(println(_))

    /* find citations in body of the xml */
    var body = (xmldata \ "content" \ "body").toString()  // this keeps the xml tags in, very important!
    var abstractStr = (xmldata \ "content" \ "header" \ "abstract").toString()  // this keeps the xml tags in, very important!

    // get references
    val numReferences = refList.length

    ///////////////////////////////////////////////////////////////////////////////
    // Figure out which CitationType functions best for this file.
    //
    ///////////////////////////////////////////////////////////////////////////////

    val citationManager : Option[CitationManager] = CitationManager.getManager(numReferences, body)

    if (citationManager == None) {
      logger.info("No current citation type worked well for this file!")
      // TODO - figure out what to do here, maybe default to one of them? For now, return
      return xmldata
    }

    infoFile.println("CitationType = " + citationManager.get.citationType)

    //val absCitManager = CitationManager(citationManager.get.citationType.getRegex.findAllMatchIn(abstractStr), abstractStr)

    // Work on the abstract as well, since there might be citations in there.

    ///////////////////////////////////////////////////////////////////////////////
    // Now process the citations of the best matching Citation Type.
    // We already have the citation list, it's just a matter of walking through
    // and creating tags at it's location in the body
    ///////////////////////////////////////////////////////////////////////////////

    var newBodyStr = citationManager.get.processCitations(referenceManager)
    var newAbstractStr = citationManager.get.processCitations(referenceManager)

    val headers = xmldata \ "content" \ "headers"
    val grants = xmldata \"grants"

/*
    import java.io._
    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
      val p = new java.io.PrintWriter(f)
      try { op(p) } finally { p.close() }
    }

    printToFile(new File("ExampleCIC.txt")) (p => { p.println(body) })
    */
    var newBody = NodeSeq.Empty
    var newAbstract = NodeSeq.Empty
    try {
      newBody = XML.loadString(newBodyStr)
      newAbstract = XML.loadString(newAbstractStr)
    } catch {
      case e: Exception => logger.info("caught exception loading new citation body text: " + e.getClass())
    }

    val newXML = <document><content>{headers +: newBody +: biblioxml}</content>{grants}</document>

    infoFile.close()

    newXML
  }
/* not used yet
   def XMLPreProcess(node : Node) : Node = {

    def updateNodes(ns: Seq[Node], mayChange: Boolean): Seq[Node] = {
      for (subnode <- ns) yield {

        subnode match {

          case Elem(prefix, "abstract", metadata, scope, children@_*) if mayChange =>

            Elem(prefix, subnode.label, metadata, scope, Text() : _*)

          // preserve text
          case other => other
        }
      }
    }
    updateNodes(node.theSeq, false)(0)
  }
  */
}

//////////////////////////////////////////////////////////////////////////

object Util {

  // get rid of prefix or trailing junk/white space
  def stripPrefixSuffix(str: String): String = {
    val ignorable = ",;. \t\r\n()[]" // maybe make this a parameter
    val stepOne = str.dropWhile(c => ignorable.indexOf(c) >= 0)
    val stepTwo = stepOne.reverse.dropWhile(c => ignorable.indexOf(c) >= 0)
    stepTwo.reverse
  }

  def cleanString(str: String) : String = {
    str.replaceAll("()[,.;\\[\\]]", "")
  }
}


//
// Citation Management Section
//

object CitationManager {
  val logger = LoggerFactory.getLogger(CitationManager.this.getClass)

  def getManager(numReferences : Int, data : String)  : Option[CitationManager] = {
    // get and idea of how each regex does on the data

    val cleanData = data.replaceAll("\n", " ")
    val regexChoices = List(NUMERICAL_BRACKETS, NUMERICAL_PARENS, AUTHOR_LAST)
    val citationManagers: Map[CitationType, CitationManager] = (for (r <- regexChoices) yield r -> new CitationManager(r.getRegex.findAllMatchIn(cleanData), r, data)).toMap

    // compare the results - for now just who finds the most citations. Add More Later!

    // TODO 2nd test should be how many citations to references are there? What's an appropriate ratio?


    val evaluations: Map[CitationType, (Int, Float)] = for ((m) <- citationManagers) yield {

      // test one: who found the most citations?
      //
      val testDiff = m._2.citationCount() - numReferences

      // test two: what is the ratio of citations to references?
      val testRatio : Float = m._2.citationCount().asInstanceOf[Float] / numReferences.asInstanceOf[Float] * 100;
      m._1 -> (testDiff, testRatio)
    }

    // get rid of outliers  (less citations then references, large numbers of citations to references)
    var reduced : Map[CitationType, (Int, Float)] = (for ((citType, tests) <- evaluations) yield {
      if (tests._1 >= 0 && tests._2 < 300 /* % */) {
        Some(citType->(tests))
      } else {
        None
      }
    }).flatten.toMap


    var theWinner: Option[(CitationType, (Int, Float))] =  None;
    var minDiff = 1000;
    var minPer = 1000F;

    reduced.foreach(test => {
      if (test._2._1 < minDiff && test._2._2 < minPer) {
        minDiff = test._2._1
        minPer = test._2._2
        theWinner = Some(test)
      }
    })
    /*
    // find the one that found the most citations
    var max = -100 // hmm, make this better
    var theWinner: Option[(CitationType, Int)] = for ((citType, tests) <- evaluations) yield {

    }

    evaluations.foreach((t,v) => {
      val typeName = t._1.getName
      logger.info(s"Type $typeName:  ${t._2} ")
      if (t._2 > max) {
        max = t._2
        theWinner = Some(t)
      }
    })
 */
    if (theWinner != None) {
      Some(citationManagers(theWinner.get._1))
    } else {
      None
    }
  }
}

class CitationManager (citations: List[Citation], cType: CitationType, xmlText : String) {
  val logger = LoggerFactory.getLogger(CitationManager.this.getClass())
  val citationList : List[Citation] = citations.flatMap(cit => splitCitations(cit, cType))
  val citationType = cType
/*
  var containsAmp = false
  var containsAnd = false
  var containsEtAl = false
*/

  def this(iterator: Iterator[Match], cType: CitationType, xmlText : String) = {
    this ((for(m <- iterator) yield {
      new Citation(m.start, m.end, m.toString())
    }).toList, cType, xmlText)
  }

  def citationCount() : Int = {citationList.length}

  def createCitationXMLTag(citations: List[Citation], references: List[Option[Reference]]) : String = {

    // check ordering here to see that it gets put back in in the proper order.
    val list : List[String] =
      for ((cit, i) <- citations.zipWithIndex) yield {
        val ref = references(i)
        val id = if(ref.nonEmpty)ref.get.id else "-1"
        "<citation refID=\"%s\">%s</citation>".format(id, cit.text)
        //Elem(null, "citation", new UnprefixedAttribute("refID", id, null), TopScope, Text(cit.text))
    }

    val str = citationType.getPrefix + list.mkString(",") + citationType.getSuffix
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

  // this will walk through all the citations it found and replace them with an XML tag.
  def processCitations(referenceManager : ReferenceManager) : String = {
    var index = 0;
    var tagCitList : List[Citation] = List()
    var tagRefList : List[Option[Reference]] = List()
    var offset : Int = 0 // how much text we have added this. Revamp to make cleaner

    var newText : String = xmlText

    for(citation <- citationList) {
      index += 1
      logger.info(s"citation $index) " + citation.text)

      tagCitList = tagCitList :+ citation
      val reference = referenceManager.findReference(citation, this)
      tagRefList = tagRefList :+ reference


      if (citation.multi == -1 || citation.isLastMulti) {
        val str = newText.substring(citation.startPos+offset, citation.endPos+offset)
        logger.info(s"str is $str")

        val newTag = this.createCitationXMLTag(tagCitList, tagRefList)

        // as we add in text, we need to adjust how much the other position information needs to be adjusted by.
        val part1 = newText.substring(0, citation.startPos + offset)
        val part2 = newText.substring(citation.endPos+offset, newText.length)

        newText = part1 + newTag + part2

        tagCitList = tagCitList.drop(tagCitList.length)
        tagRefList = tagRefList.drop(tagRefList.length)
        val len = (citation.endPos - citation.startPos) /* + 1 */
        val strlen = str.length
        offset += (newTag.length - len) // for next time round
      }
    }
    newText
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
  val Number = """[0-9]+"""


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
        case "Numerical Brackets" => Some(NUMERICAL_BRACKETS)
        case "Numerical Parens" => Some(NUMERICAL_PARENS)
        case "Author Last" => Some(AUTHOR_LAST)
        case _ => None
      }
    }
  }

  case object NONE extends CitationType       ("".r, "None", "", "", "")
  case object NUMERICAL_BRACKETS extends CitationType  (("""(\[(""" + Number + """[ ,;]*)+\])""").r, "Numerical Brackets", Number, "[", "]")
  case object NUMERICAL_PARENS extends CitationType  (("""(\((""" + Number + """[ ,;]*)+\))""").r, "Numerical Parens", Number, "(", ")")
  case object AUTHOR_LAST extends CitationType(("""(\((""" + Author + """)([ ]*[,;]{1}""" + Author + """)*\))+""").r, "Author Last", Author, "(", ")")

  val citationTypes  : Set[CitationType] = Set(NONE, NUMERICAL_BRACKETS, NUMERICAL_PARENS, AUTHOR_LAST)

  def getCitationType(name : String) : CitationType ={
    name.toLowerCase() match {
      case "none" => NONE
      case "numerical_brackets" => NUMERICAL_BRACKETS
      case "numerical_parens" => NUMERICAL_PARENS
      case "author_last" => AUTHOR_LAST
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

  def findReference(citation : Citation, citManager : CitationManager) : Option[Reference] = {
    // take the citation and parse it into something we recognize as a key

/*  NEXT STEP - Make this work.
    val andMatches = """( [aA][nN][dD] |&)""".r.findAllMatchIn(citation.text)
    val etalMatch = """( [eE][tT] [aA][lL][.]? )""".r.findAllIn(citation.text)

    if( etalMatch.nonEmpty) {
      var citSplit = citation.text.toLowerCase().split("et al.")

      if (citSplit.size == 0) {  // no period
        var citSplit = citation.text.toLowerCase().split("et al")
      }
      val nameRegex = "[a-zA-Z, -]+".r
      val dateRegex = """[0-9]{4}""".r
      var nameList = List[String]()
      var date = "";

      citSplit.foreach(c => {
        val clean = c.trim()
        c match {
          case nameRegex(r) => nameList ::= r.trim()
          case dateRegex(d) => date = d.trim()
        }
      })

      if (nameList.size > 1)  {
        logger.info("In findReference.  Under et al. the name list is longer then 1 name. Odd.")
      }

      //find a reference with same name and data and authorList > 1
      val nameMap = refMap.filter( r =>
        r._2.authorList.size > 1 && r._2.date.contains(date) &&
        r._2.authorList.find(a => nameList.contains(a.name_last)).nonEmpty
      )

      if (nameMap.size == 1) {
        /* done! */
        Some(nameMap.toList(0))
      } else if (nameMap.size == 0){
        None
      } else {
        // more then 1 in list... more comparison necessary
        val newNameMap = nameMap.filter(r => r._2.authorList.size > 2)
        if (newNameMap.size == 1) {
          Some(newNameMap.toList(0))
        } else {
          // give up, we have no way of knowing what record it matches.
          None
        }
      }
    }
*/
/*
    if (andMatches.nonEmpty) {
      var citSplit = citation.text.toLowerCase().split("and")

      if (citSplit.length == 0) {
        citSplit = citation.text.toLowerCase().split("etal")
      }

      val nameRegex = "[a-zA-Z, -]+".r
      val dateRegex = """[0-9]{4}""".r
      var nameList = List[String]()
      var date = "";

      citSplit.foreach (c => {
        c match {
          case nameRegex(r) =>  nameList ::= r.trim()
          case dateRegex(d) =>  date = d.trim()
        }
      })

      val nameMap = refMap.filter( r =>
        r._2.authorList.find(a => nameList.contains(a.name_last)).nonEmpty
      )

      val dateMap = nameMap.filter(r =>  r._2.date.find(_ == date).nonEmpty)

      // only one record, we're done.
      if (dateMap.size == 1) {
        dateMap.toList(0)
      } else {
        // still not done.
        // compare all names
        val record = dateMap.find(r =>
            r._2.authorList > 0)

            HAVE TO LOOK AT EACH AUTHOR HERE. ON BACKBURNER for now.
      }

    }
    */

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

      // link to the reference a few times... ?
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