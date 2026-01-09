package models.db

import esecuele.*
import esecuele.Functions as F
import esecuele.Column.*
import esecuele.Query as Q
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.{Logger, LoggerFactory}
import play.api.Logging

case class QW2V(
    tableName: String,
    categories: List[String],
    labels: Set[String],
    threshold: Double,
    size: Int
) extends Queryable {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  require(labels.nonEmpty)

  val cat: Column = column("category")
  val label: Column = column("word")
  val v: Column = column("vector")
  val norm: Column = column("norm")
  val T: Column = column(tableName)

  val vv: Column = Q(
    Select(F.sumForEach(v) :: Nil),
    From(T),
    PreWhere(F.in(label, F.set(labels.map(literal).toSeq)))
  ).toColumn(None).as(Some("vv"))

  val vvNorm: Column = F.sqrt(F.arraySum(Some("x -> x*x"), vv)).as(Some("vvnorm"))

  val sim: Column = F
    .ifThenElse(
      F.and(F.notEquals(vvNorm.name, literal(0d)), F.notEquals(norm, literal(0d))),
      F.divide(
        F.arraySum(Some("x -> x.1 * x.2"), F.arrayZip(vv.name, v)),
        F.multiply(norm, vvNorm.name)
      ),
      literal(0d)
    )
    .as(Some("similarity"))

  val wQ: Option[QuerySection] = Some(With(vv :: vvNorm :: sim :: Nil))
  val sQ: Option[QuerySection] = Some(Select(cat :: label :: sim.name :: Nil))
  val fromQ: Option[QuerySection] = Some(From(T, None))
  val preWhereQ: Option[QuerySection] = categories match {
    case Nil => None
    case _ =>
      val cats = F.set(categories.map(literal))
      Some(PreWhere(F.in(cat, cats)))
  }

  val whereQ: Option[QuerySection] = Some(Where(F.greaterOrEquals(sim.name, literal(threshold))))
  val orderByQ: Option[QuerySection] = Some(OrderBy(sim.name.desc :: Nil))
  val limitQ: Option[QuerySection] = Some(Limit(0, size))

  def existsLabel(id: String): Query = {
    val qElements =
      Select(F.count(star) :: Nil) :: fromQ.get :: PreWhere(F.equals(label, literal(id))) :: Nil
    val mainQ: Query = Q(qElements)
    logger.debug(mainQ.toString,
                 keyValue("query_name", "existsLabel"),
                 keyValue("query_type", this.getClass.getName)
    )

    mainQ
  }

  override val query: Query = {
    val qElements = wQ :: sQ :: fromQ :: preWhereQ :: whereQ :: orderByQ :: limitQ :: Nil
    val mainQ = Q(qElements.filter(_.isDefined).map(_.get))
    logger.debug(mainQ.toString,
                 keyValue("query_name", "query"),
                 keyValue("query_type", this.getClass.getName)
    )

    mainQ
  }
}
