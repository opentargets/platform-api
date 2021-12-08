package controllers

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import controllers.api.v4.graphql.GraphQLController
import controllers.api.v4.rest.CacheController
import models.gql.Fetchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import play.api.test.Helpers.{GET, POST}
import play.api.test.{FakeRequest, Injecting}
import play.api.{Application, Logging}
import test_configuration.IntegrationTestTag

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CacheControllerTest
    extends PlaySpec
      with GuiceOneAppPerSuite
      with Injecting
      with Logging
      with ScalaFutures {

  lazy val controller: CacheController = inject[CacheController]
  lazy val gqlController: GraphQLController = inject[GraphQLController]
  val apiKey = "superSecret"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(Map("ot.apiKeyHash" -> apiKey.hashCode.toString))
      .build()

  implicit lazy val timeout: Timeout = Timeout(1120, TimeUnit.SECONDS)

  "A cache" must {
    "be cleared" taggedAs IntegrationTestTag in {

      // given: a seeded cache
      val drugCache = Fetchers.drugsFetcherCache

      val query = Json.parse(
        """{ "query": "query { drugs(chemblIds: [\"CHEMBL221959\", \"CHEMBL2103743\"]) { id } }"}"""
      )
      val request = FakeRequest(POST, "/graphql")
        .withHeaders(("Content-Type", "application/json"))
        .withBody(query)
      Await.result(gqlController.gqlBody().apply(request), 10.second)
      // check there is something in the cache
      assert(drugCache.get("CHEMBL221959").isDefined)

      // when: the cache is cleared
      controller.clearCache()
      // then: the cache is empty
      drugCache.get("CHEMBL221959").isDefined mustBe true
    }
  }
  "A request to clear the cache" must {
    val request = FakeRequest(GET, "/cache/clear")
    "provide an `apiKey` header with correct code" in {
      // given
      val request = FakeRequest(GET, "/cache/clear").withHeaders(("apiKey", apiKey))
      // when
      val result: Result = Await.result(controller.clearCache().apply(request), 2.second)
      // then
      result.header.status mustEqual 200
    }
    "without an apiKey will result in a 403 error code" in {
      // when
      val result: Result = Await.result(controller.clearCache().apply(request), 2.second)
      // then
      result.header.status mustEqual Forbidden.header.status
    }
    "with the wrong apiKey will result in a 403 error code" in {
      // given
      val requestWithHeader = FakeRequest(GET, "/cache/clear").withHeaders(("apiKey", "luckyGuess"))
      // when
      val result: Result = Await.result(controller.clearCache().apply(requestWithHeader), 2.second)
      // then
      result.header.status mustEqual Forbidden.header.status

    }
  }
}
