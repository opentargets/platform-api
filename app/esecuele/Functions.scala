package esecuele

import esecuele.Column.{column, literal}

object Functions {
  private def f(name: String, cols: Seq[Column]): Column =
    column(cols.map(_.name).mkString(s"$name(", ",", ")"))

  private def f(name: String, col: Column, cols: Column*): Column = f(name, col +: cols)

  def apply(name: String, cols: Seq[Column]): Column = f(name, cols)

  def apply(name: String, col: Column, cols: Column*): Column = apply(name, col +: cols)

  def set(cols: Seq[Column]): Column = f("", cols)

  def uniq(cols: Seq[Column]): Column = f("uniq", cols)

  def uniq(col: Column, cols: Column*): Column = uniq(col +: cols)

  def distinct(cols: Seq[Column]): Column = f("distinct", cols)

  def distinct(col: Column, cols: Column*): Column = distinct(col +: cols)

  def groupArray(col: Column): Column = f("groupArray", col)

  def sumForEach(col: Column): Column = f("sumForEach", col)

  def groupUniqArray(col: Column): Column = f("groupUniqArray", col)

  def groupArrayIf(col1: Column, col2: Column): Column = f("groupArrayIf", col1, col2)

  def arrayMap(lambda: String, cols: Column*): Column = f("arrayMap", Column(lambda) +: cols)

  def arrayZip(col: Column, cols: Column*): Column = f("arrayZip", col +: cols)

  def any(col: Column): Column = f("any", col)

  def lower(col: Column): Column = f("lower", col)

  def upper(col: Column): Column = f("upper", col)

  def sum(col: Column): Column = f("sum", col)

  def array(cols: Seq[Column]): Column = f("array", cols)

  def array(col: Column, cols: Column*): Column = f("array", col +: cols)

  def tuple(cols: Seq[Column]): Column = f("tuple", cols)

  def tuple(col: Column, cols: Column*): Column = f("tuple", col +: cols)

  def tupleElement(tupleName: Column, n: Column): Column = f("tupleElement", tupleName, n)

  def arrayElement(tupleName: Column, n: Column): Column = f("arrayElement", tupleName, n)

  def indexOf(tupleName: Column, n: Column): Column = f("indexOf", tupleName, n)

  def joinGet(tableName: String, fieldName: String, col: Column): Column =
    f("joinGet", literal(tableName), literal(fieldName), col)

  def flatten(col: Column): Column = f("flatten", col)

  def arraySort(col: Column): Column = f("arraySort", col)

  def arrayReverseSort(lambda: Option[String] = None, col: Column): Column = {
    val params = lambda match {
      case Some(lambdaF) => Column(lambdaF) +: col +: Nil
      case None          => col +: Nil
    }
    f("arrayReverseSort", params)
  }

  def replicate(value: Column, array: Column): Column = f("replicate", value, array)

  def arraySlice(col: Column, pos: Int, size: Int): Column =
    f("arraySlice", col, literal(pos), literal(size))

  def length(col: Column): Column = f("length", col)

  def concat(col1: Column, col2: Column, cols: Column*): Column = f("concat", col1 +: col2 +: cols)

  def range(col: Column): Column = f("range", col)

  def range(start: Column, end: Column): Column = f("range", start, end)

  def arrayEnumerate(col: Column): Column = f("arrayEnumerate", col)

  def arraySum(lambda: Option[String], col: Column, cols: Column*): Column = {
    val params = lambda match {
      case Some(lambdaF) => Column(lambdaF) +: col +: cols
      case None          => col +: cols
    }
    f("arraySum", params)
  }

  def arrayJoin(col: Column): Column = f("arrayJoin", col)

  def count(col: Column): Column = f("count", col)

  def toNullable(col: Column): Column = f("toNullable", col)

  def divide(col1: Column, col2: Column): Column = f("divide", col1, col2)

  def multiply(col1: Column, col2: Column): Column = f("multiply", col1, col2)

  def modulo(col1: Column, col2: Column): Column = f("modulo", col1, col2)

  def sqrt(col: Column): Column = f("sqrt", col)

  def plus(col1: Column, col2: Column): Column = f("plus", col1, col2)

  def minus(col1: Column, col2: Column): Column = f("minus", col1, col2)

  def or(col1: Column, col2: Column, cols: Column*): Column = f("or", col1 +: col2 +: cols)

  def and(col1: Column, col2: Column, cols: Column*): Column = f("and", col1 +: col2 +: cols)

  def not(col: Column): Column = f("not", col)

  def in(col1: Column, col2: Column): Column = f("in", col1, col2)

  def notIn(col1: Column, col2: Column): Column = f("notIn", col1, col2)

  def like(col1: Column, col2: Column): Column = f("like", col1, col2)

  def notLike(col1: Column, col2: Column): Column = f("notLike", col1, col2)

  def ifNull(col1: Column, col2: Column): Column = f("ifNull", col1, col2)

  def equals(col1: Column, col2: Column): Column = f("equals", col1, col2)

  def notEquals(col1: Column, col2: Column): Column = f("notEquals", col1, col2)

  def greater(col1: Column, col2: Column): Column = f("greater", col1, col2)

  def less(col1: Column, col2: Column): Column = f("less", col1, col2)

  def greaterOrEquals(col1: Column, col2: Column): Column = f("greaterOrEquals", col1, col2)

  def lessOrEquals(col1: Column, col2: Column): Column = f("lessOrEquals", col1, col2)

  def ifThenElse(expr: Column, ifThen: Column, ifElse: Column): Column =
    f("if", expr, ifThen, ifElse)

  def ngramSearchCaseInsensitive(col1: Column, col2: Column) =
    f("ngramSearchCaseInsensitive", col1, col2)
}
