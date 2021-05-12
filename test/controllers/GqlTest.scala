package controllers

import akka.util.Timeout
import controllers.GqlItTestInputs.{diseaseGenerator, geneGenerator, searchGenerator, sizeGenerator, targetDiseaseSizeGenerator}
import controllers.GqlTest.{createRequest, generateQueryString, getQueryFromFile}
import controllers.api.v4.graphql.GraphQLController
import org.scalacheck.{Gen, Shrink}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Logging
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.reflect.io.File

object GqlItTestInputs {

  lazy val geneInputs = File(this.getClass.getResource(s"/gqlInputs/genes.txt").getPath).lines.toList
  lazy val diseaseInputs = File(this.getClass.getResource(s"/gqlInputs/efos.txt").getPath).lines.toList
  lazy val drugInputs = Seq()

  // Generators
  val geneGenerator: Gen[String] = Gen.oneOf(geneInputs)
  val diseaseGenerator: Gen[String] = Gen.oneOf(diseaseInputs)
  val sizeGenerator: Gen[Int] = Gen.chooseNum(1, 10)
  val targetDiseaseSizeGenerator: Gen[(String, String, Int)] = for {
    gene <- geneGenerator
    disease <- diseaseGenerator
    size <- sizeGenerator
  } yield (gene, disease, size)

  val searchGenerator: Gen[String] = Gen.oneOf(geneGenerator, diseaseGenerator)
}

sealed trait GqlCase[T] {
  val file: String
  val inputGenerator: Gen[T]

  def generateVariables(inputs: T): String
}

case class Search(file: String) extends GqlCase[String] {
  override val inputGenerator = searchGenerator

  def generateVariables(searchTerm: String): String = {
    s"""
      "variables": {
      "queryString": "$searchTerm"
    }
    """
  }
}

case class SearchPage(file: String) extends GqlCase[(String, String, Int)] {
  val inputGenerator = for {
    query <- searchGenerator
    entities <- Gen.atLeastOne(Seq("target", "disease", "drug")).map(_.mkString("[\"", "\", \"", "\"]"))
    page <- sizeGenerator
  } yield (query, entities, page)

  def generateVariables(inputs: (String, String, Int)): String =
    s"""
      "variables": {
      "queryString": "${inputs._1}",
      "index": ${inputs._3},
      "entityNames": ${inputs._2}
    }
    """
}

case class Target(file: String) extends GqlCase[String] {

  val inputGenerator = geneGenerator

  def generateVariables(target: String): String = {
    s"""
      "variables": {
      "ensgId": "$target"
    }
    """
  }
}

case class TargetDisease(file: String) extends GqlCase[(String, String)] {
  val inputGenerator = for {
    gene <- geneGenerator
    disease <- diseaseGenerator
  } yield (gene, disease)

  def generateVariables(inputs: (String, String)): String = {
    val (target, disease) = inputs
    s"""
      "variables": {
      "efoId": "$target",
      "ensemblId": "$disease"
    }
    """
  }
}

case class TargetDiseaseSize(file: String) extends GqlCase[(String, String, Int)] {
  val inputGenerator = targetDiseaseSizeGenerator

  def generateVariables(input: (String, String, Int)): String = generateVariables(input._1, input._2, input._3)

  def generateVariables(target: String, disease: String, size: Int): String = {
    s"""
      "variables": {
      "efoId": "$target",
      "ensemblId": "$disease",
      "size": $size
    }
    """
  }
}

case class TargetDiseaseSizeCursor(file: String) extends GqlCase[(String, String, Int)] {
  val inputGenerator = targetDiseaseSizeGenerator

  def generateVariables(input: (String, String, Int)): String = generateVariables(input._1, input._2, input._3)

  def generateVariables(target: String, disease: String, size: Int): String = {
    s"""
    "variables": {
      "efoId": "$target",
      "ensemblId": "$disease",
      "size": $size,
      "cursor": null
    }
  """
  }
}

object GqlTest {

  /**
   * Retrieves filename from target resources and returns query as string with quotes escaped.
   */
  def getQueryFromFile(filename: String): String = {
    File(this.getClass.getResource(s"/gqlQueries/$filename.gql").getPath).lines
      .withFilter(_.nonEmpty)
      .map(str =>
        str.flatMap {
          case '"' => "\\\""
          case ch => s"$ch"
        })
      .mkString("\\n")
  }

  def generateQueryString(query: String, variables: String): JsValue =
    Json.parse(s"""{"query": "$query" , $variables }""")

