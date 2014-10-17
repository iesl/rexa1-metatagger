package org.rexo.pipelinescala.extractors

import java.io.PrintStream

import org.rexo.pipelinescala.extractors.CitationTypeInformation._
import org.rexo.ui.ScalaPipelineComponent
import scala.Null
import scala.util.matching.Regex
import scala.util.matching.Regex.{Match, MatchIterator}
import scala.xml._
import org.slf4j.LoggerFactory


class CitationTaggingFilter extends ScalaPipelineComponent {
  val logger = LoggerFactory.getLogger(CitationTaggingFilter.this.getClass())

  override def apply(xmldata: Node): Node = {
    logger.info("Citation Filter Running!")
    val newXML = run_filter(xmldata);
    newXML;
  }

  // this is for running outside of MetaTag
  def run(infile: String) {
  }

  def run_filter(xmldata: Node) : Node = {

    val biblioxml = xmldata \ "content" \ "biblio"
    val refList = ReferenceExtractor(biblioxml)
    val referenceManager = new ReferenceManager(refList)

    //val infoFile = new PrintStream(filename + ".info")
    logger.info("reference list size: " + refList.size)
    logger.trace(s"reference list: ${refList.mkString("\n")}")

    /* find citations in body of the xml */
    var body = (xmldata \ "content" \ "body").toString()  // this keeps the xml tags in, very important!
    var abstractStr = (xmldata \ "content" \ "headers" \ "abstract").toString()  // this keeps the xml tags in, very important!

    // get references
    val numReferences = refList.length

    ///////////////////////////////////////////////////////////////////////////////
    // Figure out which CitationType functions best for this file.
    //
    ///////////////////////////////////////////////////////////////////////////////

    val citationManager : Option[CitationManager] = CitationManager.getManager(refList, body)

    if (citationManager == None) {
      logger.info("No current citation type worked well for this file!")
      // TODO - figure out what to do here, maybe default to one of them? For now, return
      return xmldata
    }

    val headerCitationManager = new CitationManager(citationManager.get.citationType.getRegex.findAllMatchIn(abstractStr.replaceAll("\n", "")), citationManager.get.citationType, abstractStr)

    //infoFile.println("CitationType = " + citationManager.get.citationType)

    ///////////////////////////////////////////////////////////////////////////////
    // Now process the citations of the best matching Citation Type.
    // We already have the citation list, it's just a matter of walking through
    // and creating tags at it's location in the body
    ///////////////////////////////////////////////////////////////////////////////

    var newBodyStr = citationManager.get.processCitations(referenceManager)
    var newAbstractStr = headerCitationManager.processCitations(referenceManager)

    var headers = xmldata \ "content" \ "headers"
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

      headers = updateAbstract(newAbstract(0), headers.toSeq)
    } catch {
      case e: Exception => logger.info("caught exception loading new citation body text: " + e.getClass())
    }

    val newXML = <document><content><headers>{headers}</headers>{newBody +: biblioxml}</content>{grants}</document>

    //infoFile.close()

    newXML
  }
import scala.collection.breakOut
  // this is not the best way to do this, but will work for now.
  def updateAbstract(newAbstract : Node, header: Seq[Node]) : NodeSeq = {
    header(0).child.map(subnode =>  {
        subnode match {
        case Elem(prefix, "abstract", metadata, scope, children@_*) =>
          newAbstract

        // preserve everything else
        case other =>
          logger.trace(s"Subnode label is: ${subnode.label}");
          other
        }
    }) (breakOut)
  }
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
    str.replaceAll("[\\(\\),.;\\[\\]]", "")
  }
}


//
// Citation Management Section
//

object CitationManager {
  val logger = LoggerFactory.getLogger(CitationManager.this.getClass)

  def getByAuthorLast(references : List[Reference], data : String) : CitationManager = {
    val authorNameString = (for (ref <- references)  yield {
      if (ref.authorList.length > 0) {
        ref.authorList.head.name_last.trim() // <- ADJUST THIS ??? Really only need first name
      } else {
        ""
      }
    }).distinct.filter(s => s.nonEmpty).mkString("|")


    //val matches = ("(" + authorNameString + ")").r.findAllMatchIn(data)

    val matches = ("""([(]?(e[.]?g[.]?)?("""+authorNameString+""") (((and|&) [a-zA-Z-]+)|(et al[.]?){1})?[ ]?[(]?([0-9]{4}[a-z]?)[)]?[)]?)+""").r.findAllMatchIn(data)

    val man = new CitationManager(matches, AUTHOR_LAST, data)

    man
  }

