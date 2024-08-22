import com.typesafe.sbt.packager.MappingsHelper._
import scala.language.postfixOps
import scala.sys.process._
import sbt._

name := """ot-platform-api"""
organization := "io.opentargets"

version := "latest"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayLogback)

scalaVersion := "2.13.10"
maintainer := "ops@opentargets.org"

javacOptions ++= Seq("-encoding", "UTF-8")

scalacOptions in ThisBuild ++= Seq(
  "-language:_",
  "-Xfatal-warnings"
)
scalacOptions in Compile += "-deprecation"

// include resources into the unversal zipped package
mappings in Universal ++= directory(baseDirectory.value / "resources")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  guice,
  caffeine,
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % Test
)

val playVersion = "2.9.5"
libraryDependencies += "com.typesafe.play" %% "play" % playVersion
libraryDependencies += "com.typesafe.play" %% "filters-helpers" % "2.8.21"
libraryDependencies += "com.typesafe.play" %% "play-logback" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.5"
libraryDependencies += "com.typesafe.play" %% "play-streams" % playVersion
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.1.0"

val sangriaVersion = "4.1.1"
libraryDependencies += "com.clickhouse" % "clickhouse-jdbc" % "0.3.2"
libraryDependencies += "org.sangria-graphql" %% "sangria" % sangriaVersion
libraryDependencies += "org.sangria-graphql" %% "sangria-play-json" % "2.0.2"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.12"

lazy val catsVersion = "2.12.0"
lazy val cats = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-laws" % catsVersion,
  "org.typelevel" %% "cats-kernel" % catsVersion,
  "org.typelevel" %% "cats-kernel-laws" % catsVersion
)
libraryDependencies ++= cats

val s4sVersion = "8.11.3"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % s4sVersion exclude ("org.slf4j", "slf4j-api"),
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % s4sVersion exclude ("org.slf4j", "slf4j-api"),
  "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % s4sVersion exclude ("org.slf4j", "slf4j-api"),
  "com.sksamuel.elastic4s" %% "elastic4s-json-play" % s4sVersion exclude ("org.slf4j", "slf4j-api")
)

lazy val frontendRepository = settingKey[String]("Git repository with open targets front end.")
lazy val gqlFileDir = settingKey[File]("Location to save test input queries")
lazy val getGqlFiles = taskKey[Unit]("Add *.gql files from frontendRepository to test resources")
lazy val updateGqlFiles = taskKey[Unit]("Report which files are new and which have been updated.")

frontendRepository := "https://github.com/opentargets/ot-ui-apps.git"
gqlFileDir := (Test / resourceDirectory).value / "gqlQueries"

getGqlFiles := {
  sbt.IO.withTemporaryDirectory { td =>
    // copy files
    Process(s"git clone ${frontendRepository.value} ${td.getAbsolutePath}") !
    // filter files of interest
    val gqlFiles: Seq[File] = (td / "apps/platform/" ** "*.gql").get ++ (td / "packages/sections/" ** "*.gql").get

    // delete files in current gql test resources so we can identify when the FE deletes a file
    val filesToDelete: Seq[File] =
      sbt.IO.listFiles(gqlFileDir.value, NameFilter.fnToNameFilter(!_.contains("full")))
    sbt.IO.delete(filesToDelete)
    // move files to test resources
    sbt.IO.copy(gqlFiles.map(f => (f, gqlFileDir.value / s"${f.getParentFile.name}_${f.name}")))
  }
}

updateGqlFiles := {
  // trigger update
  val a: Unit = getGqlFiles.value

  def gitStatusOpt(option: String): Seq[String] =
    Process(s"git status -$option ${gqlFileDir.value.getAbsolutePath}").lineStream
      .filter(_.contains((Test / resourceDirectory).value.getName))

  val newFiles = gitStatusOpt("u")
  val updatedFiles = gitStatusOpt("uno")

  if (newFiles.nonEmpty) {
    println("New files found:")
    newFiles.filterNot(f => updatedFiles.contains(f)).foreach(println)
  } else {
    println("No new files found since last update.")
  }

  if (updatedFiles.nonEmpty) {
    println("Files updated since last refresh:")
    updatedFiles.foreach(println)
  } else {
    println("No existing files have been updated since last check.")
  }

}
