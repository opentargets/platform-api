package models.db

import esecuele._
import esecuele.{Functions => F}
import esecuele.Column._
import esecuele.{Query => Q}
import models.entities.Harmonic
import models.entities.Harmonic.pExponentDefault
import play.api.Logging

// example query
//with (select sumForEach(vector)
//      from ot.ml_w2v
//      where word in ('ENSG00000170396', 'CHEMBL3187365')) as labels,
//    labels as vv,
//
//    sqrt(arraySum(x -> x*x, vv)) as vvnorm,
//    -- if(vvnorm <> .0, arrayMap(x -> x / vvnorm, vv), vv) as scaledvv,
//    sqrt(arraySum(x -> x*x, vector)) as vnorm,
//    if(vvnorm <> 0 and vnorm <> 0, arraySum(x -> x.1 * x.2, arrayZip(vv, vector)) / (vnorm * vvnorm), .0 ) as similarity
//    -- if(vnorm <> .0, arrayMap(x -> x / vnorm, vector), vector) as scaledv
//select
//       category,
//       word,
//       similarity
//from ot.ml_w2v ml
//prewhere category = 'drug'
//where (similarity >= 0.1)
//-- where (similarity >= 0.4) and not(word like 'CHEMBL%' or word like 'ENSG%')
//order by similarity desc
//limit 50;


case class QW2V(tableName: String,
                filterByCategory: Option[String],
                labels: Set[String],
                threshold: Double,
                size: Int)
    extends Queryable
    with Logging {

  require(labels.nonEmpty)

  val cat: Column = column("category")
  val label: Column = column("word")
  val v: Column = column("vector")
  val T: Column = column(tableName)

  val vv: Column = Q(
    Select(F.sumForEach(v) :: Nil),
    From(T),
    PreWhere(F.in(label, F.set(labels.map(literal).toSeq)))
  ).toColumn(None).as(Some("vv"))

  val vvNorm: Column = F.sqrt(F.arraySum(Some("x -> x*x"), vv)).as(Some("vvnorm"))
  val vNorm: Column = F.sqrt(F.arraySum(Some("x -> x*x"), v)).as(Some("vnorm"))

  val sim: Column = F.ifThenElse(
    F.and(F.notEquals(vvNorm.name, literal(0d)), F.notEquals(vNorm.name, literal(0d))),
    F.divide(
      F.arraySum(Some("x -> x.1 * x.2"), F.arrayZip(vv.name, v.name)),
      F.multiply(vNorm.name, vvNorm.name)),
    literal(0d)
  ).as(Some("similarity"))

  val wQ: Option[QuerySection] = Some(With(vv :: vvNorm :: vNorm :: sim :: Nil))
  val sQ: Option[QuerySection] = Some(Select(cat :: label :: sim.name :: Nil))
  val fromQ: Option[QuerySection] = Some(From(T, None))
  val preWhereQ: Option[QuerySection] = filterByCategory.map(c => PreWhere(F.equals(column(c), literal(cat))))
  val whereQ: Option[QuerySection] = Some(Where(F.greaterOrEquals(sim.name, literal(threshold))))
  val orderByQ: Option[QuerySection] = Some(OrderBy(sim.name.desc :: Nil))
  val limitQ: Option[QuerySection] = Some(Limit(0, size))

  override val query: Query = {
    val qElements = wQ :: sQ :: fromQ :: preWhereQ :: whereQ :: orderByQ :: limitQ :: Nil
    val mainQ = Q(qElements.filter(_.isDefined).map(_.get))
    logger.debug(mainQ.toString)

    mainQ
  }
}
