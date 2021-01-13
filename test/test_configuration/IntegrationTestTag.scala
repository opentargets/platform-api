package test_configuration

import org.scalatest.Tag

/**
  * Requires that:
  *   - a configured instance of Elasticsearch be reachable on localhost:9200.
  */
object IntegrationTestTag extends Tag("test_configuration.IntegrationTestTag")
