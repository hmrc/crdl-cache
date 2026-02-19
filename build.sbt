import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"

lazy val microservice = Project("crdl-cache", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(CodeCoverageSettings.settings *)
  .settings(
    PlayKeys.playDefaultPort := 7252,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      //Ignore unused import error for generated OpenApi file
      "-Wconf:msg=unused import&src=.*OpenApi.template.scala:s",
      // Disable duplicate compiler option warning as it's caused by our sbt plugins
      "-Wconf:msg=Flag.*repeatedly:s",
      // Ignore test-only code
      "--coverage-exclude-classlikes:uk.gov.hmrc.crdlcache.controllers.testonly",
      // Ignore test-only API documentation view
      "--coverage-exclude-classlikes:uk.gov.hmrc.crdlcache.views.html"
    ),
    routesImport ++= Seq(
      "java.time.Instant",
      "java.time.LocalDate",
      "play.api.libs.json.JsValue",
      "uk.gov.hmrc.crdlcache.models.*",
      "uk.gov.hmrc.crdlcache.models.Binders.bindableInstant",
      "uk.gov.hmrc.crdlcache.models.Binders.bindableLocalDate",
      "uk.gov.hmrc.crdlcache.models.Binders.bindableJsValueMap",
      "uk.gov.hmrc.crdlcache.models.Binders.bindableSet"
    ),
    // Change classloader layering to avert classloading issues
    Compile / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .disablePlugins(JUnitXmlReportPlugin)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies.it,
    // Change classloader layering to avert classloading issues
    Compile / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    // Disable duplicate compiler option warning as it's caused by our sbt plugins
    scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"
  )

addCommandAlias("runAllChecks", ";clean;compile;scalafmtAll;coverage;test;it/test;coverageReport")
