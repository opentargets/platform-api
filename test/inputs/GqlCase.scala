package inputs

import controllers.GqlTest
import models.entities.AggregationFilter
import org.scalacheck.Gen
import play.api.Logging

sealed trait GqlCase[T] extends GqlItTestInputs with Logging {
  val file: String
  val inputGenerator: Gen[T]

  def generateVariables(inputs: T): String
}

sealed trait GqlFragment[T] extends GqlCase[T] {
  // first capture group is name of fragment ot insert into query
  val fragmentRegex = """^fragment ([\w]+) on [\w]+""".r

  lazy val fragmentQuery: String = GqlTest.getQueryFromFile(file)
  lazy val fragmentName: String = fragmentRegex findFirstMatchIn fragmentQuery match {
    case Some(value) => value.group(1)
    case None =>
      logger.error(s"Unable to extract fragment name from $file.")
      ""
  }

  def generateFragmentQuery: String
}

case class AssociationDisease(file: String) extends GqlCase[(String, AggregationFilter)] {
  val inputGenerator = for {
    disease <- diseaseGenerator
    agg <- aggregationfilterGenerator
  } yield (disease, agg)

  def generateVariables(inputs: (String, AggregationFilter)) =
    s"""
      "variables": {
      "efoId": "${inputs._1}",
      "index": 0,
      "size": 10,
      "sortBy": "",
      "aggregationFilters": [
        {
        "name": "${inputs._2.name}",
        "path": ${inputs._2.path.mkString("[\"", "\", \"", "\"]")}
        }]
    }
    """
}

case class AssociationTarget(file: String) extends GqlCase[(String, AggregationFilter)] {
  val inputGenerator = for {
    target <- geneGenerator
    agg <- aggregationfilterGenerator
  } yield (target, agg)

  def generateVariables(inputs: (String, AggregationFilter)) =
    s"""
      "variables": {
      "ensemblId": "${inputs._1}",
      "index": 0,
      "size": 10,
      "sortBy": "",
      "aggregationFilters": [
        {
        "name": "${inputs._2.name}",
        "path": ${inputs._2.path.mkString("[\"", "\", \"", "\"]")}
        }]
    }
    """
}

case class Disease(file: String) extends GqlCase[String] {
  val inputGenerator = diseaseGenerator

  def generateVariables(disease: String): String =
    s"""
      "variables": {
      "efoId": "$disease",
      "size": 10,
      "index": 0
    }
    """
}

case class DiseaseAggregationfilter(file: String) extends GqlCase[(String, AggregationFilter)] {
  val inputGenerator = for {
    disease <- diseaseGenerator
    agg <- aggregationfilterGenerator
  } yield (disease, agg)

  def generateVariables(inputs: (String, AggregationFilter)): String =
    s"""
      "variables": {
      "efoId": "${inputs._1}",
      "aggregationFilters": [
        {
        "name": "${inputs._2.name}",
        "path": ${inputs._2.path.mkString("[\"", "\", \"", "\"]")}
        }]
    }
    """
}

abstract class AbstractDrug extends GqlCase[String] {
  val inputGenerator = drugGenerator

  override def generateVariables(drugId: String): String =
    s"""
      "variables": {
      "chemblId": "$drugId"
    }
    """
}

abstract class AbstractTarget extends GqlCase[String] {
  val inputGenerator = geneGenerator

  def generateVariables(target: String): String =
    s"""
      "variables": {
      "ensgId": "$target",
      "size": 10,
      "index": 0
    }
    """
}

abstract class AbstractDisease extends GqlCase[String] {
  val inputGenerator = diseaseGenerator

  def generateVariables(disease: String): String =
    s"""
      "variables": {
      "efoId": "$disease"
    }
    """
}

case class DrugFragment(file: String) extends AbstractDrug with GqlFragment[String] {

  def generateFragmentQuery: String =
    s"$fragmentQuery query DrugFragment(xyz: String!) { drug(chemblId: xyz) { ...$fragmentName } }"
      .replace("xyz", "$chemblId")
}

case class TargetFragment(file: String) extends AbstractTarget with GqlFragment[String] {
  def generateFragmentQuery: String =
    s"$fragmentQuery query TargetFragment(xyz: String!) { target(ensemblId: xyz) { ...$fragmentName } }"
      .replace("xyz", "$ensgId")
}

