import com.google.inject.AbstractModule
import services.ApplicationStart

class StartModule extends AbstractModule {
  override def configure(): Unit =
    bind(classOf[ApplicationStart]).asEagerSingleton()
}
