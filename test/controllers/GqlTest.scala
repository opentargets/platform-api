package controllers

import akka.util.Timeout
import controllers.GqlTest.{createRequest, generateQueryString, getQueryFromFile}
import controllers.api.v4.graphql.GraphQLController
import inputs._
import org.scalacheck.Shrink
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import test_configuration.{ClickhouseTestTag, IntegrationTestTag}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.reflect.io.File

object GqlTest {

  /**
   * Retrieves filename from target resources and returns query as string with quotes escaped.
   */
  def getQueryFromFile(filename: String): String = {
    File(this.getClass.getResource(s"/gqlQueries/$filename.gql").getPath).lines
      .withFilter(_.nonEmpty)
      .map(str =>
        str.flatMap {
          case '"' => "\\\""
          case ch => s"$ch"
        })
      .mkString("\\n")
  }

  def generateQueryString(query: String, variables: String): JsValue =
    Json.parse(s"""{"query": "$query" , $variables }""")

  def createRequest(query: JsValue): Request[JsValue] = {
    FakeRequest(POST, "/graphql")
      .withHeaders(("Content-Type", "application/json"))
      .withBody(query)
  }
}

class GqlTest
  extends PlaySpec
    with GuiceOneAppPerTest
    with Injecting
    with Logging
    with ScalaFutures
    with ScalaCheckDrivenPropertyChecks {

  lazy val controller: GraphQLController = inject[GraphQLController]
  implicit lazy val timeout: Timeout = Timeout(1120, TimeUnit.SECONDS)

  /*
  Use no shrink for all inputs as the standard shrinker will start cutting our inputs strings giving non-sense queries.
  In pure scalaCheck we could use `forAllNoShrink` but this isn't available with the Play wrapper. If we want to use
  scalacheck more broadly we should move the implicit into a tighter scope.
   */
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  val qt: String => String = (in: String) => in

  /**
   *
   * @param gqlCase          representing a combination of generated inputs for testing
   * @param queryTransformer function to modify variables generated by GqlCase. This is useful where the front-end queries
   *                         aren't consistent, eg. some target queries use 'ensgId' while others may use 'id' to refer to
   *                         the same thing. The case class captures the most common use case, but then we can specify
   *                         how to modify it if necessary.
   * @tparam T
   */
  def testQueryAgainstGqlEndpoint[T](gqlCase: GqlCase[T])(
    implicit queryTransformer: String => String = qt): Unit = {
    // get query from front end
    val query: String = gqlCase match {
      case fragment: GqlFragment[_] => fragment.generateFragmentQuery
      case _ => getQueryFromFile(gqlCase.file)
    }

    forAll(gqlCase.inputGenerator) { i =>
      // create test variables
      val variables = queryTransformer(gqlCase.generateVariables(i))
      // create and execute request
      val request = createRequest(generateQueryString(query, variables))
      val resultF: Future[Result] = controller.gqlBody().apply(request)
      val resultS = contentAsString(resultF)

      // validate results
      val resultHasNoErrors = !resultS.contains("errors")
      if (!resultHasNoErrors) {
        logger.error(s"Input: $i returned with error: $resultS")
      }
      resultHasNoErrors mustBe true
    }
  }

  /**
   * Convert from input variable being 'ensgId' to 'ensemblId'
   */
  val ensgTransform = (t: String) => t.replace("ensgId", "ensemblId")

  "Adverse events queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Drug("AdverseEvents_AdverseEventsQuery"))
    }
    "return a valid summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DrugFragment("AdverseEvents_AdverseEventsSummaryFragment"))
    }
  }

  "Association page queries" must {
    "return a valid response for target associations" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(AssociationTarget("TargetPage_TargetAssociations"))
    }
    "return a valid response for disease associations" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(AssociationDisease("DiseasePage_DiseaseAssociations"))
    }
  }

  "Bibliography queries" must {
    "return valid response for BibliographyQuery" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(Target("Bibliography_BibliographyQuery"))(t =>
        t.replace("ensgId", "id"))
    }
    "return valid response for BibliographySimilarEntities" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(Target("Bibliography_SimilarEntities"))(t =>
        t.replace("ensgId", "id"))
    }
    "return valid response for Bibliography summary fragment" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(DiseaseFragment("Bibliography_BibliographySummaryFragment"))
    }
    "return valid response for Bibliography similar entities summary fragment" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(TargetFragment("Bibliography_SimilarEntitiesSummary"))
    }
  }

  "Cancer gene census queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("CancerGeneCensus_sectionQuery"))
    }

  }

  "Cancer biomarker queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("CancerBiomarkers_CancerBiomarkers"))
    }
    "return a valid response for summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("CancerBiomarkers_CancerBiomarkersSummary"))
    }

  }

  "Cancer hallmarks queries" must {
    "return a valid response for summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("CancerHallmarks_HallmarksSummaryFragment"))
    }

  }

  "Summary fragments" must {
    "return a valid response for chemical probes" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("ChemicalProbes_ProbesSummaryFragment"))
    }
    "return a valid response for cancer gene census summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(
        DiseaseSummaryFragment("CancerGeneCensus_CancerGeneCensusSummaryQuery"))
    }
    "return a valid response for chembl summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("Chembl_ChemblSummaryFragment"))
    }
    "return a valid response for clingen summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("ClinGen_ClinGenSummaryFragment"))
    }
    "return a valid response for Europe PMC summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("EuropePmc_EuropePmcSummaryFragment"))
    }
    "return a valid response for EVA summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("EVA_EVASummaryQuery"))
    }
    "return a valid response for EVA somatic summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("EVASomatic_EvaSomaticSummary"))
    }
    "return a valid response for Expression atlas summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("ExpressionAtlas_ExpressionAtlasSummary"))
    }
    "return a valid response for Gene to phenotype summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(
        DiseaseSummaryFragment("Gene2Phenotype_Gene2PhenotypeSummaryFragment"))
    }
    "return a valid response for Genomics England summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(
        DiseaseSummaryFragment("GenomicsEngland_GenomicsEnglandSummaryFragment"))
    }
    "return a valid response for IntoOGen summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("IntOgen_IntOgenSummaryQuery"))
    }
    "return a valid response for Open Targets Genetics summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("OTGenetics_OpenTargetsGeneticsSummary"))
    }
    "return a valid response for Open Targets Projects summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("OTProjects_OTProjectsSummaryFragment"))
    }
    "return a valid response for orphanet summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("Orphanet_OrphanetSummaryFragment"))
    }
    "return a valid response for Phenodigm summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("Phenodigm_PhenodigmSummaryFragment"))
    }
    "return a valid response for PheWAS Catalogue summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("PheWASCatalog_PheWASCatalogSummaryQuery"))
    }
    "return a valid response for Progeny summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("Progeny_ProgenySummaryFragment"))
    }
    "return a valid response for Reactome summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("Reactome_ReactomeSummary"))
    }
    "return a valid response for Slap enrich summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("SlapEnrich_SlapEnrichSummaryFragment"))
    }
    "return a valid response for SysBio summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseSummaryFragment("SysBio_SysBioSummaryFragment"))
    }
    "return a valid response for Uniprot literature summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(
        DiseaseSummaryFragment("UniProtLiterature_UniprotLiteratureSummary"))
    }
    "return a valid response for Uniprot variants summary fragments" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(
        DiseaseSummaryFragment("UniProtVariants_UniprotVariantsSummaryQuery"))
    }
  }

  "Chembl queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Chembl_ChemblQuery"))
    }
  }

  "Chemical Probe queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("ChemicalProbesQuery"))
    }
  }
  "ClinGen queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("ClinGen_ClingenQuery"))
    }
  }

  "ComparativeGenomics queries" must {
    "CompGenomics should return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("ComparativeGenomics_CompGenomics"))(ensgTransform)
    }
    "Comparative genomics fragment should return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("ComparativeGenomics_CompGenomicsSummaryFragment"))
    }
  }

  "CRISPR queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("CRISPR_CrisprQuery"))
    }
  }

  "Disease page queries" must {
    "return a valid response for disease facets" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(DiseaseAggregationfilter("DiseasePage_DiseaseFacets"))
    }
    "return a valid response for disease page" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Disease("DiseasePage_DiseasePage"))
    }

    "return a valid response for disease profile header" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseFragment("DiseasePage_ProfileHeader"))
    }
    "return a valid response for known drugs" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseFragment("KnownDrugs_KnownDrugsSummaryFragment"))
    }
    "return a valid response for ontology summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseFragment("Ontology_OntologySummaryFragment"))
    }
    "return a valid response for phenotype summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DiseaseFragment("Phenotypes_PhenotypesSummaryFragment"))
    }
  }

  "Drug queries" must {
    "return a valid response for drug page" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Drug("DrugPage_DrugPage"))
    }
    "return a valid response for drug warning query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Drug("DrugWarnings_DrugWarningsQuery"))
    }
    "return a valid response for indications" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Drug("Indications_IndicationsQuery"))
    }
    "return a valid response for mechanisms of action" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Drug("MechanismsOfAction_MechanismsOfActionQuery"))
    }
    "return a valid response for mechanisms of action summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(
        DrugFragment("MechanismsOfAction_MechanismsOfActionSummaryFragment"))
    }
    "return a valid response for profile header" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DrugFragment("DrugPage_ProfileHeader"))
    }
    "return a valid response for drug warning summary" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DrugFragment("DrugWarnings_DrugWarningsSummaryFragment"))
    }
    "return a valid response for indications summary" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(DrugFragment("Indications_IndicationsSummaryFragment"))
    }
  }

  "EuropePmc queries" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSizeCursor("EuropePmc_sectionQuery"))
    }
  }

  "EVA queries" must {
    "return valid responses for clinvar" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSizeCursor("EVA_ClinvarQuery"))
    }
    "return valid responses for somatic" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSizeCursor("EVASomatic_EvaSomaticQuery"))
    }
  }

  "Evidence page queries" must {
    "return valid responses " taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDisease("EvidencePage_EvidencePageQuery"))(q =>
        q.replace("ensemblId", "ensgId"))
    }
  }

  "Expression queries" must {
    "return valid responses " taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("Expression_ExpressionQuery"))(ensgTransform)
    }
    "return valid responses for expression atlas" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("ExpressionAtlas_ExpressionAtlasQuery"))
    }
    "return valid responses for expression summary" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("Expression_ExpressionSummary"))
    }
  }

  "Gene2Phenotype_sectionQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDisease("Gene2Phenotype_sectionQuery"))
    }
  }

  "GenomicsEngland_sectionQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDisease("GenomicsEngland_sectionQuery"))
    }
  }

  "GeneOntologyQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(GeneOntology("GeneOntologyQuery"))
    }
  }

  "IntOgen_sectionQuery" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("IntOgen_sectionQuery"))
    }
  }

  "Known Drugs query" must {
    "return a valid response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(KnownDrugs("KnownDrugs_KnownDrugsQuery"))
    }
  }

  "MolecularInteractions" must {
    "return a valid response for interaction stats" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("MolecularInteractions_InteractionsStats"))
    }
  }

  "OT genetics queries" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("OTGenetics_sectionQuery"))
    }
  }

  "OT projects queries" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Disease("OTProjects_OTProjectsQuery"))
    }
  }

  "Orphanet queries" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Orphanet_OrphanetQuery"))
    }
  }

  "Phenodigm_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Phenodigm_sectionQuery"))
    }
  }

  "Phenotypes_query" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Disease("Phenotypes_PhenotypesQuery"))
    }
  }

  "PheWAS queries" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("PheWASCatalog_PhewasCatalogQuery"))
    }
  }

  "Progeny_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Progeny_sectionQuery"))
    }
  }

  "ProteinInformation" must {
    "return valid responses for section query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("ProteinInformation_sectionQuery"))
    }
    "return valid responses for summary query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("ProteinInformation_summaryQuery"))
    }
  }

  "Reactome_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("Reactome_sectionQuery"))
    }
  }

  "Search_SearchQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Search("Search_SearchQuery"))
    }
  }

  "SearchPage_SearchPageQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(SearchPage("SearchPage_SearchPageQuery"))
    }
  }

  "SlapEnrich_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("SlapEnrich_sectionQuery"))
    }
  }

  "SysBio_sectionQuery" must {
    "return valid responses" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("SysBio_sectionQuery"))
    }
  }

  "Target page" must {
    "return valid associations visualisation" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(TargetAggregationfilter("TargetPage_AssociationsViz"))
    }
    "return valid target facets" taggedAs(IntegrationTestTag, ClickhouseTestTag) in {
      testQueryAgainstGqlEndpoint(TargetAggregationfilter("TargetPage_TargetFacets"))
    }
    "return valid target page" taggedAs (IntegrationTestTag) in {
      testQueryAgainstGqlEndpoint(Target("TargetPage_TargetPage"))
    }
    "return valid target profile header fragment" taggedAs (IntegrationTestTag) in {
      testQueryAgainstGqlEndpoint(TargetFragment("TargetPage_TargetProfileHeader"))
    }
    "return valid tep summary fragment" taggedAs (IntegrationTestTag) in {
      testQueryAgainstGqlEndpoint(TargetFragment("Tep_TepSummaryFragment"))
    }
    "return valid tractability summary fragment" taggedAs (IntegrationTestTag) in {
      testQueryAgainstGqlEndpoint(TargetFragment("Tractability_TractabilitySummary"))
    }
    "return valid gene ontology response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("GeneOntology_GeneOntologySummary"))
    }
    "return valid known drugs fragment response" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("KnownDrugs_KnownDrugsSummary"))
    }
    "return valid response for molecular interactions" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("MolecularInteractions_InteractionsSummary"))
    }
    "return valid response for mouse phenotypes fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("MousePhenotypes_MousePhenotypesSummary"))
    }
    "return valid response for pathways fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("Pathways_PathwaysSummary"))
    }
    "return valid response for pathways target query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("Pathways_Pathways"))(ensgTransform)
    }
    "return valid response for protvista target fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("ProtVista_summaryQuery"))
    }
    "return valid response for safety summary fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("Safety_summaryQuery"))
    }
    "return valid response for subcellular location target query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(Target("SubcellularLocation_SubcellularLocation"))(ensgTransform)
    }
    "return valid response for subcellular location fragment" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetFragment("SubcellularLocation_SubcellularLocationFragment"))
    }
  }

  "Uniprot queries" must {
    "return valid response for literature query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("UniProtLiterature_UniprotLiteratureQuery"))
    }
    "return valid response for variants query" taggedAs IntegrationTestTag in {
      testQueryAgainstGqlEndpoint(TargetDiseaseSize("UniProtVariants_UniprotVariantsQuery"))
    }

  "Target queries" must {
        "support resolution of full object" taggedAs IntegrationTestTag in {
          testQueryAgainstGqlEndpoint(Target("Target_full_object"))
        }
      }
    }
}
