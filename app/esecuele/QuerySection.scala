package esecuele

abstract class QuerySection extends Rep {
  val name: String
  val content: Seq[Column]
}

case class With(content: Seq[Column]) extends QuerySection {
  override val name: String = "WITH"
  override val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class Select(content: Seq[Column]) extends QuerySection {
  override val name: String = "SELECT"
  override val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class Where(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "WHERE"
  override val rep: String = s"$name ${content.mkString("(", "", ")")}"
}

case class PreWhere(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "PREWHERE"
  override val rep: String = s"$name ${content.mkString("(", "", ")")}"
}

case class ArrayJoin(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "ARRAY JOIN"
  override val rep: String = s"$name ${content.mkString}"
}

case class Having(col: Column) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "HAVING"
  override val rep: String = s"$name ${content.mkString("(", "", ")")}"
}

case class GroupBy(content: Seq[Column]) extends QuerySection {
  override val name: String = "GROUP BY"
  override val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

case class OrderBy(content: Seq[Column]) extends QuerySection {
  override val name: String = "ORDER BY"
  override val rep: String = s"$name ${content.mkString("", ", ", "")}"
}

//LIMIT 1 OFFSET 0 BY disease_id
//limit 10 OFFSET 0;

case class Limit(offset: Int = 0, size: Int) extends QuerySection {
  override val content: Seq[Column] = Nil
  override val name: String = "LIMIT"
  override val rep: String = s"$name $size OFFSET $offset"
}

case class LimitBy(size: Int, offset: Int = 0, by: Seq[Column]) extends QuerySection {
  override val content: Seq[Column] = by
  override val name: String = "LIMIT"
  override val rep: String = s"$name $size OFFSET $offset BY ${content.mkString("", ", ", "")}"
}

case class From(col: Column, alias: Option[String] = None) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "FROM"
  override val rep: String = s"$name ${content.mkString}${alias.map(" " + _).getOrElse("")}"
}

case class Join(
    col: Column,
    setOper: Option[String] = None,
    modifier: Option[String] = None,
    global: Boolean = false,
    alias: Option[String] = None,
    using: Seq[Column] = Seq.empty
) extends QuerySection {
  override val content: Seq[Column] = Seq(col)
  override val name: String = "JOIN"
  override val rep: String =
    Seq(
      if (global) Some("GLOBAL") else None,
      setOper,
      modifier,
      Some(name),
      Some(content.mkString),
      alias,
      Some("USING " + using.mkString("(", ",", ")"))
    ).filter(_.isDefined).map(_.get).mkString(" ").trim
}

// TODO REFACTOR INTO PROPER TYPE
case class FromSelect(select: QuerySection, alias: Option[String] = None) extends QuerySection {
  override val content: Seq[Column] = select.content
  override val name: String = "FROM"
  override val rep: String =
    s"$name (${select.name} ${content.mkString})${alias.map(" " + _).getOrElse("")}"
}

object QuerySection {}
