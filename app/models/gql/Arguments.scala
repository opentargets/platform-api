package models.gql

import models.entities.Configuration.*
import models.entities.Pagination.*
import models.entities.*
import sangria.schema.*
import sangria.marshalling.playJson.*
import sangria.marshalling.FromInput
import sangria.util.tag.@@
import play.api.libs.json.{Format, Json}

object StudyTypeEnum extends Enumeration {

  type StudyType = Value
  val tuqtl, pqtl, eqtl, sqtl, sctuqtl, scpqtl, sceqtl, scsqtl, gwas = Value

  implicit val studyTypeF: Format[StudyType] = Json.formatEnum(this)
}

object Arguments {
  import sangria.macros.derive._
  implicit val StudyType: EnumType[StudyTypeEnum.Value] =
    deriveEnumType[StudyTypeEnum.Value](
      EnumTypeDescription(
        "Study type, distinguishing GWAS from different classes of molecular QTL studies"
      ),
      DocumentValue("gwas", "Genome-wide association study (GWAS) of complex traits or diseases"),
      DocumentValue("eqtl", "Bulk tissue expression quantitative trait locus (eQTL) study"),
      DocumentValue("pqtl", "Bulk tissue protein quantitative trait locus (pQTL) study"),
      DocumentValue("sqtl", "Bulk tissue splicing quantitative trait locus (sQTL) study"),
      DocumentValue("tuqtl",
                    "Bulk tissue transcript uptake quantitative trait locus (tuQTL) study"
      ),
      DocumentValue("sceqtl", "Single-cell expression quantitative trait locus (sc-eQTL) study"),
      DocumentValue("scpqtl", "Single-cell protein quantitative trait locus (sc-pQTL) study"),
      DocumentValue("scsqtl", "Single-cell splicing quantitative trait locus (sc-sQTL) study"),
      DocumentValue("sctuqtl",
                    "Single-cell transcript uptake quantitative trait locus (sc-tuQTL) study"
      )
    )
  val paginationGQLImp: InputObjectType[Pagination] = deriveInputObjectType[Pagination](
    InputObjectTypeDescription(
      "Pagination settings for controlling result set size and page navigation. Uses zero-based indexing to specify which page of results to retrieve."
    ),
    DocumentInputField("index", "Zero-based page index"),
    DocumentInputField("size", "Number of items per page [Default: 25, Max: 3000]"),
  )

  val datasourceSettingsInputImp: InputObjectType[DatasourceSettings] =
    deriveInputObjectType[DatasourceSettings](
      InputObjectTypeName("DatasourceSettingsInput"),
      InputObjectTypeDescription(
        "Input type for datasource settings configuration. Allows customization of how individual datasources contribute to target-disease association score calculations. Weights must be between 0 and 1, and can control ontology propagation and evidence requirements."
      ),
      DocumentInputField("id", "Datasource identifier"),
      DocumentInputField("weight", "Weight assigned to the datasource. Should be between 0 and 1"),
      DocumentInputField("propagate",
                         "Whether evidence from this datasource is propagated through the ontology"
      ),
      DocumentInputField(
        "required",
        "Whether evidence from this datasource is required to compute association scores"
      )
    )

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

  val pageArg: Argument[Option[Pagination]] = Argument("page",
                                                       OptionInputType(paginationGQLImp),
                                                       description =
                                                         "Pagination settings with index and size"
  )
  val pageSize: Argument[Option[Int]] =
    Argument("size", OptionInputType(IntType), description = "Number of items per page [Default: 25, Max: 3000]")
  val cursor: Argument[Option[String]] =
    Argument("cursor", OptionInputType(StringType), description = "Opaque cursor for pagination")
  val scoreThreshold: Argument[Option[Double]] = Argument(
    "scoreThreshold",
    OptionInputType(FloatType),
    description = "Threshold similarity between 0 and 1"
  )
  val databaseName: Argument[Option[String]] =
    Argument("sourceDatabase", OptionInputType(StringType), description = "Source database name")
  val queryString: Argument[String] =
    Argument("queryString", StringType, description = "Search query string")
  val category: Argument[Option[String]] =
    Argument("category", OptionInputType(StringType), description = "Category filter")
  val queryTerms: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("queryTerms", ListInputType(StringType), description = "List of query terms to map")
  val optQueryString: Argument[Option[String]] =
    Argument("queryString", OptionInputType(StringType), description = "Search query string")
  val freeTextQuery: Argument[Option[String]] =
    Argument("freeTextQuery",
             OptionInputType(StringType),
             description = "Free-text search query string"
    )
  val efoId: Argument[String] = Argument("efoId", StringType, description = "EFO ID")
  val efoIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("efoIds", ListInputType(StringType), description = "EFO ID")
  val ensemblId: Argument[String] = Argument("ensemblId", StringType, description = "Ensembl ID")
  val ensemblIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("ensemblIds", ListInputType(StringType), description = "List of Ensembl IDs")
  val ensemblIdsOpt: Argument[Option[Seq[String]]] =
    Argument("ensemblIds",
             OptionInputType(ListInputType(StringType)),
             description = "List of Ensembl IDs"
    )
  val chemblId: Argument[String] = Argument("chemblId", StringType, description = "Chembl ID")
  val chemblIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("chemblIds", ListInputType(StringType), description = "List of Chembl IDs")
  val goIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("goIds", ListInputType(StringType), description = "List of GO IDs, eg. GO:0005515")
  val variantId: Argument[String] = Argument("variantId", StringType, description = "Variant ID")
  val variantIds: Argument[Option[Seq[String]]] =
    Argument("variantIds",
             OptionInputType(ListInputType(StringType)),
             description = "List of variant IDs in CHROM_POS_REF_ALT format"
    )
  val studyId: Argument[Option[String]] =
    Argument("studyId", OptionInputType(StringType), description = "Study ID")
  val studyIds: Argument[Option[Seq[String]]] =
    Argument("studyIds", OptionInputType(ListInputType(StringType)), description = "Study IDs")
  val diseaseId: Argument[Option[String]] =
    Argument("diseaseId", OptionInputType(StringType), description = "Disease ID")
  val diseaseIds: Argument[Option[Seq[String]]] =
    Argument("diseaseIds", OptionInputType(ListInputType(StringType)), description = "Disease IDs")
  val studyTypes =
    Argument("studyTypes", OptionInputType(ListInputType(StudyType)), description = "Study types")
  val regions: Argument[Option[Seq[String]]] =
    Argument("regions",
             OptionInputType(ListInputType(StringType)),
             description = "List of genomic regions (e.g., 1:100000-200000)"
    )
  val studyLocusId: Argument[String] =
    Argument("studyLocusId", StringType, description = "Study-locus ID")
  val studyLocusIds: Argument[Option[Seq[String]]] =
    Argument("studyLocusIds",
             OptionInputType(ListInputType(StringType)),
             description = "Study-locus IDs"
    )
  val enableIndirect: Argument[Option[Boolean]] = Argument(
    "enableIndirect",
    OptionInputType(BooleanType),
    "Use the disease ontology to retrieve all its descendants and capture all their associated studies."
  )
  val indirectEvidences: Argument[Option[Boolean]] = Argument(
    "enableIndirect",
    OptionInputType(BooleanType),
    "Use the disease ontology to retrieve all its descendants and capture their associated evidence."
  )

