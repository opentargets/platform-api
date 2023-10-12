package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Configuration {
  val batchSize = 100

  case class Logging(otHeader: String, ignoredQueries: Seq[String])

  case class Cache(ignoreCache: Boolean)

  case class DataVersion(year: String, month: String, iteration: String)

  case class APIVersion(x: String, y: String, z: String)

  /** meta class compile the name and version information for
    * the application. Also, it serves as a container to include
    * future fields
    */
  case class Meta(name: String, apiVersion: APIVersion, dataVersion: DataVersion)

  case class ElasticsearchEntity(name: String, index: String, searchIndex: Option[String])

  /** elasticsearch settings class set capture its configuration and the entities are
    * stored there
    */
  case class ElasticsearchSettings(
      host: String,
      port: Int,
      entities: Seq[ElasticsearchEntity],
      highlightFields: Seq[String]
  )

  case class LUTableSettings(label: String, name: String, key: String, field: Option[String])

  case class DbTableSettings(label: String, name: String)

  case class TargetSettings(associations: DbTableSettings)

  case class DiseaseSettings(associations: DbTableSettings)

  case class DatasourceSettings(id: String, weight: Double, propagate: Boolean)

  case class HarmonicSettings(pExponent: Int, datasources: Seq[DatasourceSettings])

  /** ClickHouse settings stores the configuration for the entities it handles.
    * Target Disease and Harmonic settings used to compute associations on the fly
    * and LUTs for interaction expansions
    */
  case class ClickhouseSettings(
      target: TargetSettings,
      disease: DiseaseSettings,
      similarities: DbTableSettings,
      harmonic: HarmonicSettings,
      literature: DbTableSettings,
      literatureIndex: DbTableSettings,
      sentences: DbTableSettings
  )

  /** main Open Targets configuration object. It keeps track of meta, elasticsearch and clickhouse
    * configuration.
    */
  case class OTSettings(
      meta: Meta,
      elasticsearch: ElasticsearchSettings,
      clickhouse: ClickhouseSettings,
      ignoreCache: Boolean,
      logging: Logging
  )

  implicit val loggingJsonImp: OFormat[Logging] = Json.format[Logging]
  implicit val metaDataVersionJSONImp: OFormat[DataVersion] = Json.format[DataVersion]
  implicit val metaAPIVersionJSONImp: OFormat[APIVersion] = Json.format[APIVersion]
  implicit val metaJSONImp: OFormat[Meta] = Json.format[Meta]

  implicit val esEntitiesJSONImp: OFormat[ElasticsearchEntity] = Json.format[ElasticsearchEntity]
  implicit val esSettingsJSONImp: OFormat[ElasticsearchSettings] =
    Json.format[ElasticsearchSettings]

  implicit val luTableJSONImp: OFormat[LUTableSettings] = Json.format[LUTableSettings]
  implicit val dbTableSettingsImp: OFormat[DbTableSettings] =
    Json.format[DbTableSettings]
  implicit val datasourceSettingsJSONImp: OFormat[DatasourceSettings] =
    Json.format[DatasourceSettings]
  implicit val harmonicSettingsJSONImp: OFormat[HarmonicSettings] = Json.format[HarmonicSettings]
  implicit val targetSettingsJSONImp: OFormat[TargetSettings] = Json.format[TargetSettings]
  implicit val diseaseSettingsJSONImp: OFormat[DiseaseSettings] = Json.format[DiseaseSettings]
  implicit val clickhouseSettingsJSONImp: OFormat[ClickhouseSettings] =
    Json.format[ClickhouseSettings]

//  implicit val otSettingsJSONImp: OFormat[OTSettings] = Json.format[OTSettings]
  implicit val otSettingsJSONImp: Reads[OTSettings] = (
    (__ \ "meta").read[Meta] and
      (__ \ "elasticsearch").read[ElasticsearchSettings] and
      (__ \ "clickhouse").read[ClickhouseSettings] and
      (__ \ "ignoreCache").read[String] and
      (__ \ "logging").read[Logging]
  )(
    (meta, elasticsearchSettings, clickhouseSettings, ignoreCache, logging) =>
      OTSettings.apply(meta,
                       elasticsearchSettings,
                       clickhouseSettings,
                       ignoreCache.toBooleanOption.getOrElse(false),
                       logging))
}
