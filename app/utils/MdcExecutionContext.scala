package utils

import org.slf4j.MDC
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import javax.inject.Inject

/** ExecutionContext wrapper that captures the SLF4J MDC map at task-submission time and restores it
  * on the executing thread, ensuring values like `correlation.id` are visible in log statements
  * across async boundaries.
  */
class MdcExecutionContext(underlying: ExecutionContext) extends ExecutionContextExecutor {

  override def execute(runnable: Runnable): Unit = {
    val mdcSnapshot = Option(MDC.getCopyOfContextMap)
    underlying.execute { () =>
      val previous = Option(MDC.getCopyOfContextMap)
      try {
        mdcSnapshot.fold(MDC.clear())(MDC.setContextMap)
        runnable.run()
      } finally previous.fold(MDC.clear())(MDC.setContextMap)
    }
  }

  override def reportFailure(cause: Throwable): Unit = underlying.reportFailure(cause)
}

object MdcExecutionContext {
  def apply(underlying: ExecutionContext): MdcExecutionContext = new MdcExecutionContext(underlying)
}

/** Guice provider that wraps Pekko's default dispatcher so that all injected ExecutionContexts in
  * the application propagate MDC across thread boundaries.
  */
class MdcExecutionContextProvider @Inject() (actorSystem: org.apache.pekko.actor.ActorSystem)
    extends javax.inject.Provider[ExecutionContext] {
  override def get(): ExecutionContext = MdcExecutionContext(actorSystem.dispatcher)
}
