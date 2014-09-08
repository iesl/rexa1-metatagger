resolvers += "IESL Public Releases" at "https://dev-iesl.cs.umass.edu/nexus/content/groups/public"

addSbtPlugin("edu.umass.cs.iesl" %% "iesl-sbt-base" % "latest.release")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.4")
