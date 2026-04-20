package models.gql

import sangria.execution.deferred.FetcherCache
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import scala.jdk.CollectionConverters._

/** LRU (Least Recently Used) cache implementation for FetcherCache using Caffeine.
  *
  * @param maxBytes
  *   Maximum total memory in bytes for each cache before eviction occurs
  */
class LruFetcherCache(maxBytes: Long = 256L * 1024 * 1024) extends FetcherCache {

  private def estimateBytes(key: Any, value: Any): Int = {
    val size = (key.toString.length + value.toString.length) * 2
    math.max(1, math.min(size, Int.MaxValue)).toInt
  }

  // Primary cache for entity lookups by ID
  private val cache: Cache[Any, Any] = Caffeine
    .newBuilder()
    .maximumWeight(maxBytes)
    .weigher[Any, Any]((k, v) => estimateBytes(k, v))
    .build[Any, Any]()

  // Cache for relationship lookups
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
    if (cacheable(id)) {
      cache.put(cacheKey(id), value)
    }

  def updateRel[T](rel: Any, relId: Any, idFn: T => Any, values: Seq[T]): Unit =
    if (cacheableRel(rel, relId)) {
      values.foreach { v =>
        update(idFn(v), v)
      }
      relCache.put(cacheKeyRel(rel, relId), values)
    }

  def clear(): Unit = {
    cache.invalidateAll()
    relCache.invalidateAll()
  }

  override def clearId(id: Any): Unit =
    cache.invalidate(cacheKey(id))

  override def clearRel(rel: Any): Unit = {
    // Iterate through all keys and remove matching relationships
    val keysToRemove = relCache.asMap().keySet().asScala.filter {
      case (r, _) => r == rel
      case _      => false
    }
    relCache.invalidateAll(keysToRemove.asJava)
  }

  override def clearRelId(rel: Any, relId: Any): Unit =
    relCache.invalidate(cacheKeyRel(rel, relId))

  /** Get current cache statistics
    */
  def stats() = {
    val cacheStats = cache.stats()
    val relCacheStats = relCache.stats()
    CacheStats(
      entityCacheSize = cache.estimatedSize(),
      relCacheSize = relCache.estimatedSize(),
      entityHitRate = cacheStats.hitRate(),
      relHitRate = relCacheStats.hitRate(),
      entityEvictionCount = cacheStats.evictionCount(),
      relEvictionCount = relCacheStats.evictionCount()
    )
    cacheStats.toString()
  }
}

case class CacheStats(
    entityCacheSize: Long,
    relCacheSize: Long,
    entityHitRate: Double,
    relHitRate: Double,
    entityEvictionCount: Long,
    relEvictionCount: Long
)

object LruFetcherCache {

  /** Create an LRU cache with default memory limit (256 MB per cache)
    */
  def apply(): LruFetcherCache = new LruFetcherCache()

  /** Create an LRU cache with a custom memory limit in bytes
    */
  def apply(maxBytes: Long): LruFetcherCache = new LruFetcherCache(maxBytes)
}
