package org.rexo.output


object htmlGenerators {
  // pstotext body as div
  import org.rexo.output.XmlToHtmlFormatter._
  // import fmt.{Page, Font, Document}

  import scalatags.Text.{attrs => a, styles => s, _}
  import scalatags.Text.tags._

  //object cls {
  //  import scalatags.Text.short._
  //  val documentCls = "document".cls
  // 
  //}
  //def generateAsDiv(document: Document): String = {
  //  div(a.`class` := "document")(
  // 
  //  )
  // 
  //.document
  //  -var pageOffset:Float = 0
  //
  //  -for(page <- document.pages)
  //    .page
  //      -for(line <- page.lines)
  //        .line
  //          -for(tbox <- line.tboxes)
  //            -val adjHeight = math.min(tbox.font.h, tbox.box.height)
  //            span(style="font-size:#{adjHeight.toString}px;font-family:Times;")
  //              .tbox(xpath="#{document.id + page.id + line.id + tbox.id}" style="position:absolute;top:#{(page.box.height * 1.10 - tbox.box.ury + pageOffset + page.box.lly).toString}px;left:#{(tbox.box.llx).toString}px;")
  //                nobr
  //                  |#{tbox.text}
  //
  //    -pageOffset = pageOffset + (page.box.height * 1.10).toInt
  // }


  // pstotext body as html
  //!!!html
  //
  //-import cc.rexa2.front.core.lib.{Page, Font, Document}
  //-@ val document: Document
  //
  //head
  //body
  //  -var pageOffset:Float = 0
  //
  //  -for(page <- document.pages)
  //    div(style="position:absolute;top:#{pageOffset.toString}px;left:0px")
  //      |Page #{page.num}
  //
  //    font(size="3" face="Times")
  //      span(style="font-size:19px;font-family:Times")
  //        -for(line <- page.lines)
  //          -for(tbox <- line.tboxes)
  //            font(size="3" face="Times")
  //              -val adjHeight = math.min(tbox.font.h, tbox.height)
  //              span(style="font-size:#{adjHeight.toString}px;font-family:Times;")
  //                span(pbh="#{page.box.height}" tbury="#{tbox.ury}" poff="#{pageOffset}" pbury="#{page.box.lly}")
  //                div(style="position:absolute;top:#{(page.box.height * 1.10 - tbox.ury + pageOffset + page.box.lly).toString}px;left:#{(tbox.llx).toString}px;")
  //                  nobr
  //                    |#{tbox.text}
  //
  //    -pageOffset = pageOffset + (page.box.height * 1.10).toInt


  // pstotext multi html
  //!!!html
  //-import cc.rexa2.front.core.lib.{Page, Font, Document}
  //
  //-@ val documents: Seq[Document]
  //
  //head
  //body
  //  -var pageOffset:Float = 0
  //
  //  -for(dd <- documents.grouped(2))
  //    .embed(style="position:absolute;top:#{pageOffset.toString}px;left:0px")
  //      -render("pstotext-body-as-div.jade", Map("document" -> dd(0)))
  //    .embed(style="position:absolute;top:#{pageOffset.toString}px;left:600px")
  //      -render("pstotext-body-as-div.jade", Map("document" -> dd(1)))
  //    -pageOffset += math.max(dd(0).pages.head.box.height, dd(1).pages.head.box.height)



}
