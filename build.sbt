import play.core.PlayVersion.current
import sbt._

val nameApp = "play-unigration-test"

lazy val simpleReactiveMongo = Project(nameApp, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    scalaVersion := "2.11.12",
    resolvers += Resolver.jcenterRepo,
    makePublicallyAvailableOnBintray := true,
    majorVersion := 0
  )

libraryDependencies ++= Seq(
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.12.0",
  "uk.gov.hmrc" %% "jsoup-should-matchers" % "0.3.0",
  "org.scalatest" %% "scalatest" % "3.0.4",
  "org.mockito" %  "mockito-core" % "2.23.4",
  "uk.gov.hmrc" %% "http-caching-client" % "8.4.0-play-25",

  // NB. make sure these are all kept in sync with play version
  "com.typesafe.play" %% "play" % current,
  "com.typesafe.play" %% "play-test" % current,
  "uk.gov.hmrc" %% "http-verbs" % "9.7.0-play-25",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0",

  "org.pegdown" %  "pegdown" % "1.6.0" % "test",
  "uk.gov.hmrc" %% "emailaddress" % "3.2.0" % "test"
)
