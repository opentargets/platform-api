package models.entities

import clickhouse.rep.SeqRep._
import clickhouse.rep.SeqRep.Implicits._
import models.entities.Configuration.{DatasourceSettings, LUTableSettings}
import models.entities.Configuration.JSONImplicits._
import models.entities.Network.JSONImplicits._
import play.api.libs.json.Json
import slick.jdbc.GetResult

/**
 * this is one side of an full association as the other part is fixed. In this
 * case those are T <-> D and an association is built based on a harmonic computation
 * where the overall score is `score` and each datasource contribution is contained
 * in `scorePerDS` vector
 * @param id the Id of the entity which is associated
 * @param score the overall harmonic sum score built up from its datasource contriutions
 * @param scorePerDS the list of harmonic scores one per datasource. the order of the scores
 *                   come fixed from the order of datasource list passed to the query
 */
case class Association(id: String, score: Double, scorePerDS: Vector[Double])

/**
 * Agroup of associations to one node.
 * @param network the configuration for the progagation network used
 * @param node the `NetworkNode` contains the id used for the associations
 *             and its neighbours
 * @param datasources the list of `DatasourceSettings` per datasource
 * @param rows list of `Association` objects
 */
case class Associations(network: Option[LUTableSettings],
                        node: Option[NetworkNode],
                        datasources: Seq[DatasourceSettings],
                        rows: Vector[Association])

object Associations {
  object DBImplicits {
    implicit val getAssociationRowFromDB: GetResult[Association] = {
      GetResult(r => Association(r.<<, r.<<, DSeqRep(r.<<)))
    }
  }

  object JSONImplicits {
    implicit val AssociationImp = Json.format[Association]
    implicit val associationsImp = Json.format[Associations]
  }

}
