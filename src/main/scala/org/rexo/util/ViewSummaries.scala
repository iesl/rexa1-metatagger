
package org.rexo.util

import java.io.File

import scala.util.matching.Regex
import scala.xml.{Elem, NodeSeq, Null, XML}


// NOTE - this is very ugly code right now.  More of a proof of concept and will be cleaned up
// soon.

object ViewSummary {
  def main(args: Array[String]) {
    new ViewSummary().run(args)
  }
  def usage() {
    println("Usage: viewsummary -d directory -o output_html_file")
    println("This script will output a html file containing a summary of " +
      "the relationships between author(s) and email/institution for each" +
      "'*.summary.xml' file located in the specified directory")
  }

  def printTag(tag: NodeSeq, p : java.io.PrintWriter, indent : Int)  {
    tag match {
      case Elem(prefix, name, attribs, scope, children@_*) =>

        //p.write(s"${name.toUpperCase} </br>&nbsp&nbsp${tag.text} </br>")
        val ind = (for (a <- 1 to indent) yield "&nbsp").mkString("")

        p.write(s"</br>$ind${name.toUpperCase}:&nbsp&nbsp&nbsp&nbsp")

        if (tag.text.length > 0 && children.length == 1)
          p.write("<span style=\"color: red\">" + tag.text + "</span>")

        if (attribs.length > 0) {
          p.write("</br>\n<span style=\"width: 50%; font-style: italic\">" + ind + " attributes: ")
          attribs.foreach(c => p.write(c + " ; "))
          p.write("</span>")
        }

        children.foreach(c => {printTag(c, p, indent + 4)})
      case _=> // hmm... what here?
    }
    p.write("\n")
  }
 }

class ViewSummary {

  def listDirFiles(f: File, r: Regex): Array[File] = {
    val these = f.listFiles
    val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
    good
    // below would go down the tree - I only want
    //good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_,r))
  }

  /*def findNode(header: NodeSeq, refID: Int): Node = {
    for(subnode <- header) yield {
      subnode match {
        //case Elem(prefix, "author", attribs, scope, children : @ _*) =>

      }
    }
  } */

  def run(args : Array[String]) {
    val argMap = ParseArgs.parseArgs("ViewSummary", args, "d:o:", ViewSummary.usage)

    val dir = new File(argMap("-d")) // try catch eventually!
    val outfilename = new File(argMap("-o"))

    val fileList = listDirFiles(dir, """^*.pdf.meta.summary.xml$""".r).toList

    val pp = new scala.xml.PrettyPrinter(80, 4)

    // I realize this could be done via the XML libraries. Right now, just doing it this way.
    println (s"opening file '$outfilename'")
    val htmlFile = new java.io.PrintWriter(outfilename)

    htmlFile.write("<!DOCTYPE html>\n<html>\n<body>")

    for ((fi, index) <- fileList.zipWithIndex) {
      println(s"$index) *** starting *** file: " + fi.getName)

      htmlFile.write("<table style=\"width: 90%;border: 2px solid green;\">\n")
      htmlFile.write("<tr>\n<td style=\"background-color: #ffff7f;font-size: 20px\" colspan=\"2\">Filename: " +
        "<a target=\"popup\" href=\"" + fi.getAbsolutePath() + "\" onclick=\"return window.open(href, \"pdfview\", 'width=600, height=400')>" + fi.getName + "</a></td>\n</tr>")
      htmlFile.write("<tr>\n<td style=\"text-align: center\">Summary</td>\n<td style=\"text-align: center\">Original Info</td>\n</tr>")

      val xmldata = XML.loadFile(fi)

      val headers = xmldata \ "content" \ "headers"
      val summaries = xmldata \ "authorsummaries" \"authorsummary"

      //println("headers: " + headers)

      for (summary <- summaries) {
        htmlFile.write("<tr>\n<td style=\"width: 50%;border: 1px solid blue\">\n")

        println("Author Summary is: \n" + pp.format(summary))
        ViewSummary.printTag(summary, htmlFile, 2)
        htmlFile.write("</td>\n<td style=\"width: 50%;border: 1px solid blue\">\n")

        val authRefID = summary.attribute("refid").getOrElse(-1)
        // this is officially ugly...
        val emailNode = (summary \ "author-email").headOption.getOrElse(Null)
        val instNode =  (summary \ "author-inst").headOption.getOrElse(Null)

        val emailRefID = if (emailNode != Null) emailNode.asInstanceOf[Elem].attribute("refid").getOrElse(-1) else -1
        val instRefID = if (instNode != Null) instNode.asInstanceOf[Elem].attribute("refid").getOrElse(-1) else -1

        //println("email: " + (summary \ "author-email" ))

        if (authRefID != -1) {
          val authorRef = (headers \ "authors" \ "author").filter( f => {
            f.attribute("id").getOrElse(-1) == authRefID
          })
          authorRef.map(n => ViewSummary.printTag(n, htmlFile, 2))
        }

        if (emailRefID != -1) {
          val emailRef = (headers \ "email").filter(f => {
            f.attribute("id").getOrElse(-1) == emailRefID
          })

          emailRef.map(n => ViewSummary.printTag(n, htmlFile, 2))
        }

        if(instRefID != 1) {
          val instRef = (headers \ "institution").filter(f => {
            f.attribute("id").getOrElse(-1) == instRefID
          })
          instRef.map(n => ViewSummary.printTag(n, htmlFile, 2))
        }

        htmlFile.write("</td>\n</tr>\n")
      }
      htmlFile.write("</table>\n</br></br>")
      println(s"$index) *** finished *** file: " + fi.getName)
    }
    htmlFile.write("</body>\n</html>")
    htmlFile.close()
  }
}
