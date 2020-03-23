package models.entities

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class ECO(id: String, label: String)

object ECO {
  val logger = Logger(this.getClass)

  object JSONImplicits {
    implicit val ecoImpF = Json.format[models.entities.ECO]
  }

  def fromJsValue(jObj: JsValue): Option[ECO] = {
    /* apply transformers for json and fill the target
     start from internal objects and then map the external
     */
    import ECO.JSONImplicits._

    val source = (__ \ '_source).json.pick
    jObj.transform(source).asOpt.map(obj => {
      logger.debug(Json.prettyPrint(obj))
      obj.as[ECO]
    })
  }
}
