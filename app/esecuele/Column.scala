package esecuele

case class Column(raw: Expression, alias: Option[String]) extends Rep {
  val rep: String = {
    val ex = raw match {
      case _: EmptyExpression.type => None
      case _                       => Some(raw.toString)
    }
    List(ex, alias).withFilter(_.isDefined).map(_.get).mkString("", " AS ", "")
  }

  override def toString: String = rep

  def as(newAlias: Option[String]): Column = Column(raw, newAlias)

  def name: Column = alias.map(Column.apply).getOrElse(expr)

  def expr: Column = Column(raw, None)

  def asc: Column = Column(RawExpression(name.rep + " ASC"), None)

  def desc: Column = Column(RawExpression(name.rep + " DESC"), None)
}

object Column {
  private def parse(expr: String): Column = {
    // can produce an adt for a full parse using a proper parsing library
    val tokens = List(expr.trim.stripSuffix(",").trim)

    tokens match {
      case x :: _ => Column(RawExpression(x), None)
      case Nil    => Column(EmptyExpression)
    }
  }

  val star: Column = column("*")

  def apply(expression: Expression): Column = Column(expression, None)

  def apply(name: String): Column = parse(name)

  def apply(ex: String, asName: String): Column = parse(ex).as(Some(asName))

  def column(name: String): Column = Column(name)

  def literal[T](v: T): Column =
    v match {
      case e: String => Column(RawExpression(s"'$e'"))
      case _         => Column(RawExpression(v.toString))
    }

  def inSet(col: String, values: Seq[String]): Column =
    Functions.in(
      column(col),
      Functions.set(values.map(literal).toSeq)
    )
}
