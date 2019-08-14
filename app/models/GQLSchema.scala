package models

import play.api.libs.json.Json
import sangria.macros.derive._
import sangria.marshalling.playJson._
import sangria.schema._

object GQLSchema {
  implicit val paginationFormatImp = Json.format[Entities.Pagination]
  val pagination = deriveInputObjectType[Entities.Pagination]()
  val pageArg = Argument("page", OptionInputType(pagination))

  val query = ObjectType(
    "Query", fields[Backend, Unit](
//       Field("associationsByDiseaseId", associationTableImp,
//         arguments = diseaseIdArg :: indirectAssocsArg :: harmonicArg :: orderBy:: pageArg :: Nil,
//         resolve = ctx =>
//           ctx.ctx.getAssociationsByDisease(ctx.arg(diseaseIdArg),
//             ctx.arg(indirectAssocsArg),
//             ctx.arg(harmonicArg),
//             ctx.arg(orderBy),
//             ctx.arg(pageArg))
//       ),
//       Field("associationsByTargetId", associationTableImp,
//         arguments = targetIdArg :: indirectAssocsArg :: harmonicArg :: orderBy:: pageArg :: Nil,
//         resolve = ctx =>
//           ctx.ctx.getAssociationsByTarget(ctx.arg(targetIdArg),
//             ctx.arg(indirectAssocsArg),
//             ctx.arg(harmonicArg),
//             ctx.arg(orderBy),
//             ctx.arg(pageArg))
//       ),
//       Field("datasources", ListType(datasourceEntityImp),
//         arguments = Nil,
//         resolve = ctx => ctx.ctx.getDatasources),
//       Field("dataFields", ListType(dataTableFieldsImp),
//         arguments = Nil,
//         resolve = ctx => ctx.ctx.getDataTableFields),
//       Field("mlModels", ListType(StringType),
//         arguments = Nil,
//         resolve = ctx => ctx.ctx.getModels)
    ))

  val schema = Schema(query)
}
