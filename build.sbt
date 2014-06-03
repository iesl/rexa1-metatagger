scalaVersion := "2.10.3"

libraryDependencies ++=  Seq(
  "org.jdom" % "jdom" % "1.1",
  "junit" % "junit" % "4.10" % "test",
  "commons-io" %  "commons-io" % "2.3",
  "commons-collections" % "commons-collections" % "3.2.1",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-logging" % "commons-logging" % "1.1.3",
  "commons-logging" % "commons-logging-api" % "1.1",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "log4j" % "log4j" % "1.2.17",
  "junit" % "junit" % "4.11"
)

//javacOptions ++= Seq("-Xlint:all")
            
