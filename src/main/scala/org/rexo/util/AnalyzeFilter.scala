package org.rexo.util

import org.slf4j.{Logger, LoggerFactory}
import java.io.{PrintStream, File}

object AnalyzeFilter {
  var testList : List[TestFilter] = List[TestFilter]()
  val logger = LoggerFactory.getLogger(AnalyzeFilter.getClass)

  def addTest(test : TestFilter) = {
    testList ::= test
  }

  def usage() {
    println("Program Usage:")
    println("\tFilterAnalyzer -d <directory> -r <csv results file> [-f <outfilename>]")
    println("   -d    directory where processed files are. Will only operate on *.meta.xml files")
    println("   -r    CSV results file. One row per file (some filters may not use this)")
    println("   -f    optional filename specifying where to print results to. Default is stdout")
  }

  def main(args: Array[String]) {

		val arguments: scala.collection.mutable.Map[String, String] = ParseArgs.parseArgs("MetaTaggerAnalyzer", args, "d:i:r:f:", usage)
		val dir = arguments.getOrElse("-d", "")
		val csvFilename = arguments.getOrElse("-r", "")
		val outfile = arguments.getOrElse("-f", "")
		val dictFile = arguments.getOrElse("-i", "")

		if (dir == "" || csvFilename == "") {
			println("Missing arguments, unable to proceed.")
			usage()
			sys.exit()
		}

		// get pdf.meta.xml list
		val directory = new File(dir)
		val fileList = getFileList(directory, "pdf.meta.xml")

		AnalyzeFilter.addTest(new AnalyzeAuthorEmailTagging())
    AnalyzeFilter.addTest(new AnalyzeCitationTagging(dir))

    val results : Map[String,List[TestFilterResults]] = (for (test <- testList)  yield {
      logger.info("Currently testing filter: " + test.getName)

      // open csv file
      val resultsMap = if (csvFilename.nonEmpty) test.parseCSVData(csvFilename) else Map[String, Map[String,Map[String,String]]]()

      test.getName -> (for (file <- fileList) yield {
        logger.info(s"Looking at file: $file")
        val info = resultsMap.getOrElse(file.getName.stripSuffix(".meta.xml"), Map.empty[String, Map[String, String]]);
        if (info.nonEmpty) {
          test.apply(file, dir, info, dictFile)
        } else {
          val emptyResults = new AuthorEmailFilterResults(file.getName)
          emptyResults.registerErrorMsg("No expected information found for this file. Unable to compare.  Is this a good file?")
          emptyResults
        }
      }).toList
    }).toMap

		if (outfile.nonEmpty) {
		  val ps = new PrintStream(outfile)
			printSummary(results, ps)
			ps.close()
		} else {
		  printSummary(results, System.out)
	  }
  }

	def printSummary(results: Map[String, List[TestFilterResults]], stream : PrintStream) = {

    testList.foreach (test => {
      test.printSummary(results(test.getName), stream)
    })
	}

  def getFileList(dir: File, suffix: String): Array[File] = {
    val these = dir.listFiles

    val reg = ("""^.*""" + suffix + """$""").r
    these.filter(f => reg.findFirstIn(f.getName).isDefined)
  }
}
