package models.gql

import models.entities.Configuration._
import models.entities.Pagination._
import models.entities._
import play.api.libs.json.Json
import sangria.macros.derive._
import sangria.schema._

object GQLArguments {
  import GQLImplicits._

  val entityNames = Argument("entityNames", OptionInputType(ListInputType(StringType)),
    description = "List of entity names to search for (target, disease, drug,...)")
  val pageArg = Argument("page", OptionInputType(paginationImp))
  val pageSize = Argument("size", OptionInputType(IntType))
  val cursor = Argument("cursor", OptionInputType(ListInputType(StringType)))
  val queryString = Argument("queryString", StringType, description = "Query string")
  val freeTextQuery = Argument("freeTextQuery", OptionInputType(StringType), description = "Query string")
  val efoId = Argument("efoId", StringType, description = "EFO ID")
  val efoIds = Argument("efoIds", ListInputType(StringType), description = "EFO ID")
  val networkExpansionId = Argument("networkExpansionId", OptionInputType(StringType), description = "Network expansion ID")
  val ensemblId = Argument("ensemblId", StringType, description = "Ensembl ID")
  val ensemblIds = Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val chemblId = Argument("chemblId", StringType, description = "Chembl ID")
  val chemblIds = Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")
  val indrectEvidences = Argument("enableIndirect", OptionInputType(BooleanType),
    "Use disease ontology to capture evidences from all descendants to build associations")

  val BFilterString = Argument("BFilter", OptionInputType(StringType))
  val scoreSorting = Argument("orderByScore", OptionInputType(StringType))
  val AId = Argument("A", StringType)
  val AIds = Argument("As", ListInputType(StringType))
  val BIds = Argument("Bs", OptionInputType(ListInputType(StringType)))

  val datasourceSettingsListArg = Argument("datasources",
    OptionInputType(ListInputType(datasourceSettingsInputImp)))

  val aggregationFiltersListArg = Argument("aggregationFilters",
    OptionInputType(ListInputType(aggregationFilterImp)))
}
