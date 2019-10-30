package elesecu

/**
 * Place() can be all table functions
 * Statement(from: Place()
 *
 * SELECT [DISTINCT] expr_list
 * [FROM [db.]table | (subquery) | table_function] [FINAL]
 * [SAMPLE sample_coeff]
 * [ARRAY JOIN ...]
 * [GLOBAL] [ANY|ALL] [INNER|LEFT|RIGHT|FULL|CROSS] [OUTER] JOIN (subquery)|table USING columns_list
 * [PREWHERE expr]
 * [WHERE expr]
 * [GROUP BY expr_list] [WITH TOTALS]
 * [HAVING expr]
 * [ORDER BY expr_list]
 * [LIMIT [n, ]m]
 * [UNION ALL ...]
 * [INTO OUTFILE filename]
 * [FORMAT format]
 * [LIMIT [offset_value, ]n BY columns]
 */

case class Query(sections: Seq[QuerySection]) extends Rep {
  override val rep: String = sections.map(_.rep).mkString("", "\n", ";")
  def toColumn: Column = {
    val q = sections.map(_.rep).mkString("(", " ", ")")
    Column(RawExpression(q), None)
  }
}

object Query {
  def apply(w: With, s: Select, sections: QuerySection*): Query = Query(w +: s +: sections)
  def apply(s: Select, sections: QuerySection*): Query = Query(s +: sections)
}
