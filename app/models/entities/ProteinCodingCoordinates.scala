package models.entities

import play.api.Logging
import play.api.libs.json._

case class ProteinCodingCoordinate(
    targetId: String,
    uniprotAccessions: Seq[String],
    aminoAcidPosition: Int,
    alternateAminoAcid: String,
    referenceAminoAcid: String,
    maxVariantEffectForPosition: MaxVariantEffectForPosition,
    variantId: String,
    diseaseIds: Seq[String],
    evidenceSources: Seq[ProteinCodingEvidenceSource]
)

case class MaxVariantEffectForPosition(
    method: String,
    value: Double
)

case class ProteinCodingEvidenceSource(
    datasourceId: String,
    evidenceCount: Int
)

object ProteinCodingCoordinate extends Logging {
  implicit val proteinCodingEvidenceSourceF: OFormat[ProteinCodingEvidenceSource] =
    Json.format[ProteinCodingEvidenceSource]
  implicit val maxVariantEffectForPositionF: OFormat[MaxVariantEffectForPosition] =
    Json.format[MaxVariantEffectForPosition]
  implicit val proteinCodingCoordinateF: OFormat[ProteinCodingCoordinate] =
    Json.format[ProteinCodingCoordinate]
}

case class ProteinCodingCoordinates(
    count: Long,
    rows: IndexedSeq[ProteinCodingCoordinate]
)

object ProteinCodingCoordinates extends Logging {
  def empty(): ProteinCodingCoordinates = ProteinCodingCoordinates(0, IndexedSeq.empty)
}
