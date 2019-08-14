package models

import play.api.libs.json.Json
import sangria.macros.derive._
import sangria.marshalling.playJson._
import sangria.schema._
import Entities._
import Entities.JSONImplicits._

trait GQLMeta {
  //  implicit val hmOptionsImp = Json.format[Entities.HarmonicOptions]
  //  implicit val dsOptionsImp = Json.format[Entities.HarmonicDatasourceOptions]
  //  implicit val hsImp = Json.format[Entities.Harmonic]
  //  implicit val assocEntitiyImp = Json.format[Entities.AssociationEntity]
  //  implicit val assocRowImp = Json.format[Entities.AssociationRow]
  //  implicit val assocTableImp = Json.format[Entities.AssociationTable]
  //
  //  implicit val harmonicOptionsInputImp = deriveInputObjectType[Entities.HarmonicOptions](InputObjectTypeName("HarmonicOptionsInput"))
  //  implicit val datasourceOptionsInputImp = deriveInputObjectType[Entities.HarmonicDatasourceOptions](InputObjectTypeName("HarmonicDatasourceOptionsInput"))
  //  implicit val harmonicSumInputImp = deriveInputObjectType[Entities.Harmonic](InputObjectTypeName("HarmonicInput"))
  //
  //  implicit val harmonicOptionsImp = deriveObjectType[Backend, Entities.HarmonicOptions]()
  //  implicit val datasourceOptionsImp = deriveObjectType[Backend, Entities.HarmonicDatasourceOptions]()
  //  implicit val harmonicSumImp = deriveObjectType[Backend, Entities.Harmonic]()
  //
  //  implicit val associationEntityImp = deriveObjectType[Backend, Entities.AssociationEntity]()
  //  implicit val associationRowImp = deriveObjectType[Backend, Entities.AssociationRow]()
  //  implicit val associationTableImp = deriveObjectType[Backend, Entities.AssociationTable]()
  implicit val metaVersionImp = deriveObjectType[Backend, Entities.MetaVersion]()
  implicit val metaImp = deriveObjectType[Backend, Entities.Meta]()
}

object GQLSchema extends GQLMeta {
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
       Field("meta", metaImp,
         description = Some("Return Open Targets API metadata information"),
         arguments = Nil,
         resolve = ctx => ctx.ctx.getMeta)
    ))

  val schema = Schema(query)
}
