package elesecu

abstract class QuerySection extends Rep {
  val name: String
  val content: Seq[Column]
}

case class With(content: Seq[Column]) extends QuerySection {
  override val name: String = "WITH"
  override lazy val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class Select(content: Seq[Column]) extends QuerySection {
  override val name: String = "SELECT"
  override lazy val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class Where(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "WHERE"
  override lazy val rep: String = s"$name ${content.mkString("(", "", ")")}"
}

case class PreWhere(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "PREWHERE"
  override lazy val rep: String = s"$name ${content.mkString("(", "", ")")}"
}

case class ArrayJoin(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "ARRAY JOIN"
  override lazy val rep: String = s"$name ${content.mkString}"
}

case class Having(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "HAVING"
  override lazy val rep: String = s"$name ${content.mkString("(", "", ")")}"
}

case class GroupBy(content: Seq[Column]) extends QuerySection {
  override val name: String = "GROUP BY"
  override lazy val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class OrderBy(content: Seq[Column]) extends QuerySection {
  override val name: String = "ORDER BY"
  override lazy val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class From(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "FROM"
  override lazy val rep: String = s"$name ${content.mkString}"
}

object QuerySection {
}
