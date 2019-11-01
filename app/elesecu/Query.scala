package elesecu

case class Query(sections: Seq[QuerySection]) extends Rep {
  override def toString: String = sections.map(_.rep).mkString("", "\n", "")
  override val rep: String = sections.map(_.rep).mkString("", " ", "")
  def toColumn: Column = {
    val q = sections.map(_.rep).mkString("(", " ", ")")
    Column(RawExpression(q), None)
  }
}

object Query {
  def apply(w: With, s: Select, sections: QuerySection*): Query = Query(w +: s +: sections)
  def apply(s: Select, sections: QuerySection*): Query = Query(s +: sections)
}
