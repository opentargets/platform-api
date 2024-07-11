package models

import scala.concurrent.{Future, ExecutionContext}
import com.sksamuel.elastic4s.{ElasticClient, Response}
import play.api.Logging
import java.net.ConnectException
import com.sksamuel.elastic4s.http.JavaClientExceptionWrapper

class ElasticClientWrapper(client: ElasticClient)(implicit ec: ExecutionContext) extends Logging {
  private def retry[T](op: => Future[Response[T]], attempts: Int): Future[Response[T]] =
    op.recoverWith {
      case ex: JavaClientExceptionWrapper
          if attempts >= 1 && ex.getCause.isInstanceOf[java.net.ConnectException] =>
        logger.error(ex.getCause().toString())
        logger.error(s"Failed to execute request. Retrying... Attempts left: $attempts")
        retry(op, attempts - 1)
    }

  def execute[T](request: => Future[Response[T]], attempts: Int = 3): Future[Response[T]] =
    retry(request, attempts)
}