case class DiseaseFragment(file: String) extends AbstractDisease with GqlFragment[String] {
  def generateFragmentQuery: String =
    s"$fragmentQuery query DiseaseFragment(xyz: String!) { disease(efoId: xyz) { ...$fragmentName } }"
      .replace("xyz", "$efoId")
}

/*
This is a fragment on disease which takes a gene as an argument. Used on the FE to create
summary information.
 */
case class DiseaseSummaryFragment(file: String) extends GqlFragment[(String, String)] {
  def generateFragmentQuery: String =
    s"$fragmentQuery query DiseaseFragment(xyz: String!, ensgId: String) { disease(efoId: xyz) { ...$fragmentName } }"
      .replace("xyz", "$efoId")

  val inputGenerator = targetDiseaseGenerator

  def generateVariables(inputs: (String, String)) =
    s"""
      "variables": {
      "ensgId": "${inputs._1}",
      "efoId": "${inputs._2}"
    }
    """
}

case class Drug(file: String) extends AbstractDrug with GqlCase[String]

case class GeneOntology(file: String) extends GqlCase[List[String]] {
  val inputGenerator = goListGenerator

  def generateVariables(inputs: List[String]): String =
    s"""
       "variables": {
         "goIds": "${inputs.mkString("[", ",", "]")}"
       }
     """
}

case class KnownDrugs(file: String) extends GqlCase[String] {
  val inputGenerator = geneGenerator

  def generateVariables(target: String) =
    s"""
      "variables": {
      "ensgId": "$target",
      "cursor": null,
      "freeTextQuery": null
    }
    """
}

case class Search(file: String) extends GqlCase[String] {
  override val inputGenerator = searchGenerator

  def generateVariables(searchTerm: String): String =
    s"""
      "variables": {
      "queryString": "$searchTerm"
    }
    """
}

case class SearchPage(file: String) extends GqlCase[(String, String, Int)] {
  val inputGenerator = for {
    query <- searchGenerator
    entities <- Gen
      .atLeastOne(Seq("target", "disease", "drug"))
      .map(_.mkString("[\"", "\", \"", "\"]"))
    page <- sizeGenerator
  } yield (query, entities, page)

  def generateVariables(inputs: (String, String, Int)): String =
    s"""
      "variables": {
      "queryString": "${inputs._1}",
      "index": ${inputs._3},
      "entityNames": ${inputs._2}
    }
    """
}

case class Target(file: String) extends AbstractTarget with GqlCase[String]

case class TargetDisease(file: String) extends GqlCase[(String, String)] {
  val inputGenerator = for {
    gene <- geneGenerator
    disease <- diseaseGenerator
  } yield (gene, disease)

  def generateVariables(inputs: (String, String)): String = {
    val (target, disease) = inputs
    s"""
      "variables": {
      "efoId": "$disease",
      "ensemblId": "$target"
    }
    """
  }
}

case class TargetAggregationfilter(file: String) extends GqlCase[(String, AggregationFilter)] {
  val inputGenerator = for {
    gene <- geneGenerator
    aggregationFilter <- aggregationfilterGenerator
  } yield (gene, aggregationFilter)

  def generateVariables(inputs: (String, AggregationFilter)): String =
    s"""
      "variables": {
      "ensemblId": "${inputs._1}",
      "size": 10,
      "index": 0,
      "aggregationFilters": [
        {
        "name": "${inputs._2.name}",
        "path": ${inputs._2.path.mkString("[\"", "\", \"", "\"]")}
        }]
    }
    """

}

case class TargetDiseaseSize(file: String) extends GqlCase[(String, String, Int)] {
  val inputGenerator = targetDiseaseSizeGenerator

  def generateVariables(input: (String, String, Int)): String =
    generateVariables(input._1, input._2, input._3)

  def generateVariables(target: String, disease: String, size: Int): String =
    s"""
      "variables": {
      "efoId": "$disease",
      "ensemblId": "$target",
      "size": $size
    }
    """
}

case class TargetDiseaseSizeCursor(file: String) extends GqlCase[(String, String, Int)] {
  val inputGenerator = targetDiseaseSizeGenerator

  def generateVariables(input: (String, String, Int)): String =
    generateVariables(input._1, input._2, input._3)

  def generateVariables(target: String, disease: String, size: Int): String =
    s"""
    "variables": {
      "efoId": "$disease",
      "ensemblId": "$target",
      "size": $size,
      "cursor": null
    }
  """
}
