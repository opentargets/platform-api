# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Commands

### Development

- **Run the API locally**: `make run`
- **Run the API in debug mode**: `make debug` (listens on port 9999 for debugger)
- **Run with custom logging**: `make run_log logfile=<path-to-logback.xml>` or `make debug_log logfile=<path-to-logback.xml>`
- **Create tunnels to Elasticsearch/ClickHouse**:
  - Elasticsearch: `make es_tunnel instance=<instance-name> zone=<zone>`
  - ClickHouse: `make ch_tunnel instance=<instance-name> zone=<zone>`

### Testing

- **Run all tests**: `sbt test`
- **Run GraphQL integration tests**: `sbt testOnly controllers.GqlTest`
  - **Update GraphQL query files from frontend**: `sbt updateGqlFiles` (checks for new/updated queries)
  - **Fetch GraphQL query files from frontend**: `sbt updateGqlFiles` (downloads and replaces existing queries)

### Cache Management

- **Clear Sangria cache**: `curl --location --request GET 'http://localhost:9000/api/v4/rest/cache/clear' --header 'apikey: <secret>'`
- **Disable cache for development**: Set environment variable `PLATFORM_API_IGNORE_CACHE=true`

## Codebase Architecture

### High-Level Overview

This is a **GraphQL API** for the Open Targets Platform, built with:
- **Scala 3.5.0** (primary language)
- **Play Framework 3.0.5** (web framework)
- **Sangria 4.1.1** (GraphQL implementation)
- **Slick 3.5.0** (database query builder for ClickHouse)
- **Elastic4s 8.11.3** (Elasticsearch client)
- **ClickHouse JDBC 0.6.4** (ClickHouse database driver)

### Key Components

1. **GraphQL Schema (`app/models/GQLSchema.scala`)**
   - Defines the root query type with all available fields (targets, diseases, drugs, variants, studies, etc.)
   - Uses **Sangria deferred resolvers** for efficient data fetching and caching
   - Implements **complexity calculation** to prevent overly expensive queries

2. **Data Retrievers**
   - **ElasticRetriever** (`app/models/ElasticRetriever.scala`): Handles all Elasticsearch queries
   - **ClickHouseRetriever** (`app/models/ClickhouseRetriever.scala`): Handles all ClickHouse queries via Slick
   - **Backend** (`app/models/Backend.scala`): Coordinates between retrievers and implements business logic

3. **Entity Models**
   - Located in `app/models/entities/`
   - Define case classes for all major entities (Target, Disease, Drug, Variant, Study, etc.)
   - Each entity has a corresponding GraphQL implementation in `app/models/gql/Objects.scala`

4. **Deferred Resolvers**
   - Located in `app/models/gql/DeferredResolvers.scala` and `app/models/gql/Fetchers.scala`
   - Implement **batched data fetching** to avoid N+1 query problems
   - Support **caching** to improve performance for repeated queries

5. **REST Endpoints**
   - Cache management: `app/controllers/api/v4/rest/CacheController.scala`
   - Metadata: `app/controllers/api/v4/rest/MetaController.scala`
   - Prometheus metrics: `app/controllers/api/v4/rest/PrometheusController.scala`

### GraphQL Implementation Details

- **Query Structure**: The API supports queries for:
  - Individual entities by ID (e.g., `target`, `disease`, `drug`)
  - Multiple entities by IDs (e.g., `targets`, `diseases`, `drugs`)
  - Search across all entities (`search`)
  - Faceted search (`facets`)
  - ID mapping (`mapIds`)
  - Complex queries for studies, credible sets, and clinical reports

- **Deferred Resolution**: Uses Sangria's deferred resolution system to:
  - Batch multiple requests for the same entity type
  - Cache results to avoid redundant database queries
  - Handle complex nested queries efficiently

- **Complexity Management**: Implements custom complexity calculation to:
  - Prevent overly expensive queries that could impact performance
  - Limit the depth and breadth of queries

### Data Flow

1. **Request Handling**:
   - GraphQL requests are handled by `GraphQLController` (`app/controllers/api/v4/graphql/GraphQLController.scala`)
   - REST requests are handled by their respective controllers

2. **Query Execution**:
   - Sangria parses and validates the GraphQL query
   - Deferred resolvers are triggered for each field
   - Resolvers fetch data from Elasticsearch or ClickHouse
   - Data is transformed into entity models
   - Results are returned to the client

3. **Caching**:
   - Sangria caches deferred resolver results by default
   - Cache can be disabled with `PLATFORM_API_IGNORE_CACHE=true`
   - Cache can be cleared via REST endpoint

### Key Technical Considerations

1. **Performance**:
   - **Batched fetching**: Deferred resolvers batch multiple requests for the same entity type
   - **Caching**: Sangria's built-in caching reduces database load for repeated queries
   - **Complexity limits**: Prevents overly expensive queries from impacting performance

2. **Data Sources**:
   - **Elasticsearch**: Used for full-text search, faceting, and some entity data
   - **ClickHouse**: Used for structured data, associations, and complex queries

3. **Testing**:
   - **GraphQL integration tests**: Validate that frontend queries work against the API
   - **Tagged tests**: Integration tests require Elasticsearch access and are tagged with `IntegrationTestTag`

4. **Logging**:
   - Configured via `logback.xml`
   - Production logging is conservative to avoid excessive GCP charges
   - Local development logging can be customized via `logback.xml`

### Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `PLATFORM_API_IGNORE_CACHE` | Disable Sangria caching | `false` |
| `ELASTICSEARCH_HOST` | Elasticsearch host | `localhost` |
| `SLICK_CLICKHOUSE_URL` | ClickHouse JDBC URL | `jdbc:clickhouse://localhost:8123` |