  def getManager(refList: List[Reference], data : String) : Option[CitationManager] = {
    // get and idea of how each regex does on the data

    val cleanData = data.replaceAll("\n", " ")
    // AUTHOR_LAST is handled differently now.
    val regexChoices = List(NUMERICAL_BRACKETS, NUMERICAL_PARENS, ALPHA_NUMERIC_BRACKETS, ALPHA_NUMERIC_PARENS )
    //val regexChoices = CitationTypeInformation.citationTypes
    val cm: Map[CitationType, CitationManager] = (for (r <- regexChoices) yield r -> new CitationManager(r.getRegex.findAllMatchIn(cleanData), r, data)).toMap
    val alcm = getByAuthorLast(refList, cleanData)
    val citationManagers = cm + (AUTHOR_LAST-> alcm)

    // compare the results - for now just who finds the most citations. Add More Later!

    val evaluations: Map[CitationType, (Int, Float)] = for ((m) <- citationManagers) yield {

      // test one: who found the most citations?
      //
      val testDiff = m._2.citationCount() - refList.size

      // test two: what is the ratio of citations to references?
      val testRatio : Float = m._2.citationCount().asInstanceOf[Float] / refList.size.asInstanceOf[Float] * 100;
      m._1 -> (testDiff, testRatio)
    }

    // get rid of outliers  (less citations then references, large numbers of citations to references)
    var reduced : Map[CitationType, (Int, Float)] = (for ((citType, tests) <- evaluations) yield {
      if (tests._1 >= -10 && tests._2 < 300 /* % */) {
        Some(citType->(tests))
      } else {
        None
      }
    }).flatten.toMap


    var theWinner: Option[(CitationType, (Int, Float))] =  None
    var minDiff = 1000
    var minPer = 1000F

    reduced.foreach(test => {
     // if (Math.abs(test._2._1) < minDiff && test._2._2 < minPer) {
      if (Math.abs(test._2._1) < minDiff && (test._2._2 <= 250F && test._2._2 > 50F)) {
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

    if (cType == AUTHOR_LAST) {
      List (cit) // no op for author last...
    } else {
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

  // this will walk through all the citations it found and replace them with an XML tag.
  def processCitations(referenceManager : ReferenceManager) : String = {
    var index = 0;
    var tagCitList : List[Citation] = List()
    var tagRefList : List[Option[Reference]] = List()
    var offset : Int = 0 // how much text we have added this. Revamp to make cleaner

    var newText : String = xmlText

    for(citation <- citationList) {
      index += 1
      logger.trace(s"citation $index) ${citation.text}")

      tagCitList = tagCitList :+ citation
      val reference = referenceManager.findReference(citation, citationType, xmlText)
      tagRefList = tagRefList :+ reference


      if (citation.multi == -1 || citation.isLastMulti) {
        val str = newText.substring(citation.startPos+offset, citation.endPos+offset)
        logger.trace(s"str is $str")

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
  val Number = """[0-9]{1,3}"""  // limit to three for now, so we don't greedily grab YYYY as a citation... this could backfire.
  val AlphaNumeric = """[a-zA-Z]{2,}[0-9]+"""


  sealed abstract class CitationType (regex: Regex, name: String, simple: String, prefix : String, suffix : String) extends Ordered[CitationType] {
    def compare (that: CitationType) = 1
    def getRegex : Regex = regex
    def getSimple: String = simple
    def getName : String =  name
    def getPrefix : String = prefix
    def getSuffix : String = suffix

  }

  /*
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
  */

  case object NONE extends CitationType       ("".r, "None", "", "", "")
  case object NUMERICAL_BRACKETS extends CitationType  (("""(\[(""" + Number + """[ ,;]*)+\])""").r, "Numerical Brackets", Number, "[", "]")
  case object NUMERICAL_PARENS extends CitationType  (("""(\((""" + Number + """[ ,;]*)+\))""").r, "Numerical Parens", Number, "(", ")")
  case object ALPHA_NUMERIC_BRACKETS extends CitationType  (("""(\[(""" + AlphaNumeric + """[ ,;]*)+\])""").r, "Alpha Numeric Brackets", AlphaNumeric, "[", "]")
  case object ALPHA_NUMERIC_PARENS extends CitationType  (("""(\((""" + AlphaNumeric + """[ ,;]*)+\))""").r, "Alpha Numeric Parens", AlphaNumeric, "(", ")")
  //case object AUTHOR_LAST extends CitationType(("""([(]?(""" + Author + """)([ ](and|&) """ + Author + """)?[)]?)+""").r, "Author Last", Author, "(", ")")
  case object AUTHOR_LAST extends CitationType(("""([\[(]?[eE]?[.]?[gG]?[.]?([a-zA-Z-]+) (((and|&) [a-zA-Z-]+)|(et al[.]?){1})?[,]?[ ]?[(]?([0-9]{4}[a-z]?)[)]?[\])]?)+""").r, "Author Last", Author, "", "")

  val citationTypes  : List[CitationType] = List(NUMERICAL_BRACKETS, NUMERICAL_PARENS,
    ALPHA_NUMERIC_BRACKETS, ALPHA_NUMERIC_PARENS, AUTHOR_LAST)

  def getCitationType(name : String) : CitationType ={
    name.toLowerCase() match {
      case "none" => NONE
      case "numerical_brackets" => NUMERICAL_BRACKETS
      case "numerical_parens" => NUMERICAL_PARENS
      case "alpha_numeric_brackets" => ALPHA_NUMERIC_BRACKETS
      case "alpha_numeric_parens" => ALPHA_NUMERIC_PARENS
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
      if (key.isEmpty) None else Some(key -> ref)
    }).flatten.toMap
  }

  def findReference(citation : Citation, cType: CitationType, xmlText : String) : Option[Reference] = {
    // take the citation and parse it into something we recognize as a key

    if (cType == AUTHOR_LAST) {
      // parse the citation to extract the proper data to use as key
      val text = (xmlText.substring(citation.startPos, citation.endPos)).trim()
      val andRegex = """([a-zA-Z -]+) ([aA][nN][dD]|&) ([a-zA-Z-]+)[,]? [(]?([0-9]{4}[a-z]?)[)]?""".r.findAllIn(text)
      val etalRegex = """([a-zA-Z -]+) ([eE][tT] [aA][lL][.]?[,]?) [(]?([0-9]{4}[a-z]?)[)]?""".r.findAllIn(text)
      val noneRegex = """[(]?([a-zA-Z -]+) [(]?([0-9]{4})[)]?[)]?""".r.findAllIn(text)
      var key = ""
      var name1 = ""
      var year = ""

      if (andRegex.nonEmpty) {
        val m = andRegex.matchData.next()
        name1 = m.group(1)
        val name2 = m.group(3)
        year = m.group(4)
        key = name1 + " " + name2 + " " + year

      } else if (etalRegex.nonEmpty) {
        val m = etalRegex.matchData.next()
        name1 = m.group(1)
        year = m.group(3)
        key = name1 + " et al " + year

      } else if (noneRegex.nonEmpty) {
        val m = noneRegex.matchData.next()
        name1 = m.group(1)
        year = m.group(2)
        key = name1 + " " + year
      } else {
        // hmm... wwhat is it?
        logger.info("unable to match AUTHOR_LAST citation type to regex! '$text'")
      }

      var ref = refMap.get(key)

      // try just by author last!
      if (ref.isEmpty) {
        ref = refMap.get(name1 + " " + year)
      }

      ref

    } else {
      val key = Util.cleanString(citation.text)
      refMap.get(key)
    }

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

  }


// Keys are expected to look like this:
// Numerical: 1 2 3  (parens and brackets are removed)
// String:  AuthorLast1 YYYY                 // citation listed as Author1 YYYY - only one author listed
//          AuthorLast1 AuthorLast2 YYYY     // citation listed as Author1 and Author2 YYYY - 2 authors listed
//          AuthorLast2 et al YYYY           // citation listed as Author1 et al[.]? YYYY - more then 2 listed
//
  private def createKey(reference : Reference) : String = {
    if (reference.refmarker.nonEmpty) {
      Util.cleanString(reference.refmarker)
    } else {
      val numAuthors = reference.authorList.length

      if (numAuthors == 0) {
        ""
      } else if (numAuthors == 1) {
        Util.cleanString(reference.authorList.head.name_last + " " + reference.date.headOption.getOrElse(""))
      } else if (numAuthors == 2) {
        Util.cleanString(reference.authorList.head.name_last + " " + reference.authorList(1).name_last + " " + reference.date.headOption.getOrElse(""))
      } else {
        Util.cleanString(reference.authorList.head.name_last + " et al " + reference.date.headOption.getOrElse(""))
      }
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

object ReferenceExtractor {
  def apply(xml: NodeSeq) : List[Reference] = new ReferenceExtractor().getReferences(xml)
}

class ReferenceExtractor {
  val logger = LoggerFactory.getLogger(getClass)

  def getReferences(xml: NodeSeq) : List[Reference] = {
    var nextID = 0
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