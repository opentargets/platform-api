package controllers.api.v4.graphql

import play.api.libs.json.{Json, OWrites}

import java.sql.Timestamp

case class GraphQLError(isOT: Boolean,
                        error: String,
                        date: Timestamp,
                        variables: String,
                        complexity: Double,
                        query: String
) {

  def jsonWriter: OWrites[GraphQLError] = Json.writes[GraphQLError]

  override def toString: String = jsonWriter.writes(this).toString

}
