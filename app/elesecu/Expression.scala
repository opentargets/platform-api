package elesecu

trait Expression
case class RawExpression(raw: String) extends Expression {
  override def toString: String = raw
}
case object EmptyExpression extends Expression
