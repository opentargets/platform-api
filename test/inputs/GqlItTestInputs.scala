package inputs

import models.entities.AggregationFilter
import org.scalacheck.Gen

import scala.reflect.io.File

trait GqlItTestInputs {

  lazy val geneInputs = File(this.getClass.getResource(s"/gqlInputs/genes.txt").getPath).lines.toList
  lazy val diseaseInputs = File(this.getClass.getResource(s"/gqlInputs/efos.txt").getPath).lines.toList
  lazy val drugInputs = File(this.getClass.getResource(s"/gqlInputs/drugs.txt").getPath).lines.toList
  lazy val goInputs = File(this.getClass.getResource(s"/gqlInputs/goIds.txt").getPath).lines.toList

  val aggregationFilterMap: Map[String, Seq[String]] = Map(
    "pathwayTypes" -> Seq(
      "Autophagy",
      "Cell Cycle",
      "Cell-Cell communication",
      "Cellular responses to external stimuli",
      "Chromatin organization",
      "Circadian Clock",
      "Developmental Biology",
      "Digestion and absorption",
      "Disease",
      "DNA Repair",
      "DNA Replication",
      "Extracellular matrix organization",
      "Gene expression (Transcription)",
      "Hemostasis",
      "Immune System",
      "Metabolism",
      "Metabolism of proteins",
      "Metabolism of RNA",
      "Muscle contraction",
      "Neuronal System",
      "Organelle biogenesis and maintenance",
      "Programmed Cell Death",
      "Protein localization",
      "Reproduction",
      "Sensory Perception",
      "Signal Transduction",
      "Transport of small molecules",
      "Vesicle-mediated transport"
    ),
    "targetClasses" -> Seq(
      "Adhesion",
      "Auxiliary transport protein",
      "Enzyme",
      "Epigenetic regulator",
      "Ion channel",
      "Membrane receptor",
      "Other cytosolic protein",
      "Other membrane protein",
      "Other nuclear protein",
      "Secreted protein",
      "Surface antigen",
      "Transcription factor",
      "Transporter",
      "Unclassified protein",
      "Structural protein"
    ),
    "dataTypes" -> Seq("Genetic associations",
      "Drugs",
      "Text mining",
      "RNA expression",
      "Animal models",
      "Somatic mutations"),
    "tractabilityAntibody" -> Seq("Clinical precedence",
      "Predicted tractable high confidence",
      "Predicted tractable med low confidence"),
    "tractabilitySmallMolecule" -> Seq("Clinical precedence",
      "Discovery precedence",
      "Predicted tractable")
  )

  // Generators
  val aggregationfilterGenerator: Gen[AggregationFilter] = {
    for {
      name <- Gen.oneOf(aggregationFilterMap.keySet)
      paths <- Gen.someOf(aggregationFilterMap(name))
    } yield AggregationFilter(name, paths)
  }

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
