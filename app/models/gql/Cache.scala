package models.gql

import sangria.execution.deferred.FetcherCache
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import org.apache.pekko.Done
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import javax.inject.Inject

// ---------------------------------------------------------------------------
// SizedCache — Sangria FetcherCache backed by two memory-bounded Caffeine caches
// (entity lookups + relationship lookups)
// ---------------------------------------------------------------------------

/** @param maxBytes Maximum combined weight in bytes for each internal cache. */
class SizedCache(maxBytes: Long = 256L * 1024 * 1024) extends FetcherCache {

  private def estimateBytes(key: Any, value: Any): Int = {
    val size = (key.toString.length + value.toString.length) * 2
    math.max(1, math.min(size, Int.MaxValue)).toInt
  }

  private val cache: Cache[Any, Any] = Caffeine
    .newBuilder()
    .maximumWeight(maxBytes)
    .weigher[Any, Any]((k, v) => estimateBytes(k, v))
    .build[Any, Any]()

  private val relCache: Cache[Any, Seq[Any]] = Caffeine
    .newBuilder()
    .maximumWeight(maxBytes)
    .weigher[Any, Seq[Any]]((k, v) => estimateBytes(k, v))
    .build[Any, Seq[Any]]()

  def cacheKey(id: Any): Any = id

  def cacheKeyRel(rel: Any, relId: Any): Any = rel -> relId

  def cacheable(id: Any): Boolean = true

  def cacheableRel(rel: Any, relId: Any): Boolean = true

  def get(id: Any): Option[Any] =
    Option(cache.getIfPresent(cacheKey(id)))

  def getRel(rel: Any, relId: Any): Option[Seq[Any]] =
    Option(relCache.getIfPresent(cacheKeyRel(rel, relId)))

  def update(id: Any, value: Any): Unit =
    if (cacheable(id)) cache.put(cacheKey(id), value)

  def updateRel[T](rel: Any, relId: Any, idFn: T => Any, values: Seq[T]): Unit =
    if (cacheableRel(rel, relId)) {
      values.foreach(v => update(idFn(v), v))
      relCache.put(cacheKeyRel(rel, relId), values)
    }

  def clear(): Unit = {
    cache.invalidateAll()
    relCache.invalidateAll()
  }

  override def clearId(id: Any): Unit =
    cache.invalidate(cacheKey(id))

  override def clearRel(rel: Any): Unit = {
    val keysToRemove = relCache.asMap().keySet().asScala.filter {
      case (r, _) => r == rel
      case _      => false
    }
    relCache.invalidateAll(keysToRemove.asJava)
  }

  override def clearRelId(rel: Any, relId: Any): Unit =
    relCache.invalidate(cacheKeyRel(rel, relId))

  def stats(): String = {
    val cacheStats = cache.stats()
    val relCacheStats = relCache.stats()
    CacheStats(
      entityCacheSize = cache.estimatedSize(),
      relCacheSize = relCache.estimatedSize(),
      entityHitRate = cacheStats.hitRate(),
      relHitRate = relCacheStats.hitRate(),
      entityEvictionCount = cacheStats.evictionCount(),
      relEvictionCount = relCacheStats.evictionCount()
    ).toString
  }
}

object SizedCache {
  def apply(): SizedCache = new SizedCache()
  def apply(maxBytes: Long): SizedCache = new SizedCache(maxBytes)
}

// ---------------------------------------------------------------------------
// SizedAsyncCache — Play AsyncCacheApi backed by a single memory-bounded
// Caffeine cache, used for full GraphQL query response caching
// ---------------------------------------------------------------------------

/** @param maxBytes Maximum weight in bytes before Caffeine evicts entries. */
class SizedAsyncCache @Inject() (config: Configuration) extends AsyncCacheApi {

  private val maxBytes = config.get[Long]("ot.cache.queryMaxMb") * 1024 * 1024
  private implicit val ec: ExecutionContext = ExecutionContext.parasitic

  private val cache: Cache[String, Any] = Caffeine
    .newBuilder()
    .maximumWeight(maxBytes)
    .weigher[String, Any] { (k, v) =>
      val size = (k.length + v.toString.length) * 2
      math.max(1, math.min(size, Int.MaxValue)).toInt
    }
    .build[String, Any]()

  def get[T: ClassTag](key: String): Future[Option[T]] =
    Future.successful(Option(cache.getIfPresent(key)).flatMap {
      case v: T => Some(v)
      case _    => None
    })

  def set(key: String, value: Any, expiration: Duration = Duration.Inf): Future[Done] = {
    cache.put(key, value)
    Future.successful(Done)
  }

  def remove(key: String): Future[Done] = {
    cache.invalidate(key)
    Future.successful(Done)
  }

  def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(
      orElse: => Future[A]
  ): Future[A] =
    get[A](key).flatMap {
      case Some(v) => Future.successful(v)
      case None =>
        orElse.map { v =>
          cache.put(key, v)
          v
        }
    }

  def removeAll(): Future[Done] = {
    cache.invalidateAll()
    Future.successful(Done)
  }
}

// ---------------------------------------------------------------------------
// Shared stats model
// ---------------------------------------------------------------------------

case class CacheStats(
    entityCacheSize: Long,
    relCacheSize: Long,
    entityHitRate: Double,
    relHitRate: Double,
    entityEvictionCount: Long,
    relEvictionCount: Long
)
