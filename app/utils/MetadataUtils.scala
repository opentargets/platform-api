package utils

import models.entities.Configuration.OTSettings

object MetadataUtils {

  /** Get elasticsearch index name with the data prefix if enabled.
    *
    * @param index
    *   index name
    * @return
    *   index name with data prefix if enabled, otherwise the index name as is.
    */
  def getIndexWithPrefixOrDefault(index: String)(implicit otSettings: OTSettings): String =
    val getMeta = otSettings.meta
    if (getMeta.enableDataReleasePrefix)
      getMeta.dataPrefix + "_" + index
    else
      index
}
