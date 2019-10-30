package elesecu

case class Column(raw: Expression, alias: Option[String]) extends Rep {
  lazy val rep: String = {
    val ex = raw match {
      case _: EmptyExpression.type => None
      case _ => Some(raw.toString)
    }
    List(ex, alias).withFilter(_.isDefined).map(_.get).mkString("", " AS ", "")
  }

  override def toString: String = rep

  def as(newAlias: Option[String]): Column = Column(raw, newAlias)
  def name: Column = alias.map(Column.apply).getOrElse(expr)
  def expr: Column = Column(raw, None)
}

object Column {
  private def parse(expr: String): Column  = {
    // can produce an adt for a full parse using a proper parsing library
    val tokens = expr.split(" [aA][sS] ").map(_.trim.stripSuffix(",").trim).toList

    tokens match {
      case x :: y :: _ => Column(RawExpression(x), Some(y))
      case x :: Nil => Column(RawExpression(x))
      case _ => Column(EmptyExpression)
    }
  }

  def apply(expression: Expression): Column = Column(expression, None)
  def apply(name: String): Column = parse(name)
  def apply(ex: String, asName: String): Column = parse(ex).as(Some(asName))

  def column(name: String): Column = Column(name)

  def literal[T](v: T): Column = {
    v match {
      case e: String => Column(RawExpression(s"'$e'"))
      case _ => Column(RawExpression(v.toString))
    }
  }
}
