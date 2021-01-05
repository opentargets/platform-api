package controllers

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import controllers.api.v4.graphql.GraphQLController
import inputs.{AdverseEventInputs, DrugInputs}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Logging
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import test_configuration.IntegrationTestTag

class GraphQLControllerTest extends PlaySpec with GuiceOneAppPerTest with Injecting with DrugInputs with AdverseEventInputs with Logging {

  implicit lazy val timeout: Timeout = Timeout(1120, TimeUnit.SECONDS)
  lazy val controller: GraphQLController = inject[GraphQLController]

  "Drug queries" must {
    "return an id " taggedAs IntegrationTestTag in {
      val responses: Seq[String] = chemblIds.map(idQuery).map { q => {
        val request = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(q)
        contentAsString(controller.gqlBody.apply(request))
      }
      }
      val results = responses.zip(chemblIds).filter(s => s._1.contains("errors"))
      logger.info(s"${responses.size - results.size} entries had no errors.")
      logger.info(s"${results.size} entries contained errors.")
      results.foreach(e => logger.info(s"ID: ${e._2} \t Error: ${e._1}"))

      forAll(responses) { r =>
        r must include("data")
        r mustNot include("errors")
      }
    }
    "return full objects" taggedAs IntegrationTestTag in {
      val responses: Seq[String] = chemblIds.map(fullQuery).map { q => {
        val request = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(q)
        contentAsString(controller.gqlBody.apply(request))
      }
      }
      val results = responses.zip(chemblIds).filter(s => s._1.contains("errors"))
      logger.info(s"${responses.size - results.size} entries had no errors.")
      logger.info(s"${results.size} entries contained errors.")
      results.foreach(e => logger.info(s"ID: ${e._2} \t Error: ${e._1}"))

      forAll(responses) { r =>
        r must include("data")
        r mustNot include("errors")
      }
    }
  }

  "Hierarchical drug queries" must {
    lazy val request = FakeRequest(POST, "/graphql")
      .withHeaders(("Content-Type", "application/json"))
      .withBody(parentChildMoAQuery)
    lazy val jsObject = Json.parse(contentAsString(controller.gqlBody.apply(request)))
    lazy val drugs: Seq[ParentChildMoAReturn] = (jsObject \ "data" \ "drugs").as[JsArray].value.map(_.as[ParentChildMoAReturn])
    lazy val (parent, child) = (drugs.head, drugs.tail.head)

    "pass their children's mechanisms of action to their parents" taggedAs IntegrationTestTag in {
      assert(parent.mechanismsOfAction.get.uniqueTargetTypes sameElements child.mechanismsOfAction.get.uniqueTargetTypes)
      assert(parent.mechanismsOfAction.get.uniqueActionTypes sameElements child.mechanismsOfAction.get.uniqueActionTypes)
      assert(parent.mechanismsOfAction.get.rows.length == child.mechanismsOfAction.get.rows.length)
    }
    "pass their children's indications to their parents" taggedAs IntegrationTestTag in {
      val childDiseases = child.indications.get.rows.map(_.disease.id)
      val parentDiseases = parent.indications.get.rows.map(_.disease.id)
      parentDiseases should contain allElementsOf childDiseases
    }
    "consolidate linked targets from parent to child" taggedAs IntegrationTestTag in {
      val r = request.withBody(parentChildLinkedTargetQuery)
      val jsObject = Json.parse(contentAsString(controller.gqlBody.apply(r)))
      val drugs = (jsObject \ "data" \ "drugs").as[JsArray].value.map(_.as[ParentChildLinkedTargets])
      val (d1,d2) = (drugs.head, drugs.tail.head)
      forAll(drugs)(d =>
        assert(d.linkedTargets.isDefined)
      )
      d1.linkedTargets.get.count should equal(d2.linkedTargets.get.count)
    }
  }

  "Adverse event queries" must {
    "return valid objects when an adverse event is associated with the id" taggedAs IntegrationTestTag in {
      val responses: Seq[String] = aeChemblIds.map(simpleAeQuery).map { q => {
        val request = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(q)
        contentAsString(controller.gqlBody.apply(request))
      }
      }
      val results = responses.zip(chemblIds).filter(s => s._1.contains("errors"))
      logger.info(s"${responses.size - results.size} entries had no errors.")
      logger.info(s"${results.size} entries contained errors.")
      results.foreach(e => logger.info(s"ID: ${e._2} \t Error: ${e._1}"))

      forAll(responses) { r =>
        r must include("data")
        r mustNot include("errors")
        val jsonResponse = Json.parse(responses.head)
        assert((jsonResponse \ "data" \ "drug" \ "adverseEvents").isDefined)
      }

    }
  }

}
