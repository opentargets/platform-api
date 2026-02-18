package models.gql
import models.entities.{
  AdverseEvents,
  Colocalisations,
  CredibleSetQueryArgs,
  CredibleSets,
  L2GPredictions,
  Loci,
  Pagination,
  Interactions,
  DiseaseHPOs,
  ProteinCodingCoordinates
}
import models.{Backend, entities}
import sangria.execution.deferred.{Deferred, DeferredResolver}

import scala.concurrent.*
import utils.OTLogging

trait TypeWithId {
  val id: String
}

enum DeferredType {
  case AdverseEvents
  case Loci
  case CredibleSets
  case Colocalisations
  case L2GPredictions
  case DiseaseHPOs
  case Interactions
  case ProteinCodingCoordinates
}

case class Grouping(label: DeferredType, options: Product)

/** @param id
  *   The ID to resolve on
  * @param grouping
  *   The grouping of the deferred value
  * @tparam T
  *   The type of the deferred value
  */
abstract class DeferredMultiTerm[+T]() extends Deferred[T] {
  val id: String
  val grouping: Grouping
  def empty(): T
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[T]]
}

case class AdverseEventsDeferred(chemblId: String, pagination: Option[Pagination])
    extends DeferredMultiTerm[AdverseEvents] {
  val id: String = chemblId
  val options = (pagination)
  val grouping = Grouping(DeferredType.AdverseEvents, options)
  def empty(): AdverseEvents = AdverseEvents(0, 0.0, Seq.empty)
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[AdverseEvents]] = {
    case (c: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (p) =>
          ctx.getAdverseEvents(c, p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class ProteinCodingCoordinatesByTargetDeferred(targetId: String,
                                                    pagination: Option[Pagination]
) extends DeferredMultiTerm[ProteinCodingCoordinates] {
  val id: String = targetId
  val options = (pagination)
  val grouping = Grouping(DeferredType.ProteinCodingCoordinates, options)

  def empty(): ProteinCodingCoordinates = ProteinCodingCoordinates.empty()
  def resolver(
      ctx: Backend
  ): (Seq[String], Grouping) => Future[IndexedSeq[ProteinCodingCoordinates]] = {
    case (t: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (p) =>
          ctx.getProteinCodingCoordinatesByTarget(t, p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class ProteinCodingCoordinatesByVariantDeferred(variantId: String,
                                                     pagination: Option[Pagination]
) extends DeferredMultiTerm[ProteinCodingCoordinates] {
  val id: String = variantId
  val options = (pagination)
  val grouping = Grouping(DeferredType.ProteinCodingCoordinates, options)

  def empty(): ProteinCodingCoordinates = ProteinCodingCoordinates.empty()
  def resolver(
      ctx: Backend
  ): (Seq[String], Grouping) => Future[IndexedSeq[ProteinCodingCoordinates]] = {
    case (v: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (p) =>
          ctx.getProteinCodingCoordinatesByVariant(v, p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class DiseaseHPOsDeferred(diseaseId: String, pagination: Option[Pagination])
    extends DeferredMultiTerm[DiseaseHPOs] {
  val id: String = diseaseId
  val options = (pagination)
  val grouping = Grouping(DeferredType.DiseaseHPOs, options)

  def empty(): DiseaseHPOs = DiseaseHPOs.empty
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[DiseaseHPOs]] = {
    case (d: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (p) =>
          ctx.getDiseaseHPOs(d, p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class InteractionsDeferred(targetId: String,
                                scoreThreshold: Option[Double],
                                databaseName: Option[InteractionSourceEnum.Value],
                                pagination: Option[Pagination]
) extends DeferredMultiTerm[Interactions] {
  val id: String = targetId
  val options = (scoreThreshold, databaseName, pagination)
  val grouping = Grouping(DeferredType.Loci, options)
  def empty(): Interactions = Interactions.empty
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[Interactions]] = {
    case (s: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (st, db, p) =>
          ctx.getInteractions(s,
                              st.asInstanceOf[Option[Double]],
                              db.asInstanceOf[Option[InteractionSourceEnum.Value]],
                              p.asInstanceOf[Option[Pagination]]
          )
      }
  }
}

case class LocusDeferred(studyLocusId: String,
                         variantIds: Option[Seq[String]],
                         pagination: Option[Pagination]
) extends DeferredMultiTerm[Loci] {
  val id: String = studyLocusId
  val options = (variantIds, pagination)
  val grouping = Grouping(DeferredType.Loci, options)
  def empty(): Loci = Loci.empty()
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[Loci]] = {
    case (s: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (v, p) =>
          ctx.getLocus(s, v.asInstanceOf[Option[Seq[String]]], p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class CredibleSetsByStudyDeferred(studyId: String, pagination: Option[Pagination])
    extends DeferredMultiTerm[CredibleSets] {
  val id: String = studyId
  val options = (pagination)
  val grouping = Grouping(DeferredType.CredibleSets, options)

  def empty(): CredibleSets = CredibleSets.empty
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[CredibleSets]] = {
    case (s: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (p) =>
          ctx.getCredibleSetsByStudy(s, p.asInstanceOf[Option[Pagination]])
      }
  }
}

case class CredibleSetsByVariantDeferred(variantId: String,
                                         studyTypes: Option[Seq[StudyTypeEnum.Value]],
                                         pagination: Option[Pagination]
) extends DeferredMultiTerm[CredibleSets] {
  val id: String = variantId
  val options = (studyTypes, pagination)
  val grouping = Grouping(DeferredType.CredibleSets, options)

  def empty(): CredibleSets = CredibleSets.empty
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[CredibleSets]] = {
    case (v: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (s, p) =>
          ctx.getCredibleSetsByVariant(v,
                                       s.asInstanceOf[Option[Seq[StudyTypeEnum.Value]]],
                                       p.asInstanceOf[Option[Pagination]]
          )
      }
  }
}

case class ColocalisationsDeferred(studyLocusId: String,
                                   studyTypes: Seq[StudyTypeEnum.Value] = Seq(StudyTypeEnum.gwas),
                                   pagination: Option[Pagination]
) extends DeferredMultiTerm[Colocalisations] {
  val id: String = studyLocusId
  val options = (studyTypes, pagination)
  val grouping = Grouping(DeferredType.Colocalisations, options)
  def empty(): Colocalisations = Colocalisations.empty
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[Colocalisations]] = {
    case (s: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (st, p) =>
          ctx.getColocalisations(s,
                                 st.asInstanceOf[Seq[StudyTypeEnum.Value]],
                                 p.asInstanceOf[Option[Pagination]]
          )
      }
  }
}

case class L2GPredictionsDeferred(studyLocusId: String, pagination: Option[Pagination])
    extends DeferredMultiTerm[L2GPredictions] {
  val id: String = studyLocusId
  val options = (pagination)
  val grouping = Grouping(DeferredType.L2GPredictions, options)
  def empty(): L2GPredictions = L2GPredictions.empty
  def resolver(ctx: Backend): (Seq[String], Grouping) => Future[IndexedSeq[L2GPredictions]] = {
    case (s: Seq[String], grouping: Grouping) =>
      grouping.options match {
        case (p) =>
          ctx.getL2GPredictions(s, p.asInstanceOf[Option[Pagination]])
      }
  }
}

/** A deferred resolver for cases where we can't use the Fetch API because we resolve the values on
  * multiple terms/filters.
  */
class MultiTermResolver extends DeferredResolver[Backend] with OTLogging {

  def groupResults[T](deferred: Vector[DeferredMultiTerm[T]],
                      ctx: Backend
  ): Map[Grouping, Future[IndexedSeq[T]]] = {
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
                        results: Map[Grouping, Future[IndexedSeq[TypeWithId]]]
  )(implicit ec: ExecutionContext): Future[T] = {
    val group = results.get(deferredQ.grouping).get
    val hit = group.map(_.filter(_.id == deferredQ.id))
    hit.map(_.headOption.getOrElse(deferredQ.empty()).asInstanceOf[T])
  }

  def resolve(deferred: Vector[Deferred[Any]], ctx: Backend, queryState: Any)(implicit
      ec: ExecutionContext
  ): Vector[Future[Any]] = {
    val deferredByType = deferred collect {
      case adverseEvents: AdverseEventsDeferred            => adverseEvents
      case locus: LocusDeferred                            => locus
      case credSetByStudy: CredibleSetsByStudyDeferred     => credSetByStudy
      case credSetByVariant: CredibleSetsByVariantDeferred => credSetByVariant
      case colocalisations: ColocalisationsDeferred        => colocalisations
      case diseaseHpos: DiseaseHPOsDeferred                => diseaseHpos
      case interactions: InteractionsDeferred              => interactions
      case l2g: L2GPredictionsDeferred                     => l2g
      case proteinCodingCoordinatesByTarget: ProteinCodingCoordinatesByTargetDeferred =>
        proteinCodingCoordinatesByTarget
      case proteinCodingCoordinatesByVariant: ProteinCodingCoordinatesByVariantDeferred =>
        proteinCodingCoordinatesByVariant
    }
    val results = groupResults(deferredByType, ctx)
    deferred.map {
      case adverseEvents: AdverseEventsDeferred        => getResultForId(adverseEvents, results)
      case locus: LocusDeferred                        => getResultForId(locus, results)
      case credSetByStudy: CredibleSetsByStudyDeferred => getResultForId(credSetByStudy, results)
      case credSetByVariant: CredibleSetsByVariantDeferred =>
        getResultForId(credSetByVariant, results)
      case colocalisations: ColocalisationsDeferred => getResultForId(colocalisations, results)
      case diseaseHpos: DiseaseHPOsDeferred         => getResultForId(diseaseHpos, results)
      case interactions: InteractionsDeferred       => getResultForId(interactions, results)
      case l2g: L2GPredictionsDeferred              => getResultForId(l2g, results)
      case proteinCodingCoordinatesByTarget: ProteinCodingCoordinatesByTargetDeferred =>
        getResultForId(proteinCodingCoordinatesByTarget, results)
      case proteinCodingCoordinatesByVariant: ProteinCodingCoordinatesByVariantDeferred =>
        getResultForId(proteinCodingCoordinatesByVariant, results)
    }
  }
}

object DeferredResolvers extends OTLogging {

  val multiTermResolver = new MultiTermResolver()
  // add fetchers and locusResolver to the resolvers
  val deferredResolvers: DeferredResolver[Backend] = DeferredResolver.fetchersWithFallback(
    multiTermResolver,
    Fetchers.biosamplesFetcher,
    Fetchers.credibleSetFetcher,
    Fetchers.targetsFetcher,
    Fetchers.drugsFetcher,
    Fetchers.diseasesFetcher,
    Fetchers.hposFetcher,
    Fetchers.expressionFetcher,
    Fetchers.mechanismsOfActionFetcher,
    Fetchers.mousePhenotypesFetcher,
    Fetchers.otarProjectsFetcher,
    Fetchers.pharmacogenomicsByDrugFetcher,
    Fetchers.pharmacogenomicsByVariantFetcher,
    Fetchers.pharmacogenomicsByTargetFetcher,
    Fetchers.soTermsFetcher,
    Fetchers.indicationFetcher,
    Fetchers.goFetcher,
    Fetchers.variantFetcher,
    Fetchers.studyFetcher,
    Fetchers.targetEssentialityFetcher,
    Fetchers.targetPrioritisationFetcher
  )
}
