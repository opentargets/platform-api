package models.entities

import com.sksamuel.elastic4s.ElasticDsl.boolQuery
import com.sksamuel.elastic4s.requests.searches.aggs.TermsAggregation
import models.{Backend, ElasticRetriever}
import models.Helpers.fromJsValue
import models.entities.Configuration.ElasticsearchSettings
import models.gql.Fetchers.targetsFetcher
import models.gql.Objects.targetImp
import play.api.Logging
import play.api.libs.json._
import sangria.schema._

import scala.concurrent.{ExecutionContext, Future}

object Interaction extends Logging {

  case class Key(intA: String,
                 intB: String,
                 targetA: String,
                 targetB: Option[String],
                 intABiologicalRole: String,
                 intBBiologicalRole: String,
                 sourceDatabase: String)

  implicit val interactionKeyJSON = Json.format[Key]

  val interactionEvidencePDM = ObjectType(
    "InteractionEvidencePDM",
    fields[Backend, JsValue](
      Field("miIdentifier",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "miIdentifier").asOpt[String]),
      Field("shortName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "shortName").asOpt[String])
    )
  )

  val interactionSpecies = ObjectType(
    "InteractionSpecies",
    fields[Backend, JsValue](
      Field("mnemonic",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "mnemonic").asOpt[String]),
      Field("scientificName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "scientific_name").asOpt[String]),
      Field("taxonId",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "taxon_id").asOpt[Long])
    )
  )

  val interactionResources = ObjectType(
    "InteractionResources",
    fields[Backend, JsValue](
      Field("databaseVersion",
            StringType,
            description = None,
            resolve = js => (js.value \ "databaseVersion").as[String]),
      Field("sourceDatabase",
            StringType,
            description = None,
            resolve = js => (js.value \ "sourceDatabase").as[String])
    )
  )

  val interactionEvidence = ObjectType(
    "InteractionEvidence",
    fields[Backend, JsValue](
      Field("evidenceScore",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "evidenceScore").asOpt[Double]),
      Field("expansionMethodMiIdentifier",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "expansionMethodMiIdentifier").asOpt[String]),
      Field("expansionMethodShortName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "expansionMethodShortName").asOpt[String]),
      Field("hostOrganismScientificName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "hostOrganismScientificName").asOpt[String]),
      Field("hostOrganismTaxId",
            OptionType(LongType),
            description = None,
            resolve = js => (js.value \ "hostOrganismTaxId").asOpt[Long]),
      Field("intASource",
            StringType,
            description = None,
            resolve = js => (js.value \ "intASource").as[String]),
      Field("intBSource",
            StringType,
            description = None,
            resolve = js => (js.value \ "intBSource").as[String]),
      Field("interactionDetectionMethodMiIdentifier",
            StringType,
            description = None,
            resolve = js => (js.value \ "interactionDetectionMethodMiIdentifier").as[String]),
      Field("interactionDetectionMethodShortName",
            StringType,
            description = None,
            resolve = js => (js.value \ "interactionDetectionMethodShortName").as[String]),
      Field("interactionIdentifier",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "interactionIdentifier").asOpt[String]),
      Field("interactionTypeMiIdentifier",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "interactionTypeMiIdentifier").asOpt[String]),
      Field("interactionTypeShortName",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "interactionTypeShortName").asOpt[String]),
      Field(
        "participantDetectionMethodA",
        OptionType(ListType(interactionEvidencePDM)),
        description = None,
        resolve = js => (js.value \ "participantDetectionMethodA").asOpt[Seq[JsValue]]
      ),
      Field(
        "participantDetectionMethodB",
        OptionType(ListType(interactionEvidencePDM)),
        description = None,
        resolve = js => (js.value \ "participantDetectionMethodB").asOpt[Seq[JsValue]]
      ),
      Field("pubmedId",
            OptionType(StringType),
            description = None,
            resolve = js => (js.value \ "pubmedId").asOpt[String]),
    )
  )

  val interaction = ObjectType(
    "Interaction",
    fields[Backend, JsValue](
      Field("intA", StringType, description = None, resolve = js => (js.value \ "intA").as[String]),
      Field("targetA", OptionType(targetImp), description = None, resolve = js => {
        val tID = (js.value \ "targetA").as[String]
        targetsFetcher.deferOpt(tID)
      }),
      Field("intB", StringType, description = None, resolve = js => (js.value \ "intB").as[String]),
      Field("targetB", OptionType(targetImp), description = None, resolve = js => {
        val tID = (js.value \ "targetB").asOpt[String]
        targetsFetcher.deferOpt(tID)
      }),
      Field("intABiologicalRole",
            StringType,
            description = None,
            resolve = js => (js.value \ "intABiologicalRole").as[String]),
      Field("intBBiologicalRole",
            StringType,
            description = None,
            resolve = js => (js.value \ "intBBiologicalRole").as[String]),
      Field("score",
            OptionType(FloatType),
            description = None,
            resolve = js => (js.value \ "scoring").asOpt[Double]),
      Field("count", LongType, description = None, resolve = js => (js.value \ "count").as[Long]),
      Field("sourceDatabase",
            StringType,
            description = None,
            resolve = js => (js.value \ "sourceDatabase").as[String]),
      Field("speciesA",
            OptionType(interactionSpecies),
            description = None,
            resolve = js => (js.value \ "speciesA").asOpt[JsValue]),
      Field("speciesB",
            OptionType(interactionSpecies),
            description = None,
            resolve = js => (js.value \ "speciesB").asOpt[JsValue]),
      Field(
        "evidences",
        ListType(interactionEvidence),
        description = Some("List of evidences for this interaction"),
        resolve = r => {
          import scala.concurrent.ExecutionContext.Implicits.global
          import r.ctx._

          val ev = r.value.as[Key]
          Interaction.findEvidences(ev)
        }
      )
    )
  )

  def findEvidences(interaction: Key)(
      implicit ec: ExecutionContext,
      esSettings: ElasticsearchSettings,
      esRetriever: ElasticRetriever): Future[IndexedSeq[JsValue]] = {

    val pag = Pagination(0, 10000)

    val cbIndex = esSettings.entities
      .find(_.name == "interaction_evidence")
      .map(_.index)
      .getOrElse("interaction_evidence")

    val kv = List(
      "interactionResources.sourceDatabase.keyword" -> interaction.sourceDatabase,
      "targetA.keyword" -> interaction.targetA,
      "intA.keyword" -> interaction.intA,
      "intB.keyword" -> interaction.intB,
      "intABiologicalRole.keyword" -> interaction.intABiologicalRole,
      "intBBiologicalRole.keyword" -> interaction.intBBiologicalRole
    ) ++ interaction.targetB.map("targetB.keyword" -> _)

    esRetriever.getByIndexedQueryMust(cbIndex, kv.toMap, pag, fromJsValue[JsValue]).map {
      case (Seq(), _) => IndexedSeq.empty
      case (seq, _)   => seq
    }
  }
}
