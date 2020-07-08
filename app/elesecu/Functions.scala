package elesecu

import elesecu.Column.{column, literal}

object Functions {
  private def f(name: String, cols: Seq[Column]): Column = column(cols.map(_.name).mkString(s"$name(", ",", ")"))
  private def f(name: String, col: Column, cols: Column*): Column = f(name, col +: cols)

  def apply(name: String, cols: Seq[Column]): Column = f(name, cols)
  def apply(name: String, col: Column, cols: Column*): Column = apply(name, col +:cols)

  def uniq(cols: Seq[Column]): Column = f("uniq", cols)
  def uniq(col: Column, cols: Column*): Column = uniq(col +: cols)
  def distinct(cols: Seq[Column]): Column = f("distinct", cols)
  def distinct(col: Column, cols: Column*): Column = distinct(col +: cols)
  def groupArray(col: Column): Column = f("groupArray", col)
  def groupArrayIf(col1: Column, col2: Column): Column = f("groupArrayIf", col1, col2)
  def arrayMap(lambda: String, cols: Column*): Column = f("arrayMap", Column(lambda) +: cols)
  def any(col: Column): Column = f("any", col)
  def array(cols: Seq[Column]): Column = f("array", cols)
  def array(col: Column, cols: Column*): Column = f("array", col +: cols)
  def joinGet(tableName: String, fieldName: String, col: Column): Column =
    f("joinGet", literal(tableName), literal(fieldName), col)
  def flatten(col: Column): Column = f("flatten", col)
  def arraySort(col: Column): Column = f("arraySort", col)
  def arrayReverseSort(col: Column): Column = f("arrayReverseSort", col)
  def arraySlice(col: Column, pos: Int, size: Int): Column =
    f("arraySlice", col, literal(pos), literal(size))
  def length(col: Column): Column = f("length", col)
  def range(col: Column): Column = f("range", col)
  def arraySum(lambda: String, col1: Column, col2: Column): Column =
    f("arraySum", Column(lambda), col1, col2)

  def divide(col1: Column, col2: Column): Column = f("divide", col1, col2)
  def multiply(col1: Column, col2: Column): Column = f("multiply", col1, col2)
  def modulo(col1: Column, col2: Column): Column = f("modulo", col1, col2)
  def plus(col1: Column, col2: Column): Column = f("plus", col1, col2)
  def minus(col1: Column, col2: Column): Column = f("minus", col1, col2)

  def or(col1: Column, col2: Column): Column = f("or", col1, col2)
  def and(col1: Column, col2: Column): Column = f("and", col1, col2)
  def not(col: Column): Column = f("not", col)
  def in(col1: Column, col2: Column): Column = f("in", col1, col2)
  def notIn(col1: Column, col2: Column): Column = f("notIn", col1, col2)
  def like(col1: Column, col2: Column): Column = f("like", col1, col2)
  def notLike(col1: Column, col2: Column): Column = f("notLike", col1, col2)

  def equals(col1: Column, col2: Column): Column = f("equals", col1, col2)
  def notEquals(col1: Column, col2: Column): Column = f("notEquals", col1, col2)
  def greater(col1: Column, col2: Column): Column = f("greater", col1, col2)
  def less(col1: Column, col2: Column): Column = f("less", col1, col2)
  def greaterOrEquals(col1: Column, col2: Column): Column = f("greaterOrEquals", col1, col2)
  def lessOrEquals(col1: Column, col2: Column): Column = f("lessOrEquals", col1, col2)
}
