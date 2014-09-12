package org.rexo.util

import org.slf4j.LoggerFactory

/**
 * This is a class to help keep some simple metrics about a program and how it's running.
 * This will give us an idea of timing, but if we want to be more accurate we should probably
 * switch to using Metrics Core or Criterium (might be Java only)
 * @param project
 */
class Metrics (project : String, successTypes: List[String]) {
  val logger = LoggerFactory.getLogger(Metrics.getClass())
  val projectName = project
  val success = scala.collection.mutable.Map[String, Int]((for (t <- successTypes) yield { (t,0) }):_*)
  val failure = scala.collection.mutable.Map[String, String]()
  var timestampMap = scala.collection.mutable.Map[String, (Long, Long)]()

  def reset() {
		resetSuccess()
    failure.clear()
    timestampMap.clear()
  }

	private def resetSuccess() {
		successTypes.foreach(st => success(st) = 0)
	}

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

	def logSuccess(successType: String) {
		if( success.get(successType).nonEmpty ) {
			success(successType) += 1
		} else {
			logger.warn(s"Registering success on non existent type '$successType'")
		}
	}

  def logFailure(fname: String, errMsg: String) : Unit = {
    if (fname != "" && errMsg != "") {
      failure += fname -> errMsg
    }
  }

  def successCount(successType :String) : Int = {success.getOrElse(successType, -1)}

  def failureCount() : Int = {failure.size}

  def summary() : String = {
      "Success summary: \n" + (for((key,value) <- success) yield {s"\t$key : $value\n"}).mkString +
      s"Failed with ${failure.size} error message(s): \n" +
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
    val metric = new Metrics(projectName, List("Full", "Partial"))
    metric.logStart("program run time")

    metric.logStart("success")
    metric.logSuccess("Full")
    metric.logStop("success")
		metric.logStart("partial success")
    metric.logSuccess("Partial")
		metric.logStop("partial success")
    metric.logStart("failure")
    metric.logFailure("foo.txt", "file does not exist")
    metric.logStop("failure")

    //metric.getFunctionTime(metric.logSuccessFull())
    metric.logStop("program run time")

    logger.info(metric.summary())
  }
}
