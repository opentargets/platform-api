package utils.logging

import net.logstash.logback.marker.{LogstashMarker, ObjectAppendingMarker}
import play.api.MarkerContext

object LogUtils {

  extension (mc: MarkerContext)
    // Appends the provided SLF4J markers to the existing MarkerContext, ensuring that all markers are included in the log output.
    def fromExistingContext(extraNames: LogstashMarker): MarkerContext = {
      // Get or create the root SLF4J marker
      val markers = mc.marker match
        case Some(marker) => extraNames.and(marker)
        case None         => extraNames

      markers
    }
}
