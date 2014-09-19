package org.rexo.util

import java.io.{File, PrintStream}

import org.rexo.ui.{Institution, Email, Author}

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

  def addToTally(tally: Map[String,Float]) : Map[String, Float] = {
    Map[String, Float] ("totalSamples" -> (tally("totalSamples") + totalSamples),
     "totalSuccesses"-> (tally("totalSuccesses")+ fullSuccesses),
     "totalFalseMatches" -> (tally("totalFalseMatches")+ falseMatches)
    )
  }
}


abstract class TestFilter() {
  def apply(XMLfile : File, expectedResults : Map[String,Map[String,String]], instDict: String) : TestFilterResults

  def getName : String

}

