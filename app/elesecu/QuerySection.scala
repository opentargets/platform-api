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

case class From(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "FROM"
  override lazy val rep: String = s"$name ${content.mkString}"
}

object QuerySection {
}
