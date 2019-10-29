package elesecu

case class Column(raw: Expression, alias: Option[String]) {
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

  def lit[T](v: T): Column =  {
    v match {
      case e: String => Column(RawExpression(e))
      case _ => Column(RawExpression(v.toString))
    }
  }

  def uniq(cols: Seq[Column]): Column = column(cols.map(_.name).mkString("uniq(", ",", ")"))
  def uniq(col: Column, cols: Column*): Column = uniq(col +: cols)
  def groupArray(col: Column): Column = column(s"groupArray(${col.name})")
  def arrayMap(lambda: String, col: Column): Column = column(s"arrayMap($lambda,${col.name})")
  def any(col: Column): Column = column(s"any(${col.name})")
  def array(cols: Seq[Column]): Column = column(cols.map(_.name).mkString("array(",",", ")"))
  def array(col: Column, cols: Column*): Column = array(col +: cols)
  def joinGet(tableName: String, fieldName: String, col: Column): Column =
    column(s"joinGet('$tableName','$fieldName',${col.name})")
  def flatten(col: Column): Column = column(s"flatten(${col.name})")
  def arraySort(col: Column): Column = column(s"arraySort(${col.name})")
  def arrayReverseSort(col: Column): Column = column(s"arrayReverseSort(${col.name})")
  def arraySlice(col: Column, pos: Int, size: Int): Column =
    column(s"arraySlice(${col.name},${lit(pos).name},${lit(size).name})")
  def length(col: Column): Column = column(s"length(${col.name})")
  def range(col: Column): Column = column(s"range(${col.name})")
  def arraySum(lambda: String, col1: Column, col2: Column): Column =
    column(s"arraySum($lambda,${col1.name},${col2.name})")
  def div(col1: Column, col2: Column): Column = column(s"(${col1.name} / ${col2.name})")
}
