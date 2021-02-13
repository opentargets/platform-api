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
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.8.0"
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.2"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test

val playVersion = "2.8.2"
libraryDependencies += "com.typesafe.play" %% "play" % playVersion
libraryDependencies += "com.typesafe.play" %% "filters-helpers" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-logback" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.1"
libraryDependencies += "com.typesafe.play" %% "play-streams" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.0.0"
libraryDependencies += "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.2.5"
libraryDependencies += "org.sangria-graphql" %% "sangria" % "2.0.1"
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "2.0.1"

val s4sVersion = "7.7.0"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % s4sVersion
)
// Google app engine logging
libraryDependencies += "com.google.cloud" % "google-cloud-logging-logback" % "0.119.7-alpha"

