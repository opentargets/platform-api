package controllers

import akka.util.Timeout
import controllers.GqlTest.{createRequest, generateQueryString, getQueryFromFile}
import controllers.api.v4.graphql.GraphQLController
import inputs.{GqlCase, GqlItTestInputs, Search, SearchPage, Target, TargetDisease, TargetDiseaseSize, TargetDiseaseSizeCursor}
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
import test_configuration.IntegrationTestTag

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.reflect.io.File






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
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("CancerGeneCensus_sectionQuery"))
    }
  }

  "EuropePmc queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSizeCursor("EuropePmc_sectionQuery"))
    }
  }

  "Gene2Phenotype_sectionQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDisease("Gene2Phenotype_sectionQuery"))
    }
  }

  "GenomicsEngland_sectionQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDisease("GenomicsEngland_sectionQuery"))
    }
  }

  "IntOgen_sectionQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
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
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("OTGenetics_sectionQuery"))
    }
  }

  "Phenodigm_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Phenodigm_sectionQuery"))
    }
  }

  "Progeny_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Progeny_sectionQuery"))
    }
  }

  "ProteinInformation" must {
    "return valid responses for section query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("ProteinInformation_sectionQuery"))
    }
    // todo: figure out what to do with fragments.
    //    "return valid responses for summary query" in {
    //      testQueryAgainstGqlEndpoint(TargetDiseaseSize("ProteinInformation_summaryQuery"))
    //    }
  }

  "ProteinInteractions" must {
    "return valid responses for section query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("ProteinInteractions_sectionQuery"))
    }
    // todo: figure out what to do with fragments.
    //    "return valid responses for summary query" in {
    //      testQueryAgainstGqlEndpoint(TargetDiseaseSize("ProteinInteractions_summaryQuery"))
    //    }
  }

  "Reactome_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Reactome_sectionQuery"))
    }
  }

  // todo Safety_summaryQuery <- fragment.

  "Search_SearchQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Search("Search_SearchQuery"))
    }
  }

  "SearchPage_SearchPageQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(SearchPage("SearchPage_SearchPageQuery"))
    }
  }

  "SlapEnrich_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("SlapEnrich_sectionQuery"))
    }
  }

  "SysBio_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("SysBio_sectionQuery"))
    }
  }
}
