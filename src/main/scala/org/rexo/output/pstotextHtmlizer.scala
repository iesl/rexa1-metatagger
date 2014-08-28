package org.rexo.output
//import lib.scalate._

import edu.umass.cs.iesl.scalacommons
import scalacommons.util.{StringOps => sops}

import scala.tools.nsc.{io=>sio}
import scala.{xml=>x}
//import scalaz._, scalaz.{Scalaz => Z}, Z.{node => _, _}


import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import org.jdom.output.Format
import java.io.{File =>JFile}
import scalacommons.util.EnrichedJDom._

/**
 * Formatting utility for output of pstotext
 * Sample pstotext output xml files are in the src/test/resources
 * directory
 *
 * Very small sample document -
 * <document>
 *     <page n="1">
 *         <line>
 *             <tbox llx="138" lly="405" urx="225" ury="416" f="5">
 *                 <![CDATA[1. Introduction. ]]>
 *             </tbox>
 *             <tbox llx="225" lly="405" urx="495" ury="417" f="10">
 *                 <![CDATA[In the Bayesian theory of choice under uncertainty, a ]]>
 *             </tbox>
 *         </line>
 *         <pbox llx="0.00" lly="0.00" urx="495.00" ury="667.00"/>
 *     </page>
 * </document>
 *
 * Relies on Scalate/Jade to generate html
 *
 */

import java.io.{File, FileInputStream}

object XmlToHtmlFormatter {

  def xmlLoadIs = (is:java.io.InputStream) => x.XML.load(is)
  def readDocument = (f:File) => (xmlLoadIs andThen getDocument)(new FileInputStream(f))

  case class BBox(llx:Float, lly:Float, urx:Float, ury:Float) {
    val height = ury - lly
    val width = urx - llx
  }

  sealed trait PageElement {
    def id:String
  }


  // id looks like:
  //   xpath://document/page[n="3"]/line(3)/tbox(1)


  /**
   * represents
   *   <pbox llx="0.00" lly="0.00" urx="495.00" ury="667.00"/>
   */
  // case class PageBox(box:BBox)

  case class Font(name:String, h:Float, num:Int)

  /**
   * represents
   *   <tbox llx="138" lly="405" urx="225" ury="416" f="5">
   */
  case class TextBox(id: String, text: String, font: Font, val box:BBox) extends PageElement

  case class Page(id: String, val box:BBox, val lines: Seq[Line]) extends PageElement

  case class Line(id: String, tboxes: Seq[TextBox]) extends PageElement

  case class Document(id:String, pages: Seq[Page]) extends PageElement


  //val idLens: Lens[Document, String] = Lens(_.id, (obj, v) => obj copy (id = v))
  //
  //def setId(id: String): Document => Document = idLens mod (_, _=>id)
  //
  //val pages: Lens[Document, Seq[Page]] = Lens(_.pages, (obj, v) => obj copy (pages = v))
  //
  //def reversePages: Document => Document = pages mod (_, _.reverse)
  //
  //def headPage: Document => Document = pages mod (_, (pp => Seq(pp.head)))


  //import org.fusesource.scalate._
  //import org.fusesource.scalate.util._

  //lazy val scalate = new Scalate().withClassloader(Scalate.getClass().getClassLoader())

  def getPageBBoxes(document: x.NodeSeq): Seq[BBox] =
    for (page <- document \ "page")
    yield getBBox(page)


  def parseLlurRect(elem: x.NodeSeq) = {
    "@llx @lly @urx @ury".split(" ") map (s => ((elem \ s) text))
  }


  def getBBox(page: x.NodeSeq): BBox = {
    val r = parseLlurRect((page \ "pbox")) map (_.toFloat)
    BBox(r(0),r(1),r(2),r(3))
  }

  def getFonts(document: x.NodeSeq): Seq[Font] = {
    for {
      fonts <- document \ "fonts"
      font <- fonts \ "font"
    } yield {
      Font((font \ "@name" text),
        (font \ "@h" text).toFloat,
        (font \ "@n" text).toInt)
    }
  }


  def getDocument(document: x.NodeSeq): Document = {
    val fonts = getFonts(document) map (f => (f.num, f)) toMap

    val pages = for {
      page <- document \ "page"
    } yield Page(
      "/page[n='%d']".format((page \ "@n" text).toInt),
      getBBox(page),
      for {
        (line, linei) <- (page \ "line").zipWithIndex
      } yield Line(
        "/line(%d)".format(linei+1),
        for {
          (tbox, tboxi) <- (line \ "tbox").zipWithIndex
        } yield {
          val r = parseLlurRect(tbox) map (_.toInt)
          val fontnum = (tbox \ "@f" text).toInt
          TextBox(
            "/tbox(%d)".format(tboxi+1),
            tbox.text,
            fonts(fontnum),
            BBox(r(0),r(1),r(2),r(3)))
        }
      )
    )
    Document("/", pages)
  }




  // todo these are in scalacommons..
  def getResource[A](cls:Class[A], path:String) = cls.getResource(path)
  def getResourceFile[A](cls:Class[A], path:String) = new java.io.File(getResource(cls, path).toURI())

  def render(document: Document): String = {
    val templateFile = getResourceFile(this.getClass, "/cc/rexa2/front/core/lib/pstotext-as-html.jade")
    //scalate(templateFile).render(
    //  'document -> document)
    "todo"
  }

  // def render(xmlIn:java.io.InputStream): String = {
  //   val xml = x.XML.load(xmlIn)
  //   val templateFile = getResourceFile(this.getClass, "/cc/rexa2/front/core/lib/pstotext-as-html.jade")
  //   scalate(templateFile).render(
  //     'document -> getDocument(xml))
  // }

  def renderAsDiv(document: Document): String = {
    val templateFile = getResourceFile(this.getClass, "/cc/rexa2/front/core/lib/pstotext-body-as-div.jade")
    // scalate(templateFile).render( 'document -> reversePages(document))
    "TODO"
  }

  def combineDivs(divs: Seq[String]): String = {
    val templateFile = getResourceFile(this.getClass, "/cc/rexa2/front/core/lib/pstotext-multi-html.jade")
    // scalate(templateFile).render( 'docDivs -> divs)
    "TODO"
  }

  def renderCombined(documents: Seq[Document]): String = {
    val templateFile = getResourceFile(this.getClass, "/cc/rexa2/front/core/lib/pstotext-multi-html.jade")
    // scalate(templateFile).render( 'documents -> documents)
    "TODO"
  }


  def loadXml = (filename:String) => new SAXBuilder().build(sio.File(filename).inputStream())


  // TODO: This is one ugly ugly function. please clean it up
  def pdfSelectionToMentions(filename:String,
                             pstotextXml: org.jdom.Document,
                             box: (Double, Double, Double, Double),
                             page:Int,
                             selectionType:String
                           ): String = {

    val root = pstotextXml.getRootElement()

    selectionType match {
      case "h-indent" => {

        val refstarts = {
          val xpath = "//page[@n='%d']//tbox[@llx>=%f and @llx<=%f and @ury<=%f and @lly>=%f]".format(
            page,
            box._1, box._3,
            box._2, box._4
          )
          xpathNodes(xpath, root)
        }

        val xpath = "//page[@n='%d']//tbox[@ury<=%f and @lly>=%f]".format(
          page,
          box._2, box._4
        )

        val tboxes = xpathNodes(xpath, root)
        val sbuf = new StringBuffer()


        sbuf.append("""<?xml version="1.0" encoding="UTF-8"?>""" + "\n")

        sbuf.append("<mentions>\n")

        sbuf.append("<source>\n")

        sbuf.append("<file>" + filename + "</file>\n")
        sbuf.append("<page>" + page + "</page>\n")
        sbuf.append("<bbox>" + box + "</bbox>\n")


        sbuf.append("</source>\n")

        sbuf.append("\n")

        for {
          ref <- refstarts
        } yield {
          var concatted = new StringBuffer()
          sbuf.append("<mention>\n")
          val p1 = new XMLOutputter(Format.getPrettyFormat()).outputString(ref)
          concatted.append(ref.getTextTrim).append(" ")
          sbuf.append("<line>\n")
          sbuf.append(p1)
          sbuf.append("\n")
          var curLine = ref.getParent()
          for {
            refmore <- tboxes.dropWhile(_ != ref).drop(1).takeWhile(!refstarts.contains(_))
          } {
            if (refmore.getParent != curLine) {
              concatted.append("\n")
              curLine = refmore.getParent
              sbuf.append("</line>\n")
              sbuf.append("<line>\n")
            }
            val p2 = new XMLOutputter(Format.getPrettyFormat()).outputString(refmore)
            concatted.append(refmore.getTextTrim).append(" ")
            sbuf.append(p2)
            sbuf.append("\n")
          }
          sbuf.append("</line>\n")
          sbuf.append("<tostring>\n")
          sbuf.append(concatted.toString).append("\n")
          sbuf.append("</tostring>\n")

          sbuf.append("</mention>\n")
          sbuf.append("\n")
          concatted = new StringBuffer()
        }

        sbuf.append("</mentions>\n")
        sbuf.toString()

      }

      case "block" => {
        // Select all tboxes within vertical-y range
        val xpath = "//page[@n='%d']//tbox[@llx>=%f and @urx<=%f and @ury<=%f and @lly>=%f]".format(
          page,
          box._1, box._3,
          box._2, box._4
        )

        val tboxes = xpathNodes(xpath, root)
        val sbuf = new StringBuffer()


        sbuf.append("""<?xml version="1.0" encoding="UTF-8"?>""" + "\n")

        sbuf.append("<mentions>\n")

        sbuf.append("<source>\n")

        sbuf.append("<file>" + filename + "</file>\n")
        sbuf.append("<page>" + page + "</page>\n")
        sbuf.append("<bbox>" + box + "</bbox>\n")


        sbuf.append("</source>\n")

        sbuf.append("\n")

        var curLine:org.jdom.Parent = null

        sbuf.append("<mention-block>\n")
        var concatted = new StringBuffer()
        sbuf.append("<line>\n")
        for {
          tbox <- tboxes
        } {
          if (curLine == null) {
            curLine = tbox.getParent
          }

          if (tbox.getParent != curLine) {
            curLine = tbox.getParent
            sbuf.append("</line>\n")
            sbuf.append("<line>\n")
            concatted.append("\n")
          }
          val tbs = new XMLOutputter(Format.getPrettyFormat()).outputString(tbox)
          concatted.append(tbox.getTextTrim).append(" ")
          sbuf.append(tbs)
          sbuf.append("\n")
        }
        sbuf.append("</line>\n")
        sbuf.append("<tostring>\n")
        sbuf.append(concatted.toString).append("\n")
        sbuf.append("</tostring>\n")
        concatted = new StringBuffer()
        sbuf.append("</mention-block>\n")
        sbuf.append("</mentions>\n")
        sbuf.toString()
      }
    }
  }

  def main(args: Array[String]) {
    val inputPstotextFile = args(0)

  }

}
