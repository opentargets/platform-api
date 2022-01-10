package models.entities

import play.api.libs.json.Json
import play.api.libs.json.OFormat

object Configuration {
  val batchSize = 100

  case class Logging(otHeader: String, ignoredQueries: Seq[String])

  case class DataVersion(year: Int, month: Int, iteration: Int)

  case class APIVersion(x: Int, y: Int, z: Int)

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

  case class AssociationSettings(label: String, name: String)

  case class LiteratureIndexSettings(label: String, name: String)
  case class LiteratureSettings(label: String, name: String)

  case class TargetSettings(associations: AssociationSettings)

  case class DiseaseSettings(associations: AssociationSettings)

  case class DatasourceSettings(id: String, weight: Double, propagate: Boolean)

  case class HarmonicSettings(pExponent: Int, datasources: Seq[DatasourceSettings])

  /** ClickHouse settings stores the configuration for the entities it handles.
   * Target Disease and Harmonic settings used to compute associations on the fly
   * and LUTs for interaction expansions
   */
  case class ClickhouseSettings(
                                 target: TargetSettings,
                                 disease: DiseaseSettings,
                                 similarities: AssociationSettings,
                                 harmonic: HarmonicSettings,
                                 literature: LiteratureSettings,
                                 literatureIndex: LiteratureIndexSettings
                               )

  /** main Open Targets configuration object. It keeps track of meta, elasticsearch and clickhouse
   * configuration.
   */
  case class OTSettings(
                         meta: Meta,
                         elasticsearch: ElasticsearchSettings,
                         clickhouse: ClickhouseSettings,
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
  implicit val literatureSettingsJSONImp: OFormat[LiteratureSettings] =
    Json.format[LiteratureSettings]
  implicit val literatureIndexSettingsJSONImp: OFormat[LiteratureIndexSettings] =
    Json.format[LiteratureIndexSettings]
  implicit val associationSettingsJSONImp: OFormat[AssociationSettings] =
    Json.format[AssociationSettings]
  implicit val datasourceSettingsJSONImp: OFormat[DatasourceSettings] =
    Json.format[DatasourceSettings]
  implicit val harmonicSettingsJSONImp: OFormat[HarmonicSettings] = Json.format[HarmonicSettings]
  implicit val targetSettingsJSONImp: OFormat[TargetSettings] = Json.format[TargetSettings]
  implicit val diseaseSettingsJSONImp: OFormat[DiseaseSettings] = Json.format[DiseaseSettings]
  implicit val clickhouseSettingsJSONImp: OFormat[ClickhouseSettings] =
    Json.format[ClickhouseSettings]

  implicit val otSettingsJSONImp: OFormat[OTSettings] = Json.format[OTSettings]
}
