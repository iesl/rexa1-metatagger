package org.rexo.pipelinescala.extractors

import org.rexo.ui.{ScalaPipelineComponent, StringExtras}
import org.rexo.util.{Metrics, ParseArgs}
import org.slf4j.LoggerFactory

import scala.xml.{Attribute, Elem, Node, NodeSeq, Null, Text, XML}

class Email(email: String, refid: Int, metatag: String) {
  val id = refid
  val tag = metatag

  def this(email: String, refid: Int) {
    this(email, refid, "")
  }

  override def toString: String = {
    email
  }

  def ==(emailStr: String): Boolean = {
    !Email.isValid(emailStr) && emailStr == email
  }

  def userMatches (username : String) : Boolean = {
    val parts = email split "@"
    val user = parts(0) replaceAll("[^\\x41-\\x5a | ^\\x61-\\x7a]", "")
    username.toLowerCase == user.toLowerCase
  }

  def getDomain: String = {
    (email split "@")(1)
  }

  def getUsername: String = {
		(email split "@")(0)
	}
}

object Email {
  val logger = LoggerFactory.getLogger(Email.getClass)
  private val username_regex = """^[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+(\.[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+)*"""
  private val email_regex = (username_regex + """@(([a-zA-Z0-9]([-a-zA-Z0-9]{0,61}[a-zA-Z0-9])?)\.+)+([a-zA-Z])+$""").r

  // Returns an Either object - String if Left (error), Email Object if Right
  def getEmailObj(email: String, id: Int): Either[String, Email] = {
    if (isValid(email))
      Right(new Email(email, id))
    else
      Left(s"Invalid Email address: $email")
  }

  def isValid(email: String): Boolean = email_regex.findFirstIn(email) != None

  def getEmails(xml : NodeSeq) : List[Email] = {

    val emailXML = xml \ "email"
    var emailList = List[Email]()
    for (emailTag @ <email>{_*}</email> <- emailXML) yield {
      emailTag match {
        case Elem(prefix, "email", metadata, scope, children @ _*) =>
          // parse metadata
          metadata.map(m => {
            if(m.key.contains("email")) {

            }
          })
          var index = 0;
          val id = metadata.get("id").get.text.toInt
          var emailaddr = metadata.get(s"email$index")
          while (emailaddr != None) {
            emailList ::= new Email(emailaddr.get.text, id, s"email$index")
            index += 1
            emailaddr = metadata.get(s"email$index")
          }
      }
    }
    emailList
  }
  def extractEmails(emailStr: String, refid: Int): Either[String, List[Email]] = {
    val id = refid
    val parts = emailStr split "@"
    if (parts.length >= 2) {

      // This is a first pass through the email address to separate out the
      // users, if there is more then one user in an email address.
      // Further down we will verify that the username is of the appropriate content (see isValid())
      val caseOne = """^[f{]([^,]+)(,[^,]+)+?[g}]$""".r         /* ffred,bob,jessg */
      val caseTwo = """^[^f]([^,]+)(,[^,]+)+[^g]$""".r     /* fred,bob,jess */
      val caseThree = """^([^,][^|]+)(|[^,][^|]+)+$""".r  /* fred|bob|jess */
      //val caseFour = ("""^(""" + username_regex + """)$""").r /* jess */

      val names = parts(0)  // maybe be just one name, or more then one

      val users = names match {
        case caseOne( _* ) =>
          names.drop(1).dropRight(1).split(",").toList
        case caseTwo( _* ) =>
          names.split(",").toList
        case caseThree( _* ) =>
          names.split('|').toList
        case _ => List(names)
        //case caseFour( _* ) =>
        // List(names) // do nothing
      }

      val domain = parts(1)

      def newEmail(usr: String, dom: String): Either[String, Email] = {
        val str = usr + "@" + dom
        Email.getEmailObj(str, id) match {
          case Left(s) => Left("Email creation failed:" + s)
          case Right(e) => Right(e)
        }
      }
      val results: List[Either[String, Email]] = (users.map(usr => newEmail(usr.trim, domain.trim))).toList
      // right now, I'm not doing anything with the error list -- this may or may not be a bad thing
      val error: List[String] = (results.collect({ case Left(s) => s})).toList
      if (error.length > 0) 
		  logger.info("getEmailObj errors:" + error.map(_.toString))
      val emailList: List[Email] = (results.collect({ case Right(e) => e})).toList
      if (emailList.length == 0) {
        Left("Found zero real email addresses")
      } else {
        Right(emailList)
      }
    } else Left("Bad email string. Unable to extract email address(es) from it. ")
  }

