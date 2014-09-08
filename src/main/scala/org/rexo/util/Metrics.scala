package org.rexo.util

import org.slf4j.LoggerFactory

/**
 * This is a class to help keep some simple metrics about a program and how it's running.
 * This will give us an idea of timing, but if we want to be more accurate we should probably
 * switch to using Metrics Core or Criterium (might be Java only)
 * @param project
 */
class Metrics (project : String) {
  val logger = LoggerFactory.getLogger(Metrics.getClass())
  val projectName = project
  var success : Int = 0
  var failure = scala.collection.mutable.Map[String, String]()
  var timestampMap = scala.collection.mutable.Map[String, (Long, Long)]()

  def logStart(tag: String) : Unit = {
    timestampMap += tag -> (System.nanoTime(), -1.asInstanceOf[Long])
  }

  def logStop(tag: String) : Unit = {
    //val tagInfo = timestampMap.get(tag)
    val timeval = timestampMap.getOrElse(tag, (-1L, -1L))

    if (timeval._1 != -1) {
      timestampMap += tag -> (timeval._1, System.nanoTime())
    }
  }

  def getTimeMS(tag: String) : Double = {
    val timeval = timestampMap.getOrElse(tag, (-1L,-1L))
    (timeval._2 - timeval._1) / 1e6
  }

  // tell the time it takes to run a function
  def getFunctionTime[A](func: => A) = {
    val s = System.nanoTime()
    val ret = func
    logger.info("time: " + (System.nanoTime() - s) / 1e6 + "ms")
    ret
  }

  def logSuccess() : Unit = { success += 1 }

  def logFailure(fname: String, errMsg: String) : Unit = {
    if (fname != "" && errMsg != "") {
      failure += fname -> errMsg
    }
  }

  def successCount() : Int = {success}
  def failureCount() : Int = {failure.size}

  def summary() : String = {
    s"Successfully matched $success emails(s).\n"+
      s"Failed on the following ${failure.size}: \n" +
      (for ((key,value) <- failure) yield {s"\t$key: $value\n"}).toList.mkString  +
      "Time Values: \n" +
      (for ((key,(start, stop)) <- timestampMap) yield {s"\t$key: " + this.getTimeMS(key) +  "ms\n"}).toList.mkString
  }
}

object Metrics {
  val logger = LoggerFactory.getLogger(Metrics.getClass())
  def main(args : Array[String]) {
    TestMetric("Test Run")
  }

  def TestMetric (projectName : String) : Unit = {
    val metric = new Metrics(projectName)
    metric.logStart("program run time")

    metric.logStart("success")
    metric.logSuccess()
    metric.logStop("success")
    metric.logStart("failure")
    metric.logFailure("foo.txt", "file does not exist")
    metric.logStop("failure")

    //metric.getFunctionTime(metric.logSuccess())
    metric.logStop("program run time")

    logger.info(metric.summary())
  }
}
