import sbt._
import sbt.Keys._

object YadbBuild extends Build {
  import Dependencies._

  lazy val main = Project(id = "Yadb", base = file("."))
    .settings(Seq(
      libraryDependencies ++= List(akkaActor, akkaPersistence, playJson) ++ List(scalaTest).map(_ % Test),
      organization := "de.blogspot.volkersyadb",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.4",
      scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-feature",
        "-language:higherKinds", "-language:implicitConversions")): _*)
}

object Dependencies {

  val akkaActor = akkaModule("akka-actor")
  val akkaPersistence = akkaModule("akka-persistence-experimental")
  val playJson = "com.typesafe.play" %% "play-json" % "2.4.0-M1"
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1"

  private def akkaModule(artifactId: String): ModuleID =
    "com.typesafe.akka" %% artifactId % "2.3.6"
}
