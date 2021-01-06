package controllers

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import controllers.api.v4.graphql.GraphQLController
import inputs.{AdverseEventInputs, DrugInputs}
import org.scalatest.Inspectors.{forAll => sForAll}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Logging
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import test_configuration.IntegrationTestTag

class GraphQLControllerTest extends PlaySpec with GuiceOneAppPerTest with Injecting with DrugInputs with AdverseEventInputs
  with Logging with TableDrivenPropertyChecks {

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

      sForAll(responses) { r =>
        r must include("data")
        r mustNot include("errors")
      }
    }
    "return full objects" taggedAs IntegrationTestTag in {
      val tests = chemblIds.length
      var i = 1
      val responses: Seq[String] = chemblIds.map(fullQuery).map { q => {
        val request = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(q)
        logger.info(s"Test $i of $tests")
        i = i + 1
        contentAsString(controller.gqlBody.apply(request))
      }
      }
      val results = responses.zip(chemblIds).filter(s => s._1.contains("errors"))
      logger.info(s"${responses.size - results.size} entries had no errors.")
      logger.info(s"${results.size} entries contained errors.")
      results.foreach(e => logger.info(s"ID: ${e._2} \t Error: ${e._1}"))

      sForAll(responses) { r =>
        r must include("data")
        r mustNot include("errors")
      }
    }
  }

  "Hierarchical drug queries" must {
    val testCases = Table(
      ("parent", "child"),
      ("CHEMBL221959", "CHEMBL2103743"),
      ("CHEMBL22", "CHEMBL1201080"),
      ("CHEMBL1431", "CHEMBL1703"),
      ("CHEMBL941", "CHEMBL1642")
    )

    "pass their children's mechanisms of action to their parents" taggedAs IntegrationTestTag in {
      forAll(testCases) { (p: String, c: String) =>
        val request: FakeRequest[JsValue] = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(parentChildMoAQuery(p, c))
        val jsObject = Json.parse(contentAsString(controller.gqlBody.apply(request)))
        val drugs: Seq[ParentChildMoAReturn] = (jsObject \ "data" \ "drugs").as[JsArray].value.map(_.as[ParentChildMoAReturn])
        val (parent, child) = (drugs.head, drugs.tail.head)
        assert(parent.mechanismsOfAction.get.uniqueTargetTypes sameElements child.mechanismsOfAction.get.uniqueTargetTypes)
        assert(parent.mechanismsOfAction.get.uniqueActionTypes sameElements child.mechanismsOfAction.get.uniqueActionTypes)
        assert(parent.mechanismsOfAction.get.rows.length == child.mechanismsOfAction.get.rows.length)
      }

    }

    "consolidate linked targets from parent to child" taggedAs IntegrationTestTag in {

      forAll(testCases) { (p: String, c: String) =>
        val request: FakeRequest[JsValue] = FakeRequest(POST, "/graphql")
          .withHeaders(("Content-Type", "application/json"))
          .withBody(parentChildLinkedTargetQuery(p,c))
        val jsObject = Json.parse(contentAsString(controller.gqlBody.apply(request)))
        val drugs = (jsObject \ "data" \ "drugs").as[JsArray].value.map(_.as[ParentChildLinkedTargets])
        val (d1,d2) = (drugs.head, drugs.tail.head)
        sForAll(drugs)(d =>
          assert(d.linkedTargets.isDefined)
        )
        d1.linkedTargets.get.count should equal(d2.linkedTargets.get.count)
      }

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

      sForAll(responses) { r =>
        r must include("data")
        r mustNot include("errors")
        val jsonResponse = Json.parse(responses.head)
        assert((jsonResponse \ "data" \ "drug" \ "adverseEvents").isDefined)
      }

    }
  }

}
