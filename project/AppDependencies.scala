import sbt.*

object AppDependencies {

  private val bootstrapVersion    = "9.13.0"
  private val hmrcMongoVersion    = "2.6.0"
  private val internalAuthVersion = "4.0.0"
  private val playSuffix          = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-backend-$playSuffix"    % bootstrapVersion,
    "uk.gov.hmrc"         %% s"internal-auth-client-$playSuffix" % internalAuthVersion,
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo-$playSuffix"           % hmrcMongoVersion,
    "org.quartz-scheduler" % "quartz"                            % "2.5.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playSuffix"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"http-verbs-test-$playSuffix" % "15.2.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)
}
