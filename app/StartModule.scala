import com.google.inject.AbstractModule
import services.ApplicationStart

class StartModule extends AbstractModule {
  override def configure(): Unit = {
    // Bind your services here
     bind(classOf[ApplicationStart]).asEagerSingleton()
  }
}
