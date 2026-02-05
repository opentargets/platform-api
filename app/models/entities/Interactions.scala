package models.entities

import models.Backend
import play.api.Logging
import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import models.gql.TypeWithId
import slick.jdbc.GetResult

case class Interactions(count: Long, rows: IndexedSeq[Interaction], id: String = "")
    extends TypeWithId

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
    evidences: Vector[InteractionEvidence]
)

object Interactions extends Logging {
  val empty: Interactions = Interactions(0L, IndexedSeq.empty)
  implicit val getInteractionsFromDB: GetResult[Interactions] =
    GetResult(r => Json.parse(r.<<[String]).as[Interactions])
  implicit val getInteractionResourcesFromDB: GetResult[InteractionResources] =
    GetResult(r => Json.parse(r.<<[String]).as[InteractionResources])
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
  implicit val interactionsF: OFormat[Interactions] = Json.format[Interactions]
}
