package models.entities

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.*

case class ProteinCodingCoordinate(
    targetId: String,
    uniprotAccessions: Seq[String],
    aminoAcidPosition: Int,
    alternateAminoAcid: String,
    referenceAminoAcid: String,
    variantFunctionalConsequenceIds: Option[Seq[String]],
    variantEffect: Option[Double],
    variantId: String,
    diseases: Seq[String],
    datasources: Seq[Datasource],
    therapeuticAreas: Seq[String]
)

case class Datasource(
    datasourceCount: Int,
    datasourceId: String,
    datasourceNiceName: String
)

object ProteinCodingCoordinate {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit val DatasourceF: OFormat[Datasource] =
    Json.format[Datasource]
  implicit val proteinCodingCoordinateF: OFormat[ProteinCodingCoordinate] =
    Json.format[ProteinCodingCoordinate]
}

case class ProteinCodingCoordinates(
    count: Long,
    rows: IndexedSeq[ProteinCodingCoordinate]
)

object ProteinCodingCoordinates {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def empty(): ProteinCodingCoordinates = ProteinCodingCoordinates(0, IndexedSeq.empty)
}
