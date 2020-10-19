package models.entities

import play.api.libs.json._


// Class to represent participantDetectionMethod
case class InteractionEvidencePDM( miIdentifier: Option[String], shortName:Option[String])

case class InteractionSpecies(mnemonic: Option[String], scientificName: Option[String],
                              taxonId: Option[Long])

case class InteractionResources(databaseVersion: String,
                                sourceDatabase: String)

case class InteractionEvidence(causalInteraction: Boolean,
                               evidenceScore: Option[Long],
                               expansionMethodMiIdentifier: Option[String],
                               expansionMethodShortName: Option[String],
                               hostOrganismScientificName: Option[String],
                               hostOrganismTaxId: Option[Long],
                               intASource: String,
                               intBSource: String,
                               interactionDetectionMethodMiIdentifier: String,
                               interactionDetectionMethodShortName: String,
                               interactionIdentifier: Option[String],
                               interactionResources: InteractionResources,
                               interactionScore: Option[Double],
                               interactionTypeMiIdentifier: Option[String],
                               interactionTypeShortName: Option[String],
                               //participantDetectionMethodMiIdentifierA: Option[String],
                               //participantDetectionMethodMiIdentifierB: Option[String],
                               //participantDetectionMethodShortNameA: Option[String],
                               //participantDetectionMethodShortNameB: Option[String],
                               participantDetectionMethodA: Option[IndexedSeq[InteractionEvidencePDM]],
                               speciesA: Option[InteractionSpecies],
                               speciesB: Option[InteractionSpecies],
                               pubmedId: Option[String]
                              )

case class Interaction(intA: String, targetA: String,
                       intB: String, targetB: String,
                       intABiologicalRole: String,
                       intBBiologicalRole: String,
                       scoring: Option[Double],
                       count: Long,
                       sourceDatabase: String)

case class Interactions(count: Long, rows: IndexedSeq[Interaction])


object Interactions {
  implicit val interactionEvidencePDMJSONImp = Json.format[InteractionEvidencePDM]
  implicit val interactionSpeciesJSONImp = Json.format[InteractionSpecies]
  implicit val interactionResourcesJSONImp = Json.format[InteractionResources]
  implicit val interactionEvidenceJSONImp = Json.format[InteractionEvidence]
  implicit val interactionJSONImp = Json.format[Interaction]
  implicit val interactionsJSONImp = Json.format[Interactions]
}


