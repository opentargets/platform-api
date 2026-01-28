package models.entities

import models.ElasticRetriever
import models.Helpers.fromJsValue
import models.entities.Configuration.{ElasticsearchSettings, OTSettings}
import models.Results
import utils.MetadataUtils.getIndexWithPrefixOrDefault
import play.api.Logging
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class InteractionEvidencePDM(miIdentifier: Option[String], shortName: Option[String])

case class InteractionSpecies(mnemonic: String,
                              scientificName: Option[String],
                              taxonId: Option[Long]
)

case class InteractionResources(databaseVersion: String, sourceDatabase: String)

case class InteractionEvidence(
    evidenceScore: Option[Double],
    expansionMethodMiIdentifier: Option[String],
    expansionMethodShortName: Option[String],
    hostOrganismScientificName: Option[String],
    hostOrganismTaxId: Option[Long],
    intASource: String,
    intBSource: String,
    interactionDetectionMethodMiIdentifier: String,
    interactionDetectionMethodShortName: String,
    interactionIdentifier: Option[String],
    interactionTypeMiIdentifier: Option[String],
    interactionTypeShortName: Option[String],
    participantDetectionMethodA: Option[Seq[InteractionEvidencePDM]],
    participantDetectionMethodB: Option[Seq[InteractionEvidencePDM]],
    pubmedId: Option[String]
)

case class Interaction(
    intA: String,
    targetA: String,
    intB: String,
    targetB: Option[String],
    intABiologicalRole: String,
    intBBiologicalRole: String,
    scoring: Option[Double],
    count: Long,
    sourceDatabase: String,
    speciesA: Option[InteractionSpecies],
    speciesB: Option[InteractionSpecies],
    metaTotal: Int = 0
    // TODO: Implement evidence gathering
)

object Interaction extends Logging {
  implicit val getFromDB: GetResult[Interaction] =
    GetResult(r => Json.parse(r.<<[String]).as[Interaction])

  implicit val interactionEvidencePDMF: OFormat[InteractionEvidencePDM] =
    Json.format[InteractionEvidencePDM]

  implicit val interactionSpeciesW: OWrites[InteractionSpecies] = Json.writes[InteractionSpecies]
  implicit val interactionSpeciesR: Reads[InteractionSpecies] =
    ((__ \ "mnemonic").read[String] and
      (__ \ "scientific_name").readNullable[String] and
      (__ \ "taxon_id").readNullable[Long])(InteractionSpecies.apply)

  implicit val interactionResourcesF: OFormat[InteractionResources] =
    Json.format[InteractionResources]

  implicit val interactionEvidenceF: OFormat[InteractionEvidence] = Json.format[InteractionEvidence]

  implicit val interactionF: OFormat[Interaction] = Json.format[Interaction]

  def findEvidences(interaction: Interaction)(implicit
      ec: ExecutionContext,
      esSettings: ElasticsearchSettings,
      esRetriever: ElasticRetriever,
      otSettings: OTSettings
  ): Future[IndexedSeq[InteractionEvidence]] = {

    val pag = Pagination(0, 10000)

    val indexName = esSettings.entities
      .find(_.name == "interaction_evidence")
      .map(_.index)
      .getOrElse("interaction_evidence")

    val cbIndex = getIndexWithPrefixOrDefault(indexName)

    val kv = List(
      "interactionResources.sourceDatabase.keyword" -> interaction.sourceDatabase,
      "targetA.keyword" -> interaction.targetA,
      "intA.keyword" -> interaction.intA,
      "intB.keyword" -> interaction.intB,
      "intABiologicalRole.keyword" -> interaction.intABiologicalRole,
      "intBBiologicalRole.keyword" -> interaction.intBBiologicalRole
    ) ++ interaction.targetB.map("targetB.keyword" -> _)

    esRetriever
      .getByIndexedQueryMust(cbIndex, kv.toMap, pag, fromJsValue[InteractionEvidence])
      .map {
        case Results(Seq(), _, _, _) => IndexedSeq.empty
        case Results(seq, _, _, _)   => seq
      }
  }
}
