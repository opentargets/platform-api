package models.gql

import models.Backend
import models.entities.Configuration._
import models.entities._
import sangria.macros.derive.{InputObjectTypeName, deriveInputObjectType, deriveObjectType}

object GQLImplicits {
  implicit val paginationImp = deriveInputObjectType[Pagination]()
  implicit val metaDataVersionImp = deriveObjectType[Backend, DataVersion]()
  implicit val metaAPIVersionImp = deriveObjectType[Backend, APIVersion]()
  implicit val metaImp = deriveObjectType[Backend, Meta]()
  implicit val datasourceSettingsInputImp = deriveInputObjectType[DatasourceSettings](
    InputObjectTypeName("DatasourceSettingsInput")
  )

  implicit val aggregationFilterImp = deriveInputObjectType[AggregationFilter](
    InputObjectTypeName("AggregationFilterInput")
  )
}
