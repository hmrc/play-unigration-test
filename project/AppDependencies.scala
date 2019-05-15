import sbt._

object AppDependencies {

  val compile = Seq(
    "org.jsoup" % "jsoup" % "1.6.1",
    "org.scalatest" %% "scalatest" % "3.0.4"
  )

  val test = Seq(
    "org.pegdown" % "pegdown" % "1.6.0" % "test"
  )

}
