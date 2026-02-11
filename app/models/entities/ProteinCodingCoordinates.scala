package models.entities

import play.api.Logging
import play.api.libs.json._
import slick.jdbc.GetResult
import models.gql.TypeWithId

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

object ProteinCodingCoordinate extends Logging {
  implicit val DatasourceF: OFormat[Datasource] =
    Json.format[Datasource]
  implicit val proteinCodingCoordinateF: OFormat[ProteinCodingCoordinate] =
    Json.format[ProteinCodingCoordinate]
}

case class ProteinCodingCoordinates(
    count: Long,
    rows: IndexedSeq[ProteinCodingCoordinate],
    id: String = ""
) extends TypeWithId

object ProteinCodingCoordinates extends Logging {
  def empty(): ProteinCodingCoordinates = ProteinCodingCoordinates(0, IndexedSeq.empty)
  implicit val proteinCodingCoordinatesF: OFormat[ProteinCodingCoordinates] =
    Json.format[ProteinCodingCoordinates]
  implicit val getFromDB: GetResult[ProteinCodingCoordinates] =
    GetResult(r => Json.parse(r.<<[String]).as[ProteinCodingCoordinates])
}
