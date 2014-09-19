package org.rexo.util

// quick and dirty command line argument parser...
// assumption is arguments are one character like this:
// progname -a -d <directory> -f <file>
// todo- make this more robust at some point

object ParseArgs {

  def parseArgs(progname : String, args : Array[String], validArgs : String, usage : () => Unit) : scala.collection.mutable.Map[String,String] = {
    var map = scala.collection.mutable.Map[String, String]()
    val stack = scala.collection.mutable.Stack[String](args: _*) // _* factory method to make the args array viewed as list of strings

    println("argument stack is:")
    stack.foreach(println(_))

    if (stack.isEmpty) {
      usage()
      sys.error("Missing arguments") // exit
    }

    val reg = validArgs.r()
    val len = validArgs.length

    try {
      do {
        val arg = stack.pop() //stack.headOption.getOrElse("unknown")
        println(s"arg is $arg")
        arg match {
          case `progname` => // ignore
          case _ =>
            val index = validArgs.indexOf(arg.stripPrefix("-"))
            if (index != -1) {
              if(len > index+1 && validArgs.charAt(index + 1) == ':')
                map += arg -> stack.pop()
              else
                map += arg -> ""  // just a switch, but still need something in the hash map
            } else {
              // unknown argument
              println("Unknown argument: " + arg)
              usage(); //sys.error(s"Unknown argument: $arg")
            }
        }
      } while (stack.nonEmpty)

      map

    } catch {
      case e: NoSuchElementException => usage(); System.err.print("Missing argument: " + e); map
      case e: Exception => System.err.print("Unable to parse command line arguments: " + e); map
    }
  }
}