  def createRequest(query: JsValue): Request[JsValue] = {
    FakeRequest(POST, "/graphql")
      .withHeaders(("Content-Type", "application/json"))
      .withBody(query)
  }
}

class GqlTest
  extends PlaySpec
    with GuiceOneAppPerTest
    with Injecting
    with Logging
    with ScalaFutures with ScalaCheckDrivenPropertyChecks {

  lazy val controller: GraphQLController = inject[GraphQLController]
  implicit lazy val timeout: Timeout = Timeout(1120, TimeUnit.SECONDS)

  /*
  Use no shrink for all inputs as the standard shrinker will start cutting our inputs strings giving non-sense queries.
  In pure scalaCheck we could use `forAllNoShrink` but this isn't available with the Play wrapper. If we want to use
  scalacheck more broadly we should move the implicit into a tighter scope.
   */
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  def testQueryAgainstGqlEndpoint[T](gqlCase: GqlCase[T]): Unit = {
    forAll(gqlCase.inputGenerator) { i =>
      // get query from front end
      val query = getQueryFromFile(gqlCase.file)
      // create test variables
      val variables = gqlCase.generateVariables(i)
      // create and execute request
      val request = createRequest(generateQueryString(query, variables))
      val resultF: Future[Result] = controller.gqlBody().apply(request)
      val resultS = contentAsString(resultF)

      // validate results
      val resultHasNoErrors = !resultS.contains("errors")
      if (!resultHasNoErrors) {
        logger.error(s"Input: $i returned with error: $resultS")
      }
      resultHasNoErrors mustBe true
    }
  }

  "Cancer gene census queries" must {
    "return a valid response" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("CancerGeneCensus_sectionQuery"))
    }
  }

  "EuropePmc queries" must {
    "return a valid response" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSizeCursor("EuropePmc_sectionQuery"))
    }
  }

  "Gene2Phenotype_sectionQuery" must {
    "return a valid response" in {
      testQueryAgainstGqlEndpoint(TargetDisease("Gene2Phenotype_sectionQuery"))
    }
  }

  "GenomicsEngland_sectionQuery" must {
    "return a valid response" in {
      testQueryAgainstGqlEndpoint(TargetDisease("GenomicsEngland_sectionQuery"))
    }
  }

  "IntOgen_sectionQuery" must {
    "return a valid response" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("IntOgen_sectionQuery"))
    }
  }
  /* todo: find out what source databank is: requires
  query InteractionsSectionQuery(
  $ensgId: String!
  $sourceDatabase: String
  $index: Int = 0
  $size: Int = 10
) {
   */
  //  "MolecularInteractions_InteractionsQuery" must {
  //    "return a valid response" in {
  //      testQueryAgainstGqlEndpoint(TargetDisease("MolecularInteractions_InteractionsQuery"))
  //    }
  //  }

  "OT genetics queries" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("OTGenetics_sectionQuery"))
    }
  }

  "Phenodigm_sectionQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Phenodigm_sectionQuery"))
    }
  }

  "Progeny_sectionQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Progeny_sectionQuery"))
    }
  }

  "ProteinInformation" must {
    "return valid responses for section query" in {
      testQueryAgainstGqlEndpoint(Target("ProteinInformation_sectionQuery"))
    }
    // todo: figure out what to do with fragments.
    //    "return valid responses for summary query" in {
    //      testQueryAgainstGqlEndpoint(TargetDiseaseSize("ProteinInformation_summaryQuery"))
    //    }
  }

  "ProteinInteractions" must {
    "return valid responses for section query" in {
      testQueryAgainstGqlEndpoint(Target("ProteinInteractions_sectionQuery"))
    }
    // todo: figure out what to do with fragments.
    //    "return valid responses for summary query" in {
    //      testQueryAgainstGqlEndpoint(TargetDiseaseSize("ProteinInteractions_summaryQuery"))
    //    }
  }

  "Reactome_sectionQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Reactome_sectionQuery"))
    }
  }

  // todo Safety_summaryQuery <- fragment.

  "Search_SearchQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(Search("Search_SearchQuery"))
    }
  }

  "SearchPage_SearchPageQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(SearchPage("SearchPage_SearchPageQuery"))
    }
  }

  "SlapEnrich_sectionQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("SlapEnrich_sectionQuery"))
    }
  }

  "SysBio_sectionQuery" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("SysBio_sectionQuery"))
    }
  }
}
