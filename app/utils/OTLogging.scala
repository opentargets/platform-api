package utils

import org.slf4j.{Logger, LoggerFactory}

trait OTLogging {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
