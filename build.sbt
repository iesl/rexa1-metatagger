scalaVersion := "2.10.4"

organization := "edu.umass.cs.iesl"

name := "rexa1-metatagger"

// TODO: add mallet to these once the proper jar is in nexus
libraryDependencies ++=  Seq(
  "org.jdom" % "jdom" % "1.1",
  "junit" % "junit" % "4.10" % "test",
  "commons-io" %  "commons-io" % "2.3",
  "commons-collections" % "commons-collections" % "3.2.1",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-logging" % "commons-logging" % "1.1.3",
  "commons-logging" % "commons-logging-api" % "1.1",
  "commons-cli" % "commons-cli" % "1.0",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "log4j" % "log4j" % "1.2.17",
  "junit" % "junit" % "4.11",
  "com.scalatags" %% "scalatags" % "0.4.0",
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "edu.umass.cs.iesl" %% "scalacommons" % "latest.integration"
)

resolvers ++= Seq(
  "IESL Public Snapshots" at "http://dev-iesl.cs.umass.edu/nexus/content/groups/public-snapshots",
  "IESL Public Releases" at "http://dev-iesl.cs.umass.edu/nexus/content/groups/public"
)


javacOptions ++= Seq("-Xlint:-rawtypes", "-Xlint:-unchecked")

releaseSettings

ReleaseKeys.releaseProcess := releaseConfig.releaseSteps

  //ReleaseKeys.releaseProcess := Seq[ReleaseStep](
