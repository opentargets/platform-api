import com.typesafe.sbt.packager.MappingsHelper._

name := """ot-platform-api-beta"""
organization := "io.opentargets"

version := "latest"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayLogback)

scalaVersion := "2.12.12"
maintainer := "ops@opentargets.org"

javacOptions ++= Seq( "-encoding", "UTF-8" )

scalacOptions in ThisBuild ++= Seq(
"-language:_",
"-Ypartial-unification",
"-Xfatal-warnings"
)


// include resources into the unversal zipped package
mappings in Universal ++= directory(baseDirectory.value / "resources")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(guice, caffeine)
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.1"
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.3"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

val playVersion = "2.8.6"
libraryDependencies += "com.typesafe.play" %% "play" % playVersion
libraryDependencies += "com.typesafe.play" %% "filters-helpers" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-logback" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"
libraryDependencies += "com.typesafe.play" %% "play-streams" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.0.0"

val sangriaVersion = "2.1.0"
libraryDependencies += "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.2.6"
libraryDependencies += "org.sangria-graphql" %% "sangria" % sangriaVersion
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "2.0.1"

lazy val catsVersion = "2.4.2"
lazy val cats = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion,
  "org.typelevel" %% "cats-kernel" % catsVersion,
  "org.typelevel" %% "cats-kernel-laws" % catsVersion
)
libraryDependencies ++= cats

lazy val monocleVersion = "2.1.0"
lazy val monocle = Seq(
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion
)
libraryDependencies++= monocle

val s4sVersion = "7.9.2"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % s4sVersion
)
// Google app engine logging
libraryDependencies += "com.google.cloud" % "google-cloud-logging-logback" % "0.120.0-alpha"

import scala.sys.process._
import sbt._

lazy val frontendRepository = settingKey[String]("Git repository with open targets front end.")
lazy val getGqlFiles = taskKey[Unit]("Add *.gql files from frontendRepository to test resources")

frontendRepository := "https://github.com/opentargets/platform-app.git"

getGqlFiles := {
  sbt.IO.withTemporaryDirectory(td => {
    // copy files
    Process(s"git clone ${frontendRepository.value} ${td.getAbsolutePath}") !
    // filter files of interest
    val gqlFiles: Seq[File] = (td ** "*.gql").get

    // move files to test resources
    val dest = (Test / resourceDirectory).value / "gqlQueries"
    sbt.IO.copy(gqlFiles.map(f => (f, dest / s"${f.getParentFile.name}_${f.name}")))
  })
}