  def usernamePossibilities(author: Author) : List[String] = {
    // ie: First Middle Last ; for hyphen matching: Fi-rst Last
    // The code that does the matching will ignore any '.' in the incoming email
    // address, so no need to list those here as well.

		// TODO - some of these are dups when rendered in certain circumstances, clean it up!
    List[String](
      /* last */             author.name_last,
      /* first */            author.name_first, /* in the case of a hyphenated name, this will be unique */
      /* fmlast */           (((author.name_first split "-") map (_.head)).mkString + author.name_last), // assumes only one hyphen
      /* lastfm */           (author.name_last + ((author.name_first split "-") map (_.head)).mkString), // assumes only one hyphen
      /* first */            (author.name_first split "-").mkString, /* TODO this is a dup, if name has no hyphen */
      /* flast */            author.name_first.head + author.name_last,
      /* lastf */            author.name_last + author.name_first.head,
      /* firstlast */        author.name_first + author.name_last,
      /* lastfirst */        author.name_last + author.name_first,
		  /* firstmiddlelast */  author.name_first + author.name_middle + author.name_last,
			/* firstmlast */       author.name_first + (if (author.name_middle.nonEmpty) author.name_middle.head else "") + author.name_last
    )
  }
}

class Institution(instName: String, refid: Int) {
  val id : Int = refid
  val name : String = instName
  var address : Option[String] = None
  var note : Option[Note] = None

  def addNote(newNote : Note) {
    note = new Some(newNote)
  }

  def addAddress(newAddr: String) {
    address = new Some(newAddr)
  }

  def toXML : NodeSeq = {
    <institution-name>
      {name}
    </institution-name>
      <institution-address>
        {address.getOrElse("")}
      </institution-address>
  }
}

object Institution {
  val logger = LoggerFactory.getLogger(Institution.getClass())
  private val map = scala.collection.mutable.Map[String,(String,String)]()
  private var filename : Option[String] = None

  def toXML(instOpt : Option[Institution]) : NodeSeq = {
    instOpt.map(_.toXML).getOrElse(NodeSeq.Empty)
  }

  def getInstitutions(xml : NodeSeq) : List[Institution] = {
    val instXML = xml \ "institution"
    (for (instTag @ <institution>{_*}</institution> <- instXML) yield {
      instTag.label match {
        case institution =>
          new Institution(instTag.text, (instTag \ "@id").text.toInt)
      }
    }).toList
  }

  def readInstitutionDictionary (instFilename : String)  {
    if (instFilename == "")  return
    filename = Some(instFilename)
    val instData = scala.io.Source.fromFile(instFilename).mkString

    val lines = instData split "\n"
    for((line,index) <- lines.zipWithIndex) {
      if (!line.startsWith("#") && """^[\s]*$""".r.findFirstIn(line).isEmpty  /*&& line.nonEmpty*/) { // don't parse comment lines or blank lines
        val pair = line split ";;" // split on domain, name, address
				if (pair.length < 3)
					logger.info(s"Inst dictionary is messed up at line ($index): '${pair(0)}'")
        map += pair(0).trim.stripPrefix("www.") ->(pair(1).trim, pair(2).trim)
      }
    }

    logger.info("Institution Dictionary has " + map.size + " entries.")
  }

  def lookupInstitution(domain: String): Option[(String,String)] = {

    // start from end and walk back til it is found.  Then walk back one more to see if
    // it can be refined

    var index = domain.lastIndexOf('.', domain.lastIndexOf('.')-1)
    var key = domain.slice(index+1, domain.length)
    var foundInfo = map.get(key)
    var prevInfo : Option[(String,String)] = None

    while ((foundInfo == Some && index != -1) || (index != -1 && foundInfo == None && prevInfo == None)) {
      prevInfo = foundInfo
      index = domain.lastIndexOf('.', index - 1)
      key = domain.slice(index+1, domain.length)
      foundInfo = map.get(key)
    }

    if (foundInfo.nonEmpty) {
      foundInfo
    } else {
      prevInfo
    }
  }
}

