import com.google.inject.AbstractModule
import models.gql.SizedAsyncCache
import play.api.cache.AsyncCacheApi
import services.ApplicationStart

class StartModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
    bind(classOf[AsyncCacheApi]).to(classOf[SizedAsyncCache])
  }
}
