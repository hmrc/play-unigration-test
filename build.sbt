import play.core.PlayVersion.current
import sbt._

val nameApp = "play-unigration-test"

lazy val simpleReactiveMongo = Project(nameApp, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    scalaVersion := "2.11.12",
    resolvers += Resolver.jcenterRepo,
    crossScalaVersions := Seq("2.11.12", "2.12.6"),
    makePublicallyAvailableOnBintray := true,
    majorVersion := 0
  ).settings(scoverageSettings)

libraryDependencies ++= Seq(
  "uk.gov.hmrc" %% "jsoup-should-matchers" % "0.3.0",
  "org.scalatest" %% "scalatest" % "3.0.4",

  // NB. make sure these are all kept in sync with play version
  "com.typesafe.play" %% "play" % current,
  "com.typesafe.play" %% "play-test" % current,
  "uk.gov.hmrc" %% "http-verbs" % "9.7.0-play-25",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0",

  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "uk.gov.hmrc" %% "emailaddress" % "3.2.0" % "test"
)

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageEnabled := true,
  coverageExcludedPackages := List("<empty>").mkString(";"),
  coverageMinimum := 80,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)
