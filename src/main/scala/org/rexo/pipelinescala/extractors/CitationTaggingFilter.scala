package org.rexo.pipelinescala.extractors

import org.rexo.ui.ScalaPipelineComponent
import scala.xml.{NodeSeq, Node}
import org.slf4j.LoggerFactory

class CitationTaggingFilter extends ScalaPipelineComponent{
  val logger = LoggerFactory.getLogger(CitationTaggingFilter.this.getClass())

  override def apply(xmldata: Node):Node= {
    val newXML = run_filter(xmldata);
    logger.info("Citation Filter Running!")
    newXML;
  }

  def run(infile: String) {

  }

  def run_filter(xmldata: Node) : Node = {

    val refList = Reference.getReferences(xmldata)
    println("reference list:")
    refList.foreach(println(_))
    xmldata;
  }
}

//object CitationTaggingFilter() {

//}

/* <ref-marker pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">[1]</ref-marker>
        <authors pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">
          <author pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">
            <author-first pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">K.</author-first>
            <author-middle pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">K.</author-middle>
            <author-last pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">Agaram,</author-last>
          </author>
          <author pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">
            <author-first pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">S.</author-first>
            <author-middle pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">W.</author-middle>
            <author-last pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">Keckler,</author-last>
          </author>
          <author pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">
            <author-first pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">C.</author-first>
            <author-last pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">Lin,</author-last>
          </author>
          and
          <author pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">
            <author-first pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">K.</author-first>
            <author-middle pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">S.</author-middle>
            <author-last pageNum="72" ury="589.0" urx="385.0" lly="574.0" llx="112.0">McKinley.</author-last>
          </author>
        </authors>
        <title pageNum="72" ury="589.0" urx="513.0" lly="564.0" llx="128.0">The memory behavior of data structures in C - SPEC CPU2000 benchmarks.</title>
        <conference pageNum="72" ury="579.0" urx="481.0" lly="564.0" llx="128.0">In 2006 SPEC Benchmark Workshop,</conference>
        <date pageNum="72" ury="579.0" urx="514.0" lly="554.0" llx="128.0">January 2006.</date>

        <reference refID="p6x45.0y334.0" pageNum="6" ury="345.0" urx="282.0" lly="307.0" llx="45.0">
        <ref-marker pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">1.</ref-marker>
        <authors pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">
          <author pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">
            <author-first pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">G.</author-first>
            <author-middle pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">S.</author-middle>
            <author-last pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">Sammelmann,</author-last>
          </author>
          <author pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">
            <author-first pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">D.</author-first>
            <author-middle pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">H.</author-middle>
            <author-last pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">Trivett,</author-last>
          </author>
          <author pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">
            <author-first pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">R.</author-first>
            <author-middle pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">H.</author-middle>
            <author-last pageNum="6" ury="345.0" urx="281.0" lly="334.0" llx="45.0">Hackmann,</author-last>
          </author>
        </authors>
        <title pageNum="6" ury="345.0" urx="282.0" lly="316.0" llx="45.0">The acoustic scattering by a submerged spherical shell. I: The bifurcation of the dispersion curve for the spherical axisymmetric Lamb wave,</title>
        <journal pageNum="6" ury="318.0" urx="130.0" lly="307.0" llx="52.0">J. Acoust. Soc. Am.,</journal>
        <volume pageNum="6" ury="318.0" urx="153.0" lly="307.0" llx="123.0">85</volume>
        <number pageNum="6" ury="318.0" urx="153.0" lly="307.0" llx="123.0">(1),</number>
        <pages pageNum="6" ury="318.0" urx="211.0" lly="307.0" llx="146.0">114--124</pages>
        <date pageNum="6" ury="318.0" urx="211.0" lly="307.0" llx="146.0">(1989)</date>
      </reference>

*/

class Reference (xmldata: Node) {
  val authorList = Author.getAuthors(xmldata)
  val title = xmldata \ "title"
  val conference = xmldata \ "conference"
  val journal = xmldata \ "journal"
  val volume = xmldata \ "volume"
  val number = xmldata \ "number"
  val pages = xmldata \ "pages"
  val date = getDates(xmldata)

  def getDates(xmldata: Node) : List[String] = {
    val datesXML= xmldata \ "date"

    (for (dateTag @ <date>(_*)</date>  <- datesXML) yield {
      dateTag.label match {
        case date =>
          dateTag.text
      }
    }).toList
  }
}

object Reference {

  def getReferences(xml: NodeSeq) : List[Reference] = {
    val referenceXML = xml \ "biblio" \"reference"

    (for (refTag @ <reference>{_*}</reference> <- referenceXML) yield {
      refTag.label match {
        case reference => new Reference(refTag)
      }
    }).toList
  }

  def createReferenceMaps(references : List[Reference]) = {
    //
    // map of
  }
}