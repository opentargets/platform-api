package models.gql

import models.entities.Configuration._
import models.entities.Pagination._
import models.entities.{ComparatorEnum, _}
import sangria.macros.derive._
import sangria.schema._
import sangria.marshalling.playJson._
import sangria.marshalling.FromInput
import sangria.util.tag

object Arguments {

  import Aggregations._

  val paginationGQLImp: InputObjectType[Pagination] = deriveInputObjectType[Pagination]()

  val datasourceSettingsInputImp: InputObjectType[DatasourceSettings] =
    deriveInputObjectType[DatasourceSettings](
      InputObjectTypeName("DatasourceSettingsInput")
    )

  val aggregationFilterImp: InputObjectType[AggregationFilter] =
    deriveInputObjectType[AggregationFilter]()

  val entityNames: Argument[Option[Seq[String]]] = Argument(
    "entityNames",
    OptionInputType(ListInputType(StringType)),
    description = "List of entity names to search for (target, disease, drug,...)"
  )

  val datasourceIdsArg: Argument[Option[Seq[String]]] = Argument(
    "datasourceIds",
    OptionInputType(ListInputType(StringType)),
    description = "List of datasource ids"
  )

  val pageArg: Argument[Option[Pagination]] = Argument("page", OptionInputType(paginationGQLImp))
  val pageSize: Argument[Option[Int]] = Argument("size", OptionInputType(IntType))
  val cursor: Argument[Option[String]] = Argument("cursor", OptionInputType(StringType))
  val databaseName: Argument[Option[String]] =
    Argument("sourceDatabase", OptionInputType(StringType), description = "Database name")
  val queryString: Argument[String] =
    Argument("queryString", StringType, description = "Query string")
  val optQueryString: Argument[Option[String]] =
    Argument("queryString", OptionInputType(StringType), description = "Query string")
  val freeTextQuery: Argument[Option[String]] =
    Argument("freeTextQuery", OptionInputType(StringType), description = "Query string")
  val efoId: Argument[String] = Argument("efoId", StringType, description = "EFO ID")
  val efoIds: Argument[Seq[String with tag.Tagged[FromInput.CoercedScalaResult]]] =
    Argument("efoIds", ListInputType(StringType), description = "EFO ID")
  val ensemblId: Argument[String] = Argument("ensemblId", StringType, description = "Ensembl ID")
  val ensemblIds: Argument[Seq[String with tag.Tagged[FromInput.CoercedScalaResult]]] =
    Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId: Argument[String] = Argument("chemblId", StringType, description = "Chembl ID")
  val chemblIds: Argument[Seq[String with tag.Tagged[FromInput.CoercedScalaResult]]] =
    Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")
  val goIds: Argument[Seq[String with tag.Tagged[FromInput.CoercedScalaResult]]] =
    Argument("goIds", ListInputType(StringType), description = "List of GO IDs, eg. GO:0005515")
  val indirectEvidences: Argument[Option[Boolean]] = Argument(
    "enableIndirect",
    OptionInputType(BooleanType),
    "Use disease ontology to capture evidences from all descendants to build associations"
  )

  val ComparerEnum = deriveEnumType[ComparatorEnum.Value](
    IncludeValues("GreaterThan", "LesserThan")
  )

  val year: Argument[Option[Int]] =
    Argument("year", OptionInputType(IntType), description = "Year")
  val month: Argument[Option[Int]] =
    Argument("month", OptionInputType(IntType), description = "Month")
  val dateComparator: Argument[Option[ComparatorEnum.Value]] = Argument(
    "comparator",
    OptionInputType(ComparerEnum),
    description = "Defines if should results be either before or after specific date"
  )

  val BFilterString: Argument[Option[String]] = Argument("BFilter", OptionInputType(StringType))
  val scoreSorting: Argument[Option[String]] = Argument("orderByScore", OptionInputType(StringType))

  val AId: Argument[String] = Argument("A", StringType)
  val AIds: Argument[Seq[String with tag.Tagged[FromInput.CoercedScalaResult]]] =
    Argument("As", ListInputType(StringType))
  val BIds: Argument[Option[Seq[String]]] =
    Argument("Bs", OptionInputType(ListInputType(StringType)))

  val idsArg: Argument[Option[Seq[String]]] = Argument(
    "additionalIds",
    OptionInputType(ListInputType(StringType)),
    description = "List of IDs either EFO ENSEMBL CHEMBL"
  )
  val requiredIds: Argument[Seq[String with tag.Tagged[FromInput.CoercedScalaResult]]] = Argument(
    "ids",
    ListInputType(StringType),
    description = "List of IDs either EFO ENSEMBL CHEMBL"
  )
  val thresholdArg: Argument[Option[Double]] = Argument(
    "threshold",
    OptionInputType(FloatType),
    description = "Threshold similarity between 0 and 1"
  )

  val datasourceSettingsListArg: Argument[Option[Seq[DatasourceSettings]]] =
    Argument("datasources", OptionInputType(ListInputType(datasourceSettingsInputImp)))

  val aggregationFiltersListArg: Argument[Option[Seq[AggregationFilter]]] =
    Argument("aggregationFilters", OptionInputType(ListInputType(aggregationFilterImp)))
}
