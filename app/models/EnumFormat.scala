package models

import play.api.libs.json.*
import scala.quoted.*

trait EnumFormat[T] extends Format[T]

object EnumFormat:
  inline def derived[T]: EnumFormat[T] = ${ EnumMacros.enumFormatMacro[T] }

object EnumMacros {

  def enumFormatMacro[T: Type](using Quotes): Expr[EnumFormat[T]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]

    val sym = tpe.classSymbol match
      case Some(sym) if sym.flags.is(Flags.Enum) && !sym.flags.is(Flags.JavaDefined) =>
        sym
      case _ =>
        report.errorAndAbort(s"${tpe.show} is not an enum type")

    def reifyValueOf(name: Expr[String]) =
      Select
        .overloaded(Ref(sym.companionModule), "valueOf", Nil, name.asTerm :: Nil)
        .asExprOf[T & reflect.Enum]

    '{
      new EnumFormat[T] {
        private def valueOfUnsafe(name: String): T = ${ reifyValueOf('name) }

        override def reads(json: JsValue): JsResult[T] = try
          JsSuccess(valueOfUnsafe(json.as[JsString].value))
        catch {
          case e: NoSuchElementException => JsError(e.getMessage)
        }

        override def writes(o: T): JsValue = JsString(o.toString)
      }
    }
}
