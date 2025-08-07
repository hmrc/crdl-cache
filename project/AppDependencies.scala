import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val hmrcMongoVersion = "2.7.0"
  private val httpVerbsVersion = "15.4.0"
  private val quartzVersion    = "2.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-30"        % hmrcMongoVersion,
    "org.quartz-scheduler" % "quartz"                    % quartzVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "http-verbs-test-play-30" % httpVerbsVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion
  ).map(_ % Test)
}
