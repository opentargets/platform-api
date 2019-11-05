package models.entities

import models.entities
import play.api.libs.json.Json

object Configuration {
  case class MetaVersion(x: Int, y: Int, z: Int)
  /** meta class compile the name and version information for
   * the application. Also, it serves as a container to include
   * future fields
   * */
  case class Meta(name: String, version: MetaVersion)

  case class ElasticsearchEntity(name: String, index: String, searchIndex: String)
  /** elasticsearch settings class set capture its configuration and the entities are
   * stored there */
  case class ElasticsearchSettings(host: String, port: Int, entities: Seq[ElasticsearchEntity])

  case class LUTableSettings(label: String, name: String, key: String, field: Option[String])
  case class AssociationSettings(label: String, name: String, key: String)
  case class TargetSettings(associations: AssociationSettings,
                            networks: Seq[LUTableSettings])
  case class DiseaseSettings(associations: AssociationSettings,
                             networks: Seq[LUTableSettings])

  case class DatasourceSettings(id: String, weight: Double)
  case class HarmonicSettings(pExponent: Int, datasources: Seq[DatasourceSettings])

  /** ClickHouse settings stores the configuration for the entities it handles.
   * Target Disease and Harmonic settings used to compute associations on the fly
   * and LUTs for network expansions
   * */
  case class ClickhouseSettings(target: TargetSettings, disease: DiseaseSettings, harmonic: HarmonicSettings)

  /** main Open Targets configuration object. It keeps track of meta, elasticsearch and clickhouse
   * configuration.
   * */
  case class OTSettings(meta: Meta, elasticsearch: ElasticsearchSettings, clickhouse: ClickhouseSettings)

  /** json implicits keeps all configuration units JSON-enabled using macros
   * from Json.format Json.reads and Json.writes
   * */
  object JSONImplicits {
    implicit val metaVersionImp = Json.format[MetaVersion]
    implicit val metaImp = Json.format[Meta]

    implicit val esEntities = Json.format[ElasticsearchEntity]
    implicit val esSettingsImp = Json.format[ElasticsearchSettings]

    implicit val luTableImp = Json.format[LUTableSettings]
    implicit val associationSettingsImp = Json.format[AssociationSettings]
    implicit val datasourceSettingsImp = Json.format[DatasourceSettings]
    implicit val harmonicSettingsImp = Json.format[HarmonicSettings]
    implicit val targetSettingsImp = Json.format[TargetSettings]
    implicit val diseaseSettingsImp = Json.format[DiseaseSettings]
    implicit val clickhouseSettingsImp = Json.format[ClickhouseSettings]

    implicit val otSettingsImp = Json.format[OTSettings]
  }
}
