package inputs

import models.entities.AggregationFilter
import org.scalacheck.Gen

import scala.io.{BufferedSource, Source}

trait GqlItTestInputs {

  val geneFile: BufferedSource =
    Source.fromFile(this.getClass.getResource(s"/gqlInputs/genes.txt").getPath)
  lazy val geneInputs: Seq[String] = geneFile.getLines().toList
  val efoFile: BufferedSource =
    Source.fromFile(this.getClass.getResource(s"/gqlInputs/efos.txt").getPath)
  lazy val diseaseInputs: Seq[String] = efoFile.getLines().toList
  val drugFile: BufferedSource =
    Source.fromFile(this.getClass.getResource(s"/gqlInputs/drugs.txt").getPath)
  lazy val drugInputs: Seq[String] = drugFile.getLines().toList
  val goFile: BufferedSource =
    Source.fromFile(this.getClass.getResource(s"/gqlInputs/goIds.txt").getPath)
  lazy val goInputs: Seq[String] = goFile.getLines().toList

  // Generators
  val geneGenerator: Gen[String] = Gen.oneOf(geneInputs)
  val diseaseGenerator: Gen[String] = Gen.oneOf(diseaseInputs)
  val drugGenerator: Gen[String] = Gen.oneOf(drugInputs)
  val goGenerator: Gen[String] = Gen.oneOf(goInputs)
  val goListGenerator: Gen[List[String]] = Gen.listOf(goGenerator)
  val sizeGenerator: Gen[Int] = Gen.chooseNum(1, 10)
  val targetDiseaseSizeGenerator: Gen[(String, String, Int)] = for {
    gene <- geneGenerator
    disease <- diseaseGenerator
    size <- sizeGenerator
  } yield (gene, disease, size)
  val targetDiseaseGenerator: Gen[(String, String)] = for {
    gene <- geneGenerator
    disease <- diseaseGenerator
  } yield (gene, disease)

  val searchGenerator: Gen[String] = Gen.oneOf(geneGenerator, diseaseGenerator)
}
