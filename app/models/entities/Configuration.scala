package models.entities

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Configuration {
  val batchSize = Pagination.sizeMax

  case class Logging(otHeader: String, ignoredQueries: Seq[String])

  case class DataVersion(year: String, month: String, iteration: Option[String])

  case class APIVersion(x: String, y: String, z: String, suffix: Option[String])

  /** meta class compile the name and version information for the application. Also, it serves as a
    * container to include future fields
    */
  case class Meta(name: String,
                  apiVersion: APIVersion,
                  dataVersion: DataVersion,
                  product: String,
                  enableDataReleasePrefix: Boolean,
                  dataPrefix: String
  )

  case class ElasticsearchEntity(name: String,
                                 index: String,
                                 searchIndex: Option[String],
                                 facetSearchIndex: Option[String]
  )

  /** elasticsearch settings class set capture its configuration and the entities are stored there
    */
  case class ElasticsearchSettings(
      host: String,
      port: Int,
      entities: Seq[ElasticsearchEntity],
      highlightFields: Seq[String]
  )

  case class LUTableSettings(label: String, name: String, key: String, field: Option[String])

  case class DbTableSettings(label: String, name: String)

  case class TargetSettings(label: String, name: String, associations: DbTableSettings)

  case class DiseaseSettings(associations: DbTableSettings)

  case class DatasourceSettings(id: String,
                                weight: Double,
                                propagate: Boolean,
                                required: Boolean = false
  )

  case class HarmonicSettings(pExponent: Int, datasources: Seq[DatasourceSettings])

  /** ClickHouse settings stores the configuration for the entities it handles. Target Disease and
    * Harmonic settings used to compute associations on the fly and LUTs for interaction expansions
    */
  case class ClickhouseSettings(
      defaultDatabaseName: String,
      intervals: DbTableSettings,
      target: TargetSettings,
      disease: DiseaseSettings,
      similarities: DbTableSettings,
      harmonic: HarmonicSettings,
      literature: DbTableSettings,
      literatureIndex: DbTableSettings
  )

  /** main Open Targets configuration object. It keeps track of meta, elasticsearch and clickhouse
    * configuration.
    */
  case class OTSettings(
      meta: Meta,
      elasticsearch: ElasticsearchSettings,
      clickhouse: ClickhouseSettings,
      ignoreCache: Boolean,
      qValidationLimitNTerms: Int,
      logging: Logging
  )

  implicit val loggingJsonImp: OFormat[Logging] = Json.format[Logging]

  implicit val apiVersionReads: Reads[APIVersion] = Reads[APIVersion] { json =>
    json.validate[String].flatMap { versionStr =>
      val regex = """^(\d+)\.(\d+)\.(\d+)(?:-(.+))?$""".r
      versionStr match {
        case regex(x, y, z, suffix) => JsSuccess(APIVersion(x, y, z, Option(suffix)))
        case regex(x, y, z, null)   => JsSuccess(APIVersion(x, y, z, None))
        case _ =>
          JsError(
            s"Invalid API version format: $versionStr. Value should conform to x.y.z[-suffix] where x,y,z are integers and suffix is an optional string"
          )
      }
    }
  }

  implicit val dataVersionReads: Reads[DataVersion] = Reads[DataVersion] { json =>
    json.validate[String].flatMap { versionStr =>
      val regex = """^(\d+)\.(\d+)(?:\.(\d+))?$""".r
      versionStr match {
        case regex(year, month, iteration) =>
          JsSuccess(DataVersion(year, month, Option(iteration)))
        case regex(year, month, null) =>
          JsSuccess(DataVersion(year, month, None))
        case _ =>
          JsError(
            s"Invalid data version format: $versionStr. Value should conform to year.month[.iteration] where year and month are integers and iteration is an optional integer"
          )
      }
    }
  }

  implicit val metaJSONImp: Reads[Meta] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "apiVersion").read[APIVersion] and
      (JsPath \ "dataRelease").read[DataVersion] and
      (JsPath \ "product").read[String] and
      (JsPath \ "enableDataReleasePrefix").read[Boolean] and
      (JsPath \ "product").read[String].and((JsPath \ "dataRelease").read[String]) {
        (product, dataRelease) =>
          s"$product${dataRelease.replace(".", "")}"
      }
  )(Meta.apply)

  implicit val esEntitiesJSONImp: OFormat[ElasticsearchEntity] = Json.format[ElasticsearchEntity]
  implicit val esSettingsJSONImp: Reads[ElasticsearchSettings] = ((__ \ "host").read[String] and
    (__ \ "port").read[String].map(_.toInt).orElse((__ \ "port").read[Int]) and
    (__ \ "entities").read[Seq[ElasticsearchEntity]] and
    (__ \ "highlightFields").read[Seq[String]])(ElasticsearchSettings.apply)

  implicit val esSettingsJSONWrites: OWrites[ElasticsearchSettings] =
    Json.writes[ElasticsearchSettings]

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

  implicit val otSettingsJSONImp: Reads[OTSettings] = ((__ \ "meta").read[Meta] and
    (__ \ "elasticsearch").read[ElasticsearchSettings] and
    (__ \ "clickhouse").read[ClickhouseSettings] and
    (__ \ "ignoreCache").read[String] and
    (__ \ "qValidationLimitNTerms").read[String] and
    (__ \ "logging").read[Logging])(
    (meta,
     elasticsearchSettings,
     clickhouseSettings,
     ignoreCache,
     qValidationLimitNTerms,
     logging
    ) =>
      OTSettings.apply(meta,
                       elasticsearchSettings,
                       clickhouseSettings,
                       ignoreCache.toBooleanOption.getOrElse(false),
                       qValidationLimitNTerms.toInt,
                       logging
      )
  )
}
