
object releaseConfig {

  import sbtrelease.ReleasePlugin._
  import sbtrelease._
  import ReleaseStateTransformations._

  val releaseSteps = Seq[ReleaseStep](
    checkSnapshotDependencies,              // : ReleaseStep
    inquireVersions,                        // : ReleaseStep
    runTest,                                // : ReleaseStep
    setReleaseVersion,                      // : ReleaseStep
    commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
    tagRelease,                             // : ReleaseStep
    // publishArtifacts,                       // : ReleaseStep, checks whether `publishTo` is properly set up
    setNextVersion,                         // : ReleaseStep
    commitNextVersion,                      // : ReleaseStep
    pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
  )

}