class Note(noteXML: NodeSeq) {
  val note: String = noteXML.text
  val attributes = Map("llx" -> (noteXML \ "@llx").text,
    "lly" -> (noteXML \ "@lly").text,
    "urx" -> (noteXML \ "@urx").text,
    "ury" -> (noteXML \ "@ury").text)

  def ==(noteObj: Note): Boolean = {
    noteObj.note == note
  }

  override def toString : String = {
    note
  }
}

object cleaner
{
  // TODO - this sort of thing should probably be done earlier in this process (ie, before this filter)
  // consider foreign characters in this string (umlaut, etc)
  def cleanName(name: String) : String = {
    val nameRe = """([a-zA-Z-\. ]+)""".r
    nameRe.findFirstIn(name).getOrElse("").trim()
  }
}

class Author (xmlseq: NodeSeq, emailOption : Option[Email]) {
  val id = if (xmlseq \ "@id" != NodeSeq.Empty) (xmlseq \ "@id").text.toInt else -1
  val name_first = cleaner.cleanName((xmlseq \ "author-first").text)
  val name_middle = cleaner.cleanName((xmlseq \ "author-middle").text)
  val name_last = cleaner.cleanName((xmlseq \ "author-last").text)
  var note : Option[Note] =  Some (new Note(xmlseq \ "note"))
  val attributes = Map(
    "pagenum"->(xmlseq \ "@pageNum").text,
    "llx"->(xmlseq \ "@llx").text,
    "lly"->(xmlseq \ "@lly").text,
    "urx"->(xmlseq \ "@urx").text,
    "ury"->(xmlseq \ "@ury").text)
  val emailMeta : Option[String] = if (xmlseq \ "@email" != NodeSeq.Empty) Some((xmlseq \ "@email").text) else None
  var email = emailOption
	var emailScore : Float = 0  // how certain we are
  val instMeta : Option[String] = if (xmlseq \"@institution" != NodeSeq.Empty) Some((xmlseq \ "@institution").text) else None
  var institution: Option[Institution] = None

  def this (xmlseq: NodeSeq) { this(xmlseq, None) }

  override def toString : String =  {
    val attrs = (for ((attr, value) <- attributes ) yield { s"$attr: $value"}).mkString("\n")

    s"""|
       |Author Name: $name_first $name_last
       |Author ID: $id
       |PDF Attributes:  \n$attrs
       """.stripMargin
  }
  def getFullName : String = {
    var name = name_first + " " + name_middle
    if (name_middle != "") name += " "
    name += name_last
    name
  }

  def getNote : Option[Note] = { note }

  def addEmail(email : Email) { this.email = new Some(email)}
  def addInstitution(inst : Institution) { this.institution = new Some(inst)}

  def toXML() : Elem = {
    val instID = institution.map(f => f.id).getOrElse(-1)
    val emailID = email.map(f => f.id).getOrElse(-1)
    val instxml =
      <author-inst>
        {Institution.toXML(institution)}
      </author-inst>

    val emailxml =
      <author-email>
        {email.getOrElse(None).toString}
      </author-email>

    val node =
      <authorsummary>
        <author-first>{name_first}</author-first>
        <author-middle>{name_middle}</author-middle>
        <author-last>{name_last}</author-last>
        {if (institution != None) instxml % Attribute(None, "refid", Text(instID.toString), Null)}
        {if (email != None) emailxml % Attribute(None, "refid", Text(emailID.toString), Null)}
      </authorsummary>

    node

  }
}

object Author {

  // this getAuthors will work on the headers xml. Not used (yet) and not quite right yet
  def getAuthors(xml : NodeSeq) : List[Author] = {
    // should add something to weed out potential duplicates, but I think it's a rare file
    // that has them.
    val authorXML = xml \ "authors" \ "author"
    (for (authorTag @ <author>{_*}</author> <- authorXML) yield {
      authorTag.label match {
        case author =>
          new Author(authorTag)
      }
    }).toList

  }
}


