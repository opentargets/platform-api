package models

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import play.api.{Application, Logging}

class SearchAfterCacheTest extends PlaySpec with GuiceOneAppPerSuite with Injecting with Logging {

  lazy val searchAfterCache: SearchAfterCache = inject[SearchAfterCache]
  val cacheClearFrequency = "2s"
  val configPath = "ot.elasticsearch.cache.frequency"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(Map(configPath -> cacheClearFrequency))
      .build()

  "The SearchAfterCache" must {
    "automatically clean itself periodically" in {
      // given
      val input = (1L, "asdf")
      SearchAfterCache.add(input)
      // when
      val isPresent = SearchAfterCache.get(input._2).isDefined
      Thread.sleep(app.configuration.getMillis(configPath) + 100)
      val isCleared = SearchAfterCache.get(input._2).isEmpty
      // then
      assertResult(
        true,
        "Item should be immediately present, and then cleared after cache clear frequency.")(
        isPresent && isCleared)

    }
  }

}
