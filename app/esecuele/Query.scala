package esecuele

trait AnyQuery extends Rep

case class Query(sections: Seq[QuerySection]) extends AnyQuery {
  override def toString: String = sections.map(_.rep).mkString("", "\n", "")

  override val rep: String = sections.map(_.rep).mkString("", " ", "")

  def toColumn(named: Option[String] = None): Column = {
    val q = sections.map(_.rep).mkString("(", " ", ")") + s"${named.map(" " + _).getOrElse("")}"
    Column(RawExpression(q), None)
  }
}

object Query {
  def apply(w: With, s: Select, sections: QuerySection*): Query = Query(w +: s +: sections)

  def apply(w: With, s: Select, section: QuerySection, sections: Option[QuerySection]*): Query =
    Query(w +: s +: section +: sections.withFilter(_.isDefined).map(_.get))

  def apply(s: Select, sections: QuerySection*): Query = Query(s +: sections)

  def apply(s: Select, section: QuerySection, sections: Option[QuerySection]*): Query =
    Query(s +: section +: sections.withFilter(_.isDefined).map(_.get))
}
