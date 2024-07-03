package inputs

import org.scalacheck.Gen

import scala.reflect.io.File

trait GqlItTestInputs {

  lazy val geneInputs =
    File(this.getClass.getResource(s"/gqlInputs/genes.txt").getPath).lines().toList
  lazy val diseaseInputs =
    File(this.getClass.getResource(s"/gqlInputs/efos.txt").getPath).lines().toList
  lazy val drugInputs =
    File(this.getClass.getResource(s"/gqlInputs/drugs.txt").getPath).lines().toList
  lazy val goInputs =
    File(this.getClass.getResource(s"/gqlInputs/goIds.txt").getPath).lines().toList

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
