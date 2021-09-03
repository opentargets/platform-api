package test_configuration

import org.scalatest.Tag

/**
 * Requires that:
 *   - a configured instance of Elasticsearch be reachable on localhost:9200.
 */
object IntegrationTestTag extends Tag("test_configuration.IntegrationTestTag")

/**
 * Requires that:
 *   - a configured instance of Clickhouse be reachable on localhost:8123
 */
object ClickhouseTestTag extends Tag("test_configuration.ClickhouseTestTag")
