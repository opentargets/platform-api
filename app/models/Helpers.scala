package models

import com.typesafe.config.{ConfigObject, ConfigRenderOptions}
import play.api.libs.json._
import play.api._

import annotation.targetName
import scala.util.Try
import sangria.marshalling.FromInput
import sangria.util.tag.@@
import sangria.schema.*
import entities._

object Helpers extends Logging {
  object Cursor extends Logging {
    def to(searchAfter: Option[String]): Option[JsValue] =
      searchAfter
        .flatMap { sa =>
          val vv =
            Try(Json.parse(Base64Engine.decode(sa)))
              .map(_.asOpt[JsValue])
              .fold(
                ex => {
                  logger.error(s"bae64 encoded  ${ex.toString}")
                  None
                },
                identity
              )

          if (logger.isDebugEnabled) {
            logger.debug(
              s"base64 $sa decoded and parsed into JsObject " +
                s"as ${Json.stringify(vv.getOrElse(JsNull))}"
            )
          }

          vv
        }

    def from(obj: Option[JsValue]): Option[String] =
      obj.map(jsv => Base64Engine.encode(Json.stringify(jsv))).map(new String(_))
  }

  object Base64Engine extends Logging {
    def encode(msg: String): String =
      java.util.Base64.getEncoder.encode(msg.getBytes).map(_.toChar).mkString

    def decode(msg: String): String =
      java.util.Base64.getDecoder.decode(msg.getBytes).map(_.toChar).mkString
  }

  def loadConfigurationObject[T](key: String, config: Configuration)(implicit
      tReader: Reads[T]
  ): T = {
    val defaultHarmonicOptions = Json
      .parse(
        config.underlying
          .getObject(key)
          .render(ConfigRenderOptions.concise())
      )
      .as[T]

    defaultHarmonicOptions
  }

  def loadConfigurationObjectList[T](key: String, config: Configuration)(implicit
      tReader: Reads[T]
  ): Seq[T] = {
    val defaultHarmonicDatasourceOptions = config.underlying
      .getObjectList(key)
      .toArray
      .toSeq
      .map { el =>
        val co = el.asInstanceOf[ConfigObject]
        Json.parse(co.render(ConfigRenderOptions.concise())).as[T]
      }

    defaultHarmonicDatasourceOptions
  }

  def fromJsValue[A](jObj: JsValue)(implicit reader: Reads[A]): Option[A] = {
    val source = (__ \ Symbol("_source")).json.pick
    jObj
      .transform(source)
      .asOpt
      .map { obj =>
        logger.trace(Json.prettyPrint(obj))
        obj.as[A]
      }
  }

  def emptySetToSetOfEmptyString(s: Set[String]): Set[String] =
    if (s.isEmpty) Set("") else s

  object ComplexityCalculator {
    @targetName("multiplierFromStringSeq")
    def complexityCalculator(
        multiplierArg: Argument[Seq[String @@ FromInput.CoercedScalaResult]]
    ): (Backend, Args, Double) => Double = { (ctx, args, childScore) =>
      args.arg(multiplierArg).length * childScore
    }
    @targetName("multiplierFromPageArgs")
    def complexityCalculator(
        multiplierArg: Argument[Option[Pagination]]
    ): (Backend, Args, Double) => Double = { (ctx, args, childScore) =>
      args.arg(multiplierArg).getOrElse(Pagination.mkDefault).size * childScore
    }
    @targetName("multiplierFromPageSize")
    def complexityCalculator(
        multiplierArg: Argument[Option[Int]]
    ): (Backend, Args, Double) => Double = { (ctx, args, childScore) =>
      args.arg(multiplierArg).getOrElse(Pagination.sizeDefault) * childScore
    }
  }
}
