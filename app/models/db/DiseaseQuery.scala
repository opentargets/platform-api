package models.db

import esecuele.Column.column
import esecuele.Column.literal
import esecuele._
import play.api.Logging

case class DiseaseQuery(ids: Seq[String], tableName: String) extends Queryable with Logging {
  val query = {
    val q: Query = DiseaseQuery.getQuery(ids, tableName)
    logger.debug(q.toString)
    q
  }
}

object DiseaseQuery extends Logging {
  val id: Column = column("id")
  val code: Column = column("code")
  val name: Column = column("name")
  val description: Column = column("description")
  val dbXRefs: Column = column("dbXRefs")
  val parents: Column = column("parents")
  val synonyms: Column = column("synonyms")
  val obsoleteTerms: Column = column("obsoleteTerms")
  val obsoleteXRefs: Column = column("obsoleteXRefs")
  val children: Column = column("children")
  val ancestors: Column = column("ancestors")
  val therapeuticAreas: Column = column("therapeuticAreas")
  val descendants: Column = column("descendants")
  val ontology: Column = column("ontology.isTherapeuticArea")

  private def getQuery(ids: Seq[String], tableName: String) = Query(
    Select(
      id :: code :: name :: description :: dbXRefs :: parents :: synonyms ::
        obsoleteTerms :: obsoleteXRefs :: children :: ancestors :: therapeuticAreas ::
        descendants :: ontology :: Nil
    ),
    From(column(tableName)),
    Where(Functions.in(id, Functions.set(ids.map(literal).toSeq)))
  )
}