object AuthorEmailTaggingFilter {
  val logger = LoggerFactory.getLogger(AuthorEmailTaggingFilter.getClass()) // hmm... not sure this is correct
  val metrics = new Metrics("AuthorEmailTaggingFilter", List("EmailExact", "EmailClose", "EmailAndInstitution", "EmailBetterMatchFound"))

  def main(args: Array[String]) {
		val argMap = ParseArgs.parseArgs("AuthorEmailTaggingFilter", args, "-i:-d:", AuthorEmailTaggingFilter.usage)
    // need exception handling here!!
    val infile = argMap("-i")
    var dictFile = ""

    logger.info("Current directory is: " + (new java.io.File(".")).getCanonicalPath)

    try {
      dictFile = argMap("-d") // may not be set
    } catch {
      case e: NoSuchElementException =>
    }

    new AuthorEmailTaggingFilter(dictFile).run(infile)
  }


  def usage() {
    println("Usage: authoremailtaggerfilter -d dict_filename -i filename")
  }

	def max(valList: List[Int]) : Int = {
		valList.foldLeft(0)((r,c) => r.max(c))
		//valList.foldLeft(0)((r,c) => if (r > c) r else c)
	}

  // XMLPreProcess adds an id attribute to the author, email, institution xml tags.
  // For email tag: it will also split out the email addresses and put them into an attribute on
  // the email tag
  def XMLPreProcess(node : Node) : Node = {
    var authorID = -1
    var emailID = -1
    var instID = -1

    def updateNodes(ns: Seq[Node], mayChange: Boolean): Seq[Node] = {
      for (subnode <- ns) yield {

        subnode match {

          case Elem(prefix, "author", metadata, scope, children@_*) if mayChange =>
            authorID += 1
            val meta = metadata.append(Attribute(None, "id", Text(authorID.toString), Null))
            Elem(prefix, subnode.label, meta, scope, updateNodes(children, mayChange): _*)

          case Elem(prefix, "email", metadata, scope, children@_*) if mayChange =>
            emailID += 1

            var meta = metadata

            Email.extractEmails(subnode.text, emailID) match {
              case Left(e) => logger.warn("email extraction failed for: " + subnode.text)
                List[scala.xml.MetaData]()
              case Right(e) =>
                for ((el, index) <- e.zipWithIndex) yield {
                  meta = meta.append(Attribute(None, s"email$index", Text(el.toString), Null))
                }
            }
            meta = meta.append(Attribute(None, "id", Text(emailID.toString), Null))
            Elem(prefix, subnode.label, meta, scope, updateNodes(children, mayChange): _*)

          case Elem(prefix, "institution", metadata, scope, children@_*) if mayChange =>
            instID += 1
            val meta = metadata.append(Attribute(None, "id", Text(instID.toString), Null))
            Elem(prefix, subnode.label, meta, scope, updateNodes(children, mayChange): _*)

          // I'm only interested in tagging things in the header right now, so let's not
          // touch anything else
          case Elem(prefix, "headers", metadata, scope, children@_*) =>
            Elem(prefix, "headers", metadata, scope, updateNodes(children, true): _*)

          // catch all for every other element - things outside of the header element
          // cannot change (ie, author in another section will not get an ID
          case Elem(prefix, label, metadata, scope, children@_*) =>
            Elem(prefix, label, metadata, scope, updateNodes(children, mayChange): _*)

          // preserve text
          case other => other
        }
      }
    }
    updateNodes(node.theSeq, false)(0)

  }

