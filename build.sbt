import com.typesafe.sbt.packager.MappingsHelper._

name := """ot-platform-api-beta"""
organization := "io.opentargets"

version := "latest"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayLogback)

scalaVersion := "2.12.10"
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

libraryDependencies += guice
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.8.0"
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.2"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
val playVersion = "2.7.3"
libraryDependencies += "com.typesafe.play" %% "play" % playVersion
libraryDependencies += "com.typesafe.play" %% "filters-helpers" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-logback" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-slick" % "4.0.2"
libraryDependencies += "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.2"
libraryDependencies += "org.sangria-graphql" %% "sangria" % "2.0.0-M3"
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "2.0.0"

val s4sVersion = "7.3.1"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % s4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % s4sVersion
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.opentargets.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.opentargets.binders._"
// uri length for dev mode
// PlayKeys.devSettings := Seq("play.akka.dev-mode.akka.http.parsing.max-uri-length" -> "16k")
