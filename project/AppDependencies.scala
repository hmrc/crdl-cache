import sbt.*

object AppDependencies {

  private val bootstrapVersion    = "9.19.0"
  private val hmrcMongoVersion    = "2.12.0"
  private val internalAuthVersion = "4.3.0"
  private val httpVerbsVersion    = "15.7.0"
  private val quartzVersion       = "2.5.0"
  private val playSuffix          = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-backend-$playSuffix"    % bootstrapVersion,
    "uk.gov.hmrc"         %% s"internal-auth-client-$playSuffix" % internalAuthVersion,
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo-$playSuffix"           % hmrcMongoVersion,
    "org.quartz-scheduler" % "quartz"                            % quartzVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playSuffix"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"http-verbs-test-$playSuffix" % httpVerbsVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)
}
