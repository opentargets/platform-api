package models.entities

import models.{Backend, ElasticRetriever}
import models.Helpers.fromJsValue
import models.entities.Configuration.ElasticsearchSettings
import models.gql.Fetchers.targetsFetcher
import models.gql.Objects.targetImp
import models.Results
import play.api.Logging
import play.api.libs.json._
import sangria.schema._

import scala.concurrent.{ExecutionContext, Future}

case class Key(
                intA: String,
                intB: String,
                targetA: String,
                targetB: Option[String],
                intABiologicalRole: String,
                intBBiologicalRole: String,
                sourceDatabase: String
              )

case class InteractionEvidencePDM(miIdentifier: Option[String], shortName: Option[String])

case class InteractionSpecies(mnemonic: Option[String], scientific_name: Option[String], taxon_id: Option[Long])

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
                        targetB: String,
                        intABiologicalRole: String,
                        intBBiologicalRole: String,
                        scoring: Option[Double],
                        count: Long,
                        sourceDatabase: String,
                        speciesA: Option[InteractionSpecies],
                        speciesB: Option[InteractionSpecies],
    //TODO: Implement evidence gathering
                      )

object Interaction extends Logging {

  implicit val interactionKeyJSON: OFormat[Key] = Json.format[Key]
  
  implicit val interactionEvidencePDMF: OFormat[InteractionEvidencePDM] = Json.format[InteractionEvidencePDM]

  implicit val interactionSpeciesF: OFormat[InteractionSpecies] = Json.format[InteractionSpecies]// TODO: Corregir para leer json underscore y asignar a entidad

  implicit val interactionResourcesF: OFormat[InteractionResources] = Json.format[InteractionResources]

  implicit val interactionEvidenceF: OFormat[InteractionEvidence] = Json.format[InteractionEvidence]
  
  implicit val interactionF: OFormat[Interaction] = Json.format[Interaction]

  def findEvidences(interaction: Key)(implicit
      ec: ExecutionContext,
      esSettings: ElasticsearchSettings,
      esRetriever: ElasticRetriever
  ): Future[IndexedSeq[InteractionEvidence]] = {

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

    esRetriever.getByIndexedQueryMust(cbIndex, kv.toMap, pag, fromJsValue[InteractionEvidence]).map {
      case Results(Seq(), _, _, _) => IndexedSeq.empty
      case Results(seq, _, _, _)   => seq
    }
  }
}
