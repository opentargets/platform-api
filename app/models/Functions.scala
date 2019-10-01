package models

import better.files._
import com.typesafe.config.{ConfigObject, ConfigRenderOptions}
import play.api.libs.json._
import play.api.Configuration

object Functions {
  val defaultPaginationSizeES: Option[Int] = Some(10)

  /** Given a `filename`, the function fully loads the content into an option and
    * maps it with `Json.parse`
    * @param filename fully filename of a resource file
    * @return A wrapped Json object from the given filename with an Option
    */
  def loadJSONFromFilename(filename: String): JsValue =
    Json.parse(filename.toFile.contentAsString)

  def loadJSONLinesIntoMap[A, B](filename: String)(f: JsValue => (A, B)): Map[A, B] = {
    val parsedLines = filename.toFile.lines.map(Json.parse)

    val pairs = for (l <- parsedLines)
      yield f(l)

    pairs.toMap
  }

  def loadConfigurationObject[T](key: String, config: Configuration)(implicit tReader: Reads[T]): T = {
    val defaultHarmonicOptions = Json.parse(config.underlying.getObject(key)
      .render(ConfigRenderOptions.concise())).as[T]

    defaultHarmonicOptions
  }

  def loadConfigurationObjectList[T](key: String, config: Configuration)(implicit tReader: Reads[T]): Seq[T] = {
    val defaultHarmonicDatasourceOptions = config.underlying.getObjectList(key).toArray.toSeq.map(el => {
      val co = el.asInstanceOf[ConfigObject]
      Json.parse(co.render(ConfigRenderOptions.concise())).as[T]
    })

    defaultHarmonicDatasourceOptions
  }

  /** the indexation of the pagination starts at page number 0 set by pageIndex and takes pageSize chunks
   * each time. The default pageSize is defaultPaginationSize
   * @param pageIndex ordinal of the pages chunked by pageSize. It 0-start based
   * @param pageSize the number of elements to get per page. default number defaultPaginationSize
   * @return tuple with the offset elements and the size to get
   */
  def parsePaginationTokensForES(pageIndex: Option[Int],
                                 pageSize: Option[Int] = defaultPaginationSizeES): (Int, Int) = {
    val pair = List(pageIndex, pageSize).map(_.map(_.abs).getOrElse(0))

    pair match {
      case List(0, 0) => (0, defaultPaginationSizeES.get)
      case List(0, s) => (0, s)
      case List(i, 0)  => (i*defaultPaginationSizeES.get, defaultPaginationSizeES.get)
      case List(i, s) => (i*s, s)
    }
  }
}
