package models.db

import esecuele.Column.column
import esecuele._
import play.api.Logging

case class SentenceQuery(pmid: String, tableName: String) extends Queryable with Logging {
  val query = {
    val q: Query = SentenceQuery.getQuery(pmid, tableName)
    logger.debug(q.toString)
    q
  }
}

object SentenceQuery extends Logging {
  val pmidC: Column = column("pmid")
  val sectionC: Column = column("section")
  val labelC: Column = column("label")
  val sectionEndC: Column = column("sectionEnd")
  val sectionStartC: Column = column("sectionStart")
  val startInSentenceC: Column = column("startInSentence")
  val endInSentenceC: Column = column("endInSentence")
  val keywordTypeC: Column = column("keywordType")
  val keywordIdC: Column = column("keywordId")

  private def getQuery(pmid: String, tableName: String) = Query(
    Select(
      pmidC :: sectionC :: labelC :: sectionEndC :: sectionStartC :: startInSentenceC :: endInSentenceC :: keywordTypeC :: keywordIdC :: Nil
    ),
    From(column(tableName)),
    Where(Functions.equals(pmidC, column(pmid)))
  )
}
