import com.google.inject.AbstractModule
import scala.concurrent.ExecutionContext
import services.ApplicationStart
import utils.MdcExecutionContextProvider

class StartModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
    bind(classOf[ExecutionContext]).toProvider(classOf[MdcExecutionContextProvider])
  }
}