  def XMLPostProcess(node : Node, authorList : List[Author]) : Node = {

    def updateNodes(ns: Seq[Node], mayChange: Boolean): Seq[Node] = {

      for (subnode <- ns) yield {
        subnode match {

          case Elem(prefix, "author", metadata, scope, children@_*) if mayChange =>
            var meta = metadata;
            val author = authorList.filter(
              x => {
                x.id == metadata.get("id").get.text.toInt
              })(0)

            if (author.email != None) {
              meta = meta.append(Attribute(None, "email", Text(author.email.get.id + "-" + author.email.get.tag), Null))
            }

            if (author.institution != None) {
              meta = meta.append(Attribute(None, "institution", Text(author.institution.get.id.toString), Null))
            }

            Elem(prefix, subnode.label, meta, scope, updateNodes(children, mayChange): _*)

          // I'm only interested in adding things to the header right now, so let's not
          // touch anything else
          case Elem(prefix, "headers", metadata, scope, children@_*) =>
            Elem(prefix, "headers", metadata, scope, updateNodes(children, true): _*)

          // catch all for every other element - things outside of the header element
          // will not change
          case Elem(prefix, label, metadata, scope, children@_*) =>
            Elem(prefix, label, metadata, scope, updateNodes(children, mayChange): _*)

          // preserve text
          case other =>  other
        }
      }
    }
    updateNodes(node.theSeq, false)(0)
  }
}

class AuthorEmailTaggingFilter (instDict: String) extends ScalaPipelineComponent {
  val logger = LoggerFactory.getLogger(AuthorEmailTaggingFilter.getClass())

  override def apply(xmldata: Node): Node = {
    AuthorEmailTaggingFilter.metrics.logStart("AuthorEmailTaggingFilter")
    //val xmldata = XML.loadString(doc.toString)

    val newXML = run_filter(xmldata)

    newXML
  }

	/* TODO - move the inst dictionary reference elsewhere - so that we only load it once
	   per application, versus per file!!
	 */

  def run(infile: String) {

    AuthorEmailTaggingFilter.metrics.logStart("AuthurEmailTaggingFilter")

    val newXML = run_filter(XML.loadFile(infile))
    //XML.save((infile split ".xml")(0) + ".summary.xml", newXML, "UTF-8", true)
		XML.save(infile, newXML, "UTF-8", true)
  }

  def run_filter(xmldata : Node) : Node = {

    val refXML = AuthorEmailTaggingFilter.XMLPreProcess(xmldata)
    val headerXML = refXML \ "content" \ "headers"

    var authorList: List[Author] = List()
    var emailList: List[Email] = List()
    var instList: List[Institution] = List()
    AuthorEmailTaggingFilter.metrics.reset()
    AuthorEmailTaggingFilter.metrics.logStart("Parsing Header")

		if (instDict != "")
			Institution.readInstitutionDictionary(instDict)

    /* maybe pay attention to notes in the future. Currently they are not useful */
    authorList = Author.getAuthors(headerXML)
    emailList = Email.getEmails(headerXML)
    instList = Institution.getInstitutions(headerXML)

    AuthorEmailTaggingFilter.metrics.logStop("Parsing Header")

    if (authorList.length == 0) {
      logger.info("****** Document has no authors listed in it. Exiting ******")
    }

    val emailInstMap = mapEmailToInst(emailList, instList)
    mapAuthorToEmail(authorList, emailList, emailInstMap)

    AuthorEmailTaggingFilter.metrics.logStop("AuthorEmailTaggingFilter")

    logger.info(AuthorEmailTaggingFilter.metrics.summary())

    AuthorEmailTaggingFilter.XMLPostProcess(refXML, authorList)
  }

  def mapEmailToInst(emailList: List[Email], instList: List[Institution]) : Map[Email,Institution] = {

    val thelist : List[(Email, Option[Institution])] =
      (for (email <- emailList) yield {
				val dictInst = Institution.lookupInstitution(email.getDomain)
        if (dictInst.nonEmpty) {
					// now we see if we can match it to an institution listed in the document
          val seDictInst = new StringExtras(dictInst.get._1)
					val matchInst = instList.find(inst => {
							val substrMatch = dictInst.get._1.toLowerCase.r.findFirstIn(inst.name.toLowerCase).nonEmpty ||
							    inst.name.toLowerCase.r.findFirstMatchIn(dictInst.get._1.toLowerCase).nonEmpty

              substrMatch
              /* This needs some work.
              if (substrMatch) true  // shortcut out of here, else...

              val distance = seDictInst.editDistance(inst.name)
              // if the distance is less then 20% of the overall length....

              if (distance / seDictInst.length < .20) true else false
              */
          })

					if (matchInst.nonEmpty) {
						// we have a match (one is a substring of the other, doesn't matter which one)
						email -> matchInst // use the name from the paper, not dictionary
					} else {
            email -> None
          }
        } else {
          // no institution found in our lookup table
          email -> None
        }
      }).toList

    thelist.filter(_._2.isDefined).map(x => x._1 -> x._2.get).toMap
  }

