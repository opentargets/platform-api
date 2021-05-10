package controllers

import akka.util.Timeout
import controllers.GqlTest.{createRequest, generateQueryString, getQueryFromFile}
import controllers.api.v4.graphql.GraphQLController
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Logging
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import test_configuration.IntegrationTestTag
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.reflect.io.File

sealed trait GqlCase {
  val file: String
  def generateVariables(): String
}

case class TargetDiseaseSize(file: String) extends GqlCase {
  def generateVariables(): String = {
    // todo update this to use a generator to create tuple.
    val vars = ("EFO_0000712", "ENSG00000073756", 10)
    val varJson =
      """
    "variables": {
      "efoId": "EFO_0000712",
      "ensemblId": "ENSG00000073756",
      "size": 10
    }
  """
   varJson
  }
}

case class TargetDiseaseSizeCursor(file: String) extends GqlCase {
  def generateVariables(): String = {
    val varJson =
      """
    "variables": {
      "efoId": "EFO_0000712",
      "ensemblId": "ENSG00000073756",
      "size": 10,
      "cursor": null
    }
  """
    varJson
  }
}

object GqlTest {

  /**
   * Retrieves filename from target resources and returns query as string with quotes escaped.
   */
  def getQueryFromFile(filename: String): String = {
    File(this.getClass.getResource(s"/gqlQueries/$filename.gql").getPath).lines
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

/*
This should be named something more descriptive and moved to an IT test package.
 */
class GqlTest
  extends PlaySpec
    with GuiceOneAppPerTest
    with Injecting
    with Logging
    with ScalaFutures {

  lazy val controller: GraphQLController = inject[GraphQLController]
  implicit lazy val timeout: Timeout = Timeout(1120, TimeUnit.SECONDS)

  def testQueryAgainstGqlEndpoint(gqlCase: GqlCase): Unit = {
    // get query from front end
    val query = getQueryFromFile(gqlCase.file)
    // create test variables
    val variables = gqlCase.generateVariables()
    // create and execute request
    val request = createRequest(generateQueryString(query, variables))
    val resultF: Future[Result] = controller.gqlBody().apply(request)
    val result = Await.result(resultF, 10.seconds)
    val resultS = contentAsString(resultF)

    // validate results
    result.header.status mustBe 200
    resultS must include("data")
    resultS mustNot include("errors")
  }

  "OT genetics queries" must {
    "return valid responses" in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("OTGenetics_sectionQuery"))
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

}
