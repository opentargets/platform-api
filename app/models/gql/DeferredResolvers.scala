package models.gql
import models.entities.{Loci, Pagination, CredibleSets, CredibleSetQueryArgs}
import models.{Backend, entities}
import play.api.Logging
import sangria.execution.deferred.{Deferred, DeferredResolver}
import scala.concurrent._
import models.gql.Arguments.studyId
import models.gql.Objects.locationAndSourceImp


trait TypeWithId {
  val id: String
}


/** @param id The ID to resolve on
  * @param grouping A tuple of the values that are used to group the deferred values
  * @tparam T The type of the deferred value
  */
abstract class DeferredMultiTerm[+T]() extends Deferred[T] {
  val id: String
  val grouping: Product  
  def empty(): T
  def resolver(ctx: Backend): (Seq[String], Product) => Future[IndexedSeq[T]]
}

case class LocusDeferred(studyLocusId: String,
                         variantIds: Option[Seq[String]],
                         pagination: Option[Pagination]
                         ) extends DeferredMultiTerm[Loci] {
  val id: String = studyLocusId
  val grouping = (variantIds, pagination)
  def empty(): Loci = Loci.empty()
  def resolver(ctx: Backend): (Seq[String], Product) => Future[IndexedSeq[Loci]] = {
    case (s: Seq[String], options: Product) =>
      options match {
        case (v, p) =>
          ctx.getLocus(s, v.asInstanceOf[Option[Seq[String]]], p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class CredibleSetsByStudyDeferred(studyId: String, pagination: Option[Pagination]) extends DeferredMultiTerm[CredibleSets] {
  val id: String = studyId
  val grouping = (pagination)
  def empty(): CredibleSets = CredibleSets.empty
  def resolver(ctx: Backend): (Seq[String], Product) => Future[IndexedSeq[CredibleSets]] = {
    case (s: Seq[String], options: Product) =>
      options match {
        case (p) => {
            ctx.getCredibleSetsByStudy(s, p.asInstanceOf[Option[Pagination]])
        }
      }
  }
}


/** A deferred resolver for cases where we can't use the Fetch API because we resolve the
  * values on multiple terms/filters.
  */
class MultiTermResolver extends DeferredResolver[Backend] with Logging {
  def groupResults[T](deferred: Vector[DeferredMultiTerm[T]],
                      ctx: Backend
  ): Map[Product, Future[IndexedSeq[T]]] = {
    val grouped = deferred.groupBy(q => q.grouping)
    val queries = grouped.map { case (grouping, queries) =>
      val ids = queries.map(_.id)
      val resolver = queries.head.resolver(ctx)
      (ids, grouping, resolver)
    }
    val results = queries.map { case (ids, grouping, resolver) =>
      val r = resolver(ids, grouping)
      grouping -> r
    }.toMap
    results
  }

  def getResultForId[T](deferredQ: DeferredMultiTerm[T],
                        results: Map[Product, Future[IndexedSeq[TypeWithId]]]
  )(implicit ec: ExecutionContext): Future[T] = {
    val group = results.get(deferredQ.grouping).get
    val hit = group.map(_.filter(_.id == deferredQ.id))
    hit.map(_.headOption.getOrElse(deferredQ.empty()).asInstanceOf[T])
  }

  def resolve(deferred: Vector[Deferred[Any]], ctx: Backend, queryState: Any)(implicit
      ec: ExecutionContext
  ): Vector[Future[Any]] = {
    val deferredByType = deferred collect { 
        case locus: LocusDeferred => locus
        case credSetByStudy: CredibleSetsByStudyDeferred => credSetByStudy
    }
    val results = groupResults(deferredByType, ctx)
    deferred.map { 
        case locus: LocusDeferred => getResultForId(locus, results)
        case credSetByStudy: CredibleSetsByStudyDeferred => getResultForId(credSetByStudy, results)
    }
  }
}

object DeferredResolvers extends Logging {
  val multiTermResolver = new MultiTermResolver()
  // add fetchers and locusResolver to the resolvers
  val deferredResolvers: DeferredResolver[Backend] = DeferredResolver.fetchersWithFallback(
    multiTermResolver,
    Fetchers.biosamplesFetcher,
    Fetchers.credibleSetFetcher,
    Fetchers.l2gFetcher,
    Fetchers.targetsFetcher,
    Fetchers.drugsFetcher,
    Fetchers.diseasesFetcher,
    Fetchers.hposFetcher,
    Fetchers.reactomeFetcher,
    Fetchers.expressionFetcher,
    Fetchers.otarProjectsFetcher,
    Fetchers.soTermsFetcher,
    Fetchers.indicationFetcher,
    Fetchers.goFetcher,
    Fetchers.variantFetcher,
    Fetchers.studyFetcher
  )
}