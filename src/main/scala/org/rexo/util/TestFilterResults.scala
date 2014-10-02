package org.rexo.util

import java.io.{File, PrintStream}

import org.rexo.pipelinescala.extractors.Author

import scala.collection.immutable.List

abstract class TestFilterResults(filename : String, filtername: String) {
  val name = filtername
  var totalSamples = 0
  var fullSuccesses: Int = 0
  // filter found a match, but it wasn't the expected results
  var falseMatches: Int = 0
  //val time_ms: Double = 0 // unused

  def upSampleCount() = {
    totalSamples += 1
  }

  def registerSuccess() = {
    fullSuccesses += 1
  }

  def registerFalseMatch() = {
    falseMatches += 1
  }

  def prettyPrint(stream: PrintStream)

  def nonEmpty : Boolean

  def addToTally(tally: Map[String,Float]) : Map[String, Float] = {
    Map[String, Float] ("totalSamples" -> (tally.getOrElse("totalSamples", 0F) + totalSamples),
     "totalSuccesses"-> (tally.getOrElse("totalSuccesses", 0F)+ fullSuccesses),
     "totalFalseMatches" -> (tally.getOrElse("totalFalseMatches", 0F)+ falseMatches)
    )
  }
}


abstract class TestFilter() {
  def apply(XMLfile : File, directory: String, expectedResults : Map[String,Map[String,String]], instDict: String) : TestFilterResults

  def getName : String

  def printSummary(results: List[TestFilterResults], stream : PrintStream)

  def parseCSVData(csvFilename : String) : Map[String, Map[String, Map[String, String]]]

}

