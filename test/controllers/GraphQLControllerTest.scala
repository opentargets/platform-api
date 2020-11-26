package controllers

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import controllers.api.v4.graphql.GraphQLController
import inputs.DrugInputs
import org.scalatest.Inspectors.forAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Logging
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import test_configuration.IntegrationTestTag

class GraphQLControllerTest extends PlaySpec with GuiceOneAppPerTest with Injecting with DrugInputs with Logging {

  implicit lazy val timeout: Timeout = Timeout(20, TimeUnit.SECONDS)
  lazy val controller: GraphQLController = inject[GraphQLController]

  "Drug queries" must {
    "return an id " taggedAs IntegrationTestTag in {
      val responses: Seq[String] = chemblIds.map(idQuery).map{ q => {
        val request = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(q)
        contentAsString(controller.gqlBody.apply(request))
      }}
      val results = responses.zip(chemblIds).filter(s => s._1.contains("errors"))
      logger.info(s"${responses.size - results.size} entries had no errors.")
      logger.info(s"${results.size} entries contained errors.")
      results.foreach(e => logger.info(s"ID: ${e._2} \t Error: ${e._1}"))

      forAll (responses){ r =>
        r must include("data")
        r mustNot include("errors")
      }
    }
    "return full objects" taggedAs IntegrationTestTag in {
      val responses: Seq[String] = chemblIds.map(fullQuery).map{ q => {
        val request = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(q)
        contentAsString(controller.gqlBody.apply(request))
      }}
      val results = responses.zip(chemblIds).filter(s => s._1.contains("errors"))
      logger.info(s"${responses.size - results.size} entries had no errors.")
      logger.info(s"${results.size} entries contained errors.")
      results.foreach(e => logger.info(s"ID: ${e._2} \t Error: ${e._1}"))

      forAll (responses){ r =>
        r must include("data")
        r mustNot include("errors")
      }
    }

  }

}
