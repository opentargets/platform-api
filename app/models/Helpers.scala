package models

import better.files._
import com.typesafe.config.{ConfigObject, ConfigRenderOptions}
import play.api.libs.json._
import play.api._

import scala.util.Try

object Helpers extends Logging {
  object Cursor extends Logging {
    def to(searchAfter: Option[String]): Option[JsValue] =
      searchAfter
        .flatMap(sa => {
          val vv =
            Try(Json.parse(Base64Engine.decode(sa)))
              .map(_.asOpt[JsValue])
              .fold(ex => {
                logger.error(s"bae64 encoded  ${ex.toString}")
                None
              }, identity)

          if (logger.isDebugEnabled) {
            logger.debug(
              s"base64 $sa decoded and parsed into JsObject " +
                s"as ${Json.stringify(vv.getOrElse(JsNull))}")
          }

          vv
        })

    def from(obj: Option[JsValue]): Option[String] =
      obj.map(jsv => Base64Engine.encode(Json.stringify(jsv))).map(new String(_))
  }

  object Base64Engine extends Logging {
    def encode(msg: String): String = java.util.Base64.getEncoder.encode(msg.getBytes).map(_.toChar).mkString
    def decode(msg: String): String = java.util.Base64.getDecoder.decode(msg.getBytes).map(_.toChar).mkString
  }


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
        logger.trace(Json.prettyPrint(obj))
        obj.as[A]
      })
  }
}
