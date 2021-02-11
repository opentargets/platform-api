package models

import java.time.{Duration, LocalDateTime}

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.Inject
import play.api.inject.SimpleModule
import play.api.{Logging, inject}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class CacheModule extends SimpleModule(inject.bind[SearchAfterCache].toSelf.eagerly())

object SearchAfterCache extends Logging {

  private val cache: mutable.Map[String, (Long, String, LocalDateTime)] =
    mutable.Map.empty[String, (Long, String, LocalDateTime)]

  def add(item: (Long, String)): Unit = {
    logger.debug(s"Adding $item to SearchAfterCache")
    cache += (item._2 -> (item._1, item._2, LocalDateTime.now))
  }

  def get(key: String): Option[(Long, String)] = {
    logger.debug(s"Retrieving $key from SearchAfterCache")
    cache.get(key).map(v => (v._1, v._2))
  }

}

class SearchAfterCache @Inject()(actorSystem: ActorSystem, config: Config)(implicit ec: ExecutionContext) extends Logging {
  val frequency: Duration = config.getDuration("ot.elasticsearch.cache.frequency")
  val fd: FiniteDuration = FiniteDuration(frequency.toMillis, "ms")
  logger.info(s"SearchAfterCache cleaning frequency set to $fd.")
  // Scheduled task
  actorSystem.scheduler.scheduleAtFixedRate(initialDelay = fd, interval = fd) { () =>
    logger.info("Clearing SearchAfterCache...")
    clearExpiredCacheEntries()
  }

  def clearExpiredCacheEntries(): Unit = {
    def expiredFilter(cached: (Long, String, LocalDateTime)): Boolean = cached._3.plusMinutes(frequency.toMinutes).isBefore(LocalDateTime.now)

    val expired: Seq[String] = SearchAfterCache.cache.withFilter(v => expiredFilter(v._2)).map(it => it._1).toSeq
    expired.foreach(SearchAfterCache.cache.remove)
    logger.info(
      s"${expired.length} entries removed from SearchAfterCache. Next clean scheduled for ${LocalDateTime.now.plusHours(1).toLocalTime}")
  }
}
