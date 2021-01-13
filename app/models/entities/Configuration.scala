package models.entities

import play.api.libs.json.Json

object Configuration {
  val batchSize = 100

  case class DataVersion(year: Int, month: Int, iteration: Int)

  case class APIVersion(x: Int, y: Int, z: Int)

  /** meta class compile the name and version information for
    * the application. Also, it serves as a container to include
    * future fields
    * */
  case class Meta(name: String, apiVersion: APIVersion, dataVersion: DataVersion)

  case class ElasticsearchEntity(name: String, index: String, searchIndex: Option[String])

  /** elasticsearch settings class set capture its configuration and the entities are
    * stored there */
  case class ElasticsearchSettings(host: String,
                                   port: Int,
                                   entities: Seq[ElasticsearchEntity],
                                   highlightFields: Seq[String])

  case class LUTableSettings(label: String, name: String, key: String, field: Option[String])

  case class AssociationSettings(label: String, name: String)

  case class TargetSettings(associations: AssociationSettings)

  case class DiseaseSettings(associations: AssociationSettings)

  case class DatasourceSettings(id: String, weight: Double, propagate: Boolean)

  case class HarmonicSettings(pExponent: Int, datasources: Seq[DatasourceSettings])

  /** ClickHouse settings stores the configuration for the entities it handles.
    * Target Disease and Harmonic settings used to compute associations on the fly
    * and LUTs for interaction expansions
    * */
  case class ClickhouseSettings(target: TargetSettings,
                                disease: DiseaseSettings,
                                harmonic: HarmonicSettings)

  /** main Open Targets configuration object. It keeps track of meta, elasticsearch and clickhouse
    * configuration.
    * */
  case class OTSettings(meta: Meta,
                        elasticsearch: ElasticsearchSettings,
                        clickhouse: ClickhouseSettings)

  implicit val metaDataVersionJSONImp = Json.format[DataVersion]
  implicit val metaAPIVersionJSONImp = Json.format[APIVersion]
  implicit val metaJSONImp = Json.format[Meta]

  implicit val esEntitiesJSONImp = Json.format[ElasticsearchEntity]
  implicit val esSettingsJSONImp = Json.format[ElasticsearchSettings]

  implicit val luTableJSONImp = Json.format[LUTableSettings]
  implicit val associationSettingsJSONImp = Json.format[AssociationSettings]
  implicit val datasourceSettingsJSONImp = Json.format[DatasourceSettings]
  implicit val harmonicSettingsJSONImp = Json.format[HarmonicSettings]
  implicit val targetSettingsJSONImp = Json.format[TargetSettings]
  implicit val diseaseSettingsJSONImp = Json.format[DiseaseSettings]
  implicit val clickhouseSettingsJSONImp = Json.format[ClickhouseSettings]

  implicit val otSettingsJSONImp = Json.format[OTSettings]
}