  val indirectTargetEvidences: Argument[Option[Boolean]] = Argument(
    "enableIndirect",
    OptionInputType(BooleanType),
    "Utilize the target interactions to retrieve all diseases associated with them and capture their respective evidence."
  )

  val startYear: Argument[Option[Int]] =
    Argument("startYear",
             OptionInputType(IntType),
             description = "Year at the lower end of the filter"
    )
  val startMonth: Argument[Option[Int]] =
    Argument(
      "startMonth",
      OptionInputType(IntType),
      description =
        "Month at the lower end of the filter. This value will be ignored if startYear is not set"
    )
  val endYear: Argument[Option[Int]] =
    Argument("endYear",
             OptionInputType(IntType),
             description = "Year at the higher end of the filter"
    )
  val endMonth: Argument[Option[Int]] =
    Argument(
      "endMonth",
      OptionInputType(IntType),
      description =
        "Month at the higher end of the filter. This value will be ignored if endYear is not set"
    )

  val BFilterString: Argument[Option[String]] = Argument(
    "BFilter",
    OptionInputType(StringType),
    description = "Filter to apply to the ids with string prefixes"
  )
  val scoreSorting: Argument[Option[String]] = Argument(
    "orderByScore",
    OptionInputType(StringType),
    description = "Ordering for the associations. Accepts a string with two words separated by a space. The first word is the column to sort by: either `score` to use the overall association score (default), a datasource id (e.g., `impc`), or a datatype id (e.g., `animal_model`). The second word is the order: `desc` (default) or `asc`.",
  )

  val AId: Argument[String] =
    Argument("A",
             StringType,
             description = "Fixed entity A identifier (e.g., target or disease ID)"
    )
  val AIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] =
    Argument("As",
             ListInputType(StringType),
             description = "List of fixed entity A identifiers (e.g., target or disease IDs)"
    )
  val BIds: Argument[Option[Seq[String]]] =
    Argument("Bs",
             OptionInputType(ListInputType(StringType)),
             description = "List of disease or target IDs"
    )

  val idsArg: Argument[Option[Seq[String]]] = Argument(
    "additionalIds",
    OptionInputType(ListInputType(StringType)),
    description = "List of IDs (EFO disease IDs, Ensembl gene IDs, or ChEMBL molecule IDs)"
  )
  val requiredIds: Argument[Seq[String @@ FromInput.CoercedScalaResult]] = Argument(
    "ids",
    ListInputType(StringType),
    description = "List of IDs (EFO disease IDs, Ensembl gene IDs, or ChEMBL molecule IDs)"
  )
  val thresholdArg: Argument[Option[Double]] = Argument(
    "threshold",
    OptionInputType(FloatType),
    description = "Threshold similarity between 0 and 1"
  )

  val datasourceSettingsListArg: Argument[Option[Seq[DatasourceSettings]]] =
    Argument("datasources",
             OptionInputType(ListInputType(datasourceSettingsInputImp)),
             description = "List of datasource settings"
    )

  val facetFiltersListArg: Argument[Option[Seq[String]]] = Argument(
    "facetFilters",
    OptionInputType(ListInputType(StringType)),
    description = "List of the facet IDs to filter by (using AND)"
  )

  val includeMeasurements: Argument[Option[Boolean]] = Argument(
    "includeMeasurements",
    OptionInputType(BooleanType),
    description = "Whether to include measurements in the response"
  )
}
