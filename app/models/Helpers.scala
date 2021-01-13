package models

import better.files._
import com.typesafe.config.{ConfigObject, ConfigRenderOptions}
import play.api.libs.json._
import play.api._


object Helpers extends Logging {

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

  def loadConfigurationObject[T](key: String, config: Configuration)(
      implicit tReader: Reads[T]): T = {
    val defaultHarmonicOptions = Json
      .parse(
        config.underlying
          .getObject(key)
          .render(ConfigRenderOptions.concise()))
      .as[T]

    defaultHarmonicOptions
  }

  def loadConfigurationObjectList[T](key: String, config: Configuration)(
      implicit tReader: Reads[T]): Seq[T] = {
    val defaultHarmonicDatasourceOptions = config.underlying
      .getObjectList(key)
      .toArray
      .toSeq
      .map(el => {
        val co = el.asInstanceOf[ConfigObject]
        Json.parse(co.render(ConfigRenderOptions.concise())).as[T]
      })

    defaultHarmonicDatasourceOptions
  }

  def fromJsValue[A](jObj: JsValue)(implicit reader: Reads[A]): Option[A] = {
    val source = (__ \ '_source).json.pick
    jObj
      .transform(source)
      .asOpt
      .map(obj => {
        logger.debug(Json.prettyPrint(obj))
        obj.as[A]
      })
  }
}
