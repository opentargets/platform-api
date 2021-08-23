package models.gql

import models.entities.Configuration._
import models.entities.Pagination._
import models.entities._
import sangria.macros.derive._
import sangria.schema._
import sangria.marshalling.playJson._

object Arguments {

  import Aggregations._

  val paginationGQLImp = deriveInputObjectType[Pagination]()

  val datasourceSettingsInputImp = deriveInputObjectType[DatasourceSettings](
    InputObjectTypeName("DatasourceSettingsInput")
  )

  val aggregationFilterImp = deriveInputObjectType[AggregationFilter]()

  val entityNames = Argument("entityNames",
    OptionInputType(ListInputType(StringType)),
    description =
      "List of entity names to search for (target, disease, drug,...)")

  val datasourceIdsArg = Argument("datasourceIds",
    OptionInputType(ListInputType(StringType)),
    description = "List of datasource ids")

  val pageArg = Argument("page", OptionInputType(paginationGQLImp))
  val pageSize = Argument("size", OptionInputType(IntType))
  val cursor = Argument("cursor", OptionInputType(StringType))
  val databaseName =
    Argument("sourceDatabase", OptionInputType(StringType), description = "Database name")
  val queryString = Argument("queryString", StringType, description = "Query string")
  val optQueryString =
    Argument("queryString", OptionInputType(StringType), description = "Query string")
  val freeTextQuery =
    Argument("freeTextQuery", OptionInputType(StringType), description = "Query string")
  val efoId = Argument("efoId", StringType, description = "EFO ID")
  val efoIds = Argument("efoIds", ListInputType(StringType), description = "EFO ID")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID")
  val ensemblIds =
    Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID")
  val chemblIds = {
    Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")
  }
  val goIds = Argument("goIds", ListInputType(StringType), description = "List of GO IDs, eg. GO:0005515")
  val indirectEvidences = Argument(
    "enableIndirect",
    OptionInputType(BooleanType),
    "Use disease ontology to capture evidences from all descendants to build associations")

  val BFilterString = Argument("BFilter", OptionInputType(StringType))
  val scoreSorting = Argument("orderByScore", OptionInputType(StringType))

  val AId = Argument("A", StringType)
  val AIds = Argument("As", ListInputType(StringType))
  val BIds = Argument("Bs", OptionInputType(ListInputType(StringType)))

  val idsArg = Argument("additionalIds", OptionInputType(ListInputType(StringType)),
    description = "List of IDs either EFO ENSEMBL CHEMBL")
  val requiredIds = Argument("ids",ListInputType(StringType), description = "List of IDs either EFO ENSEMBL CHEMBL")
  val thresholdArg = Argument("threshold", OptionInputType(FloatType), description = "Threshold similarity between 0 and 1")

  val datasourceSettingsListArg =
    Argument("datasources", OptionInputType(ListInputType(datasourceSettingsInputImp)))

  val aggregationFiltersListArg =
    Argument("aggregationFilters", OptionInputType(ListInputType(aggregationFilterImp)))
}
