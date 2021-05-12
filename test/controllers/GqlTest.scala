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

object GqlItTestInputs {
  val geneInputs = Seq(
    ("ENSG00000155966", "AFF2"),
    ("ENSG00000095110", "NXPE1"),
    ("ENSG00000198947", "DMD"),
    ("ENSG00000121281", "ADCY7"),
    ("ENSG00000187796", "CARD9"),
    ("ENSG00000129084", "PSMA1"),
    ("ENSG00000198901", "PRC1"),
    ("ENSG00000110435", "PDHX"),
    ("ENSG00000139618", "BRCA2"),
    ("ENSG00000102081", "FMR1"),
    ("ENSG00000115267", "IFIH1"),
    ("ENSG00000105855", "ITGB8"),
    ("ENSG00000079263", "SP140"),
    ("ENSG00000173531", "MST1"),
    ("ENSG00000206172", "HBA1"),
    ("ENSG00000204842", "ATXN2"),
    ("ENSG00000134460", "IL2RA"),
    ("ENSG00000162594", "IL23R"),
    ("ENSG00000013725", "CD6"),
    ("ENSG00000114013", "CD86"),
    ("ENSG00000188536", "HBA2"),
    ("ENSG00000244734", "HBB"),
    ("ENSG00000167207", "NOD2"),
    ("ENSG00000240972", "MIF"),
    ("ENSG00000115232", "ITGA4"),
    ("ENSG00000135679", "MDM2"),
    ("ENSG00000142192", "APP"),
    ("ENSG00000141510", "TP53"),
    ("ENSG00000197386", "HTT"),
    ("ENSG00000139687", "RB1"),
    ("ENSG00000090339", "ICAM1"),
    ("ENSG00000136634", "IL10"),
    ("ENSG00000166949", "SMAD3"),
    ("ENSG00000005844", "ITGAL"),
    ("ENSG00000164308", "ERAP2"),
    ("ENSG00000146648", "EGFR"),
    ("ENSG00000135446", "CDK4"),
    ("ENSG00000141736", "ERBB2"),
    ("ENSG00000157764", "BRAF"),
    ("ENSG00000104936", "DMPK"),
    ("ENSG00000105397", "TYK2"),
    ("ENSG00000171862", "PTEN"),
    ("ENSG00000175354", "PTPN2"),
    ("ENSG00000117450", "PRDX1"),
    ("ENSG00000169738", "DCXR"),
    ("ENSG00000141867", "BRD4"),
    ("ENSG00000106462", "EZH2"),
    ("ENSG00000176920", "FUT2"),
    ("ENSG00000143799", "PARP1"),
    ("ENSG00000166278", "C2"),
    ("ENSG00000197943", "PLCG2"),
    ("ENSG00000001626", "CFTR"),
    ("ENSG00000080815", "PSEN1"),
    ("ENSG00000143801", "PSEN2")
  )

  val diseaseInputs = Seq(
    "Orphanet_908",
    "Orphanet_166",
    "Orphanet_98756",
    "EFO_0000430",
    "Orphanet_255182",
    "Orphanet_93616",
    "Orphanet_846",
    "Orphanet_273",
    "Orphanet_262",
    "Orphanet_100973",
    "Orphanet_848",
    "Orphanet_564",
    "Orphanet_2843",
    "EFO_0003767",
    "Orphanet_399",
    "EFO_0001365",
    "Orphanet_93256",
    "EFO_0006906",
    "EFO_0003885",
    "Orphanet_586"
  )

  val drugInputs = Seq()

  // Generators
  val geneGenerator: Gen[String] = Gen.oneOf(geneInputs).map(_._1) // ensemblId
  val diseaseGenerator: Gen[String] = Gen.oneOf(diseaseInputs)
  val sizeGenerator: Gen[Int] = Gen.chooseNum(1, 10)
  val targetDiseaseSizeGenerator: Gen[(String, String, Int)] = for {
    gene <- geneGenerator
    disease <- diseaseGenerator
    size <- sizeGenerator
  } yield (gene, disease, size)
}

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