	/*
	 Start tests for author/email matching
	 */

  // rather then just test author name (First Middle Last) this tests a variety of
  // variants on the name, as well as FirstMiddleLast.
	def emailTest_Regex(author: Author, email: Email) : Int = {

		val usernamePossibilities = Email.usernamePossibilities(author)
		val score = for(possEmail <- usernamePossibilities) yield {
			if (email.userMatches(possEmail)) {
				// have direct match!
			  possEmail.length
			} else {
				0
			}
		}

		AuthorEmailTaggingFilter.max(score)
	}

  // rather then just test author name (First Middle Last) this tests a variety of
  // variants on the name, as well as FirstMiddleLast.
	def emailTest_EditDistance(author: Author, email: Email) : Int = {
		val usernamePossibilities = Email.usernamePossibilities(author)
		val username = new StringExtras(email.getUsername.toLowerCase)
		val scores = for (possEmail <- usernamePossibilities) yield {
			// not sure if this makes sense.  Take the distance difference and subtract it from the
			// length. Then we return the number of matching characters to compare in the end
			username.length - username.editDistance(possEmail.toLowerCase)
		}

		AuthorEmailTaggingFilter.max(scores)
	}

	/*
	 End tests for author/email matching
	 */

	// TODO this is just a very rough first pass!
	// Will return a value between 0 and 1 (inclusive). 1 means it's a perfect match
	def analyzeScores(scores: List[Int],author: Author, email: Email) : Float = {
		val maxScore: Float = AuthorEmailTaggingFilter.max(scores)

		// we have the max score from the algorithm
		val userLen: Float = email.getUsername.length
		val score: Float = maxScore / userLen

		score
	}

  def mapAuthorToEmail(authorList : List[Author], emailList: List[Email], emailInstMap : Map[Email,Institution]) {

    var matchedSet = scala.collection.mutable.Set[Author]()

		for (email <- emailList;
				 author <- authorList) {

			// Step One - Run Tests
			val testScores = List(
				emailTest_Regex(author, email),
				emailTest_EditDistance(author, email))

			// Step Two - Analyze Tests
			val score = analyzeScores(testScores, author, email)

		  // Step Three - Response based on overall score
			if (score > .60) {
				// sometimes we find another close match in email. Save the one with the
				// highest score
				if (author.emailScore < score) {
					var replace = false

					// TODO - sort out how these logSuccess values should interact. If we are replacing a value, then
					// we should not increment "close" again. Email exact should only get set one time, with no one
					// replacing it, so it can just stay as is.

				  if (author.emailScore > 0) {
					  // we are replacing results, since the emailScore was already greater then the default of 0
						AuthorEmailTaggingFilter.metrics.logSuccess("EmailBetterMatchFound")
						replace = true
				  }

					if (score == 1) {
						// found a perfect match
						AuthorEmailTaggingFilter.metrics.logSuccess("EmailExact")
						if (replace)
							AuthorEmailTaggingFilter.metrics.success("EmailClose") -= 1

					} else {
						// we found a fairly close match, which is probably correct, though there is room for error
						if (!replace)
							AuthorEmailTaggingFilter.metrics.logSuccess("EmailClose")
					}

					// link them up!
					author.emailScore = score
					author.addEmail(email)

					emailInstMap.get(email) match {
						case Some(inst) =>
							AuthorEmailTaggingFilter.metrics.logSuccess("EmailAndInstitution")
							author.addInstitution(inst)
						case None => // no mapping, ignore
					}
				}
			}
		}

    /*
      // just maybe they're matched by notes.  Rare, probably
      if (author.note != None) {
        val auth_note = author.note.getOrElse("") // Should be set at this point, big time error condition if not.

        // find institution if possible
        for (inst <- instList) yield {

          val inst_note = inst.note.getOrElse("")
          if (inst_note == auth_note) {
            author.institution = new Some(inst)
          }
        }
      }
      */
  }




}



