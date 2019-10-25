package models.entities

import models.entities
import play.api.libs.json.Json

object Configuration {
  case class MetaVersion(x: Int, y: Int, z: Int)
  case class Meta(name: String, version: MetaVersion)

  case class ElasticsearchIndices(target: String, disease: String, drug: String, search: Seq[String])
  case class ElasticsearchEntity(name: String, index: String, searchIndex: String)
  case class ElasticsearchSettings(host: String, port: Int, entities: Seq[ElasticsearchEntity])

  case class LUTableSettings(name: String, key: String, field: Option[String])
  case class NetworkSettings(lut: LUTableSettings, networks: Seq[LUTableSettings])
  case class TargetSettings(lut: LUTableSettings, networks: Seq[LUTableSettings])
  case class DiseaseSettings(lut: LUTableSettings, networks: Seq[LUTableSettings])

  case class DatasourceSettings(id: String, weight: Double)
  case class HarmonicSettings(pExponent: Int, datasources: Seq[DatasourceSettings])

  case class ClickhouseSettings(target: TargetSettings, disease: DiseaseSettings, harmonic: HarmonicSettings)

  case class OTSettings(meta: Meta, elasticsearch: ElasticsearchSettings, clickhouse: ClickhouseSettings)
  object JSONImplicits {
    implicit val metaVersionImp = Json.format[MetaVersion]
    implicit val metaImp = Json.format[Meta]

    implicit val esEntities = Json.reads[ElasticsearchEntity]
    implicit val esIndices = Json.reads[ElasticsearchIndices]
    implicit val esSettingsImp = Json.reads[ElasticsearchSettings]

    implicit val luTableImp = Json.reads[LUTableSettings]
    implicit val datasourceSettingsImp = Json.reads[DatasourceSettings]
    implicit val harmonicSettingsImp = Json.reads[HarmonicSettings]
    implicit val networkSettingsImp = Json.reads[NetworkSettings]
    implicit val targetSettingsImp = Json.reads[TargetSettings]
    implicit val diseaseSettingsImp = Json.reads[DiseaseSettings]
    implicit val clickhouseSettingsImp = Json.reads[ClickhouseSettings]

    implicit val otSettingsImp = Json.reads[OTSettings]
  }
}
