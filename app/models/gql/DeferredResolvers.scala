package models.gql
import models.entities.{Loci, Pagination}
import models.{Backend, entities}
import play.api.Logging
import sangria.execution.deferred.{Deferred, DeferredResolver}
import scala.concurrent._

case class LocusDeferred(studyLocusId: String,
                         variantIds: Option[Seq[String]],
                         pagination: Option[Pagination]
) extends Deferred[Loci]

// TODO: Genericize this resolver to handle not just locus but other deferred types: hint - use the groupDeferred method
class LocusResolver extends DeferredResolver[Backend] with Logging {
  def resolve(deferred: Vector[Deferred[Any]], ctx: Backend, queryState: Any)(implicit
      ec: ExecutionContext
  ): Vector[Future[Any]] = {
    val lq = deferred collect { case q: LocusDeferred => q }
    // group by variantIds and pagination so that we can use studyLocusId to fetch the loci
    val groupedLq = lq.groupBy(q => (q.variantIds, q.pagination))
    val locusQueries = groupedLq.map { case ((variantIds, pagination), queries) =>
      val studyLocusIds = queries.map(_.studyLocusId)
      (studyLocusIds, variantIds, pagination)
    }
    // results grouped by variantIds and pagination
    val results = locusQueries.map { case (studyLocusIds, variantIds, pagination) =>
      val r = ctx.getLocus(studyLocusIds, variantIds, pagination)
      (variantIds, pagination) -> r
    }.toMap
    deferred.map { case LocusDeferred(studyLocusId, variantIds, pagination) =>
      // lookup results based on the variantIds pagination group in the results
      val group = results.get((variantIds, pagination)).get
      val l = group.map(loci => loci.filter(studyLocusId == _.studyLocusId))
      l.map(_.headOption.getOrElse(Loci.empty()))
    }
  }
}

object DeferredResolvers extends Logging {
  val locusResolver = new LocusResolver()
  // add fetchers and locusResolver to the resolvers
  val deferredResolvers: DeferredResolver[Backend] = DeferredResolver.fetchersWithFallback(
    locusResolver,
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
