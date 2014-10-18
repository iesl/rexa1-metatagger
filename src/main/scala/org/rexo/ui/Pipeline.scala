package org.rexo.ui

import java.io.StringWriter
import java.io.StringReader
import java.io.FileOutputStream

import org.jdom.Document
import org.jdom.input.SAXBuilder
import org.jdom.output.{XMLOutputter,Format}
import scala.xml.{XML, Node}
import org.xml.sax.InputSource
import org.slf4j.{Logger,LoggerFactory}

case class PipelineNode(xml: Node, fileName: String)

trait ScalaPipelineComponent {
  def apply(node: PipelineNode) : PipelineNode
}

class ScalaPipeline (pipelineList : List[ScalaPipelineComponent]) {
  val logger = LoggerFactory.getLogger(this.getClass)

  // val pdfExtractor  // not necessary here (yet?)
  //val docTransformer =

  def apply(doc: Document, fileName: String) : Document = {

    val strbuf = new StringWriter()
    val xmlOutputter = new XMLOutputter(Format.getPrettyFormat())

    xmlOutputter.output(doc, strbuf)

    val xmldata : Node = XML.loadString(strbuf.toString)
    val xmlfinal = pipelineList.foldLeft(PipelineNode(xmldata, fileName)) { (xml, component) =>
      component(xml)
    }

    logger.info("returning new data now")
    val builder = new SAXBuilder
    val writer = new StringWriter()

    // todo - this might crash if xmldata is empty
    XML.write(writer, xmlfinal.xml, "UTF-8", true, null)

    builder.build(new InputSource(new StringReader(writer.toString)))
  }
}
