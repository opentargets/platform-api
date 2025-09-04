package clickhouse.rep

import scala.language.implicitConversions
import scala.reflect.ClassTag

/** Clickhouse supports Array of elements from different types and this is an approximation to it.
  *
  * @param from
  * @tparam T
  */
sealed abstract class SeqRep[T, C[_]](val from: String) {
  protected val minLenTokensForStr = 4
  protected val minLenTokensForNum = 2
  lazy val rep: SeqT = parse(from)
  type SeqT = C[T]
  protected def parse(from: String): SeqT
}

object SeqRep {
  // TODO: might need to be deleted in next release 25.x.x. This is no longer needed due to update in the driver
  def parseFastString(str: String): String = str.slice(1, str.length - 1)

  sealed abstract class NumSeqRep[T](override val from: String, val f: String => T)(implicit
      val ct: ClassTag[T]
  ) extends SeqRep[T, Vector](from) {
    override protected def parse(from: String): SeqT =
      if (from.nonEmpty) {
        from.length match {
          case n if n > minLenTokensForNum =>
            from.slice(1, n - 1).split(",").map(f(_)).toVector
          case _ => Vector.empty
        }
      } else
        Vector.empty
  }

  case class DSeqRep(override val from: String) extends NumSeqRep[Double](from, _.toDouble)
  case class ISeqRep(override val from: String) extends NumSeqRep[Int](from, _.toInt)
  case class LSeqRep(override val from: String) extends NumSeqRep[Long](from, _.toLong)

  case class StrSeqRep(override val from: String) extends SeqRep[String, Vector](from) {
    override protected def parse(from: String): SeqT =
      if (from.nonEmpty) {
        from.length match {
          case n if n > minLenTokensForStr =>
            from.slice(1, n - 1).split(",").map(t => t.slice(1, t.length - 1)).toVector
          case _ => Vector.empty
        }
      } else
        Vector.empty
  }

  case class TupleSeqRep[T](override val from: String, val f: String => T)(implicit
      val ct: ClassTag[T]
  ) extends SeqRep[T, Vector](from) {
    override protected def parse(from: String): SeqT =
      if (from.nonEmpty) {
        from.length match {
          case n if n > minLenTokensForStr =>
            val offset: Int = minLenTokensForStr / 2
            from.slice(offset, n - offset).split("\\], \\[").map(f(_)).toVector
          case _ => Vector.empty
        }
      } else
        Vector.empty
  }

  object Implicits {
    implicit def seqInt(from: ISeqRep): Vector[Int] = from.rep
    implicit def seqLong(from: LSeqRep): Vector[Long] = from.rep
    implicit def seqDouble(from: DSeqRep): Vector[Double] = from.rep
    implicit def seqStr(from: StrSeqRep): Vector[String] = from.rep
  }
}
