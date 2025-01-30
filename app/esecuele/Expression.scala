package esecuele

trait Rep {
  val rep: String
}

abstract class Expression extends Rep {
  val raw: String
}

case class RawExpression(raw: String) extends Expression {
  override val rep: String = raw

  override def toString: String = rep
}

case object EmptyExpression extends Expression {
  override val raw: String = ""
  override val rep: String = ""

  override def toString: String = rep
}
