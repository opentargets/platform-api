# Open Targets Platform API

The GraphQL API supports queries for targets, disease/phenotype, drug, target-disease association and Search. You can also query metadata on the API to get the version of the data and API.

## Tech Stack

| Technology      | Version |
| --------------- | ------- |
| Scala           | 2.13.10 |
| Play Framework  | 2.8.18  |
| Sangria         | 3.5.3   |
| Slick           | 3.4.1   |
| elastic4s       | 8.5.3   |
| clickhouse-jdbc | 0.3.2   |

## Requirement

SBT (Scala)
Java 1.8 or later
Play Framework

ElasticSearch server: 7.2

## How to use

To run locally you will need to have access to Elastic Search and Clickhouse. These instances need to run in specific ports Elastic Search will have to run in port 9200 and Clickhouse needs to be running in port 8123. You can do this by having local instances or tunnel to a server hosted instance.

To tunnel the instances hosted in GCP you can use the follow commands

Elastic Search

```bash
gcloud beta compute ssh --zone "<instance zone>" <some ES instance> --tunnel-through-iap -- -L 9200:localhost:9200
```

Clickhouse

```bash
gcloud compute ssh <some Clickhouse instance> --zone="<instance zone>" --tunnel-through-iap -- -L 8123:localhost:8123
```

Once you have access to the data you can execute `sbt run` to run the API. This will start an instance in port 9000. To debug the API you'll need to run `sbt -jvm-debug 9999 run`. After the API has started you can access the GraphQL Playground in `http://localhost:9000/playground`.

## Sangria caches

This application uses Sangria as a GraphQL wrapper and uses deferred resolver caches to improve query times. In cases
where the data is updated in Elasticsearch it will not be available on the front-end if it has previously been cached.

To avoid using the cache in your deployments you can set the environment variable `$PLATFORM_API_IGNORE_CACHE` to `true`. The default value is `false` which means the cache will be used.

To reset the cache following a data update use the following request:

```
curl --location --request GET 'http://localhost:9000/api/v4/rest/cache/clear' \
--header 'apikey: <very secret code>'
```

## Logging

Logging to local use / development can be configured by updating the `logback.xml` file in the _conf_ directory.

Production deployments use the `production.xml` file to configure loggging. These should be set conservatively because
GCP charges based on the quantity of logs, so we only want to produce what we need for monitoring, basic
trouble-shooting.

## Testing

Tests annoted with `IntegrationTestTag` require there to be access to a configured ElasticSearch instance against which
to run the queries.

### Testing GraphQL queries

The Open Targets Platform front end makes use of pre-written GraphQL queries. Since we want to be aware if changes in
the API are likely to break the FE, we have integration tests in place to check if this is going to happen.

Note, make sure you have access to ElasticSearch and Clickhouse!

1. Get the files: run `sbt updateGqlFiles` to retrieve all '\*.gql' files from the front-end repository and copy them to
   the
   `test/resources/gqlQueries` directory and prints output regarding which files are new / changed.
2. Run tests `sbt testOnly controllers.GqlTest`

#### Maintaining up to date

Since the FE and BE are developed independently, it's worth checking what has changed since we last tested. Before
testing run `sbt updateGqlFiles`. This will print which files are new or updated.

If there are updated files, run `git diff test/resources/gqlQueries` to see if any previously configured tests require
updating (mainly if the input parameters change. If there are new files new tests will need to be added.

#### Adding new tests and inputs

If the above step shows that there are more files to add, create a new test for them using an existing one as a
template. For example:

```scala
"Cancer gene census queries" must {
  "return a valid response" in {
    testQueryAgainstGqlEndpoint(TargetDiseaseSize("CancerGeneCensus_sectionQuery"))
  }
}
```

Take note of the following:

- 'CancerGeneCensus_sectionQuery' is the name of the file, this will be used to read in the actual query.
- `TargetDiseaseSize` is a case class which extends `GqlCase`. You choose the relevant case class based on which inputs
  are required by the file you are adding. Looking at the 'CancerGeneCensus_sectionQuery' query, we see that it takes
  three parameters, target, disease and size:

```
query CancerGeneCensusQuery($ensemblId: String!, $efoId: String!, $size: Int!) {
  disease(efoId: $efoId) {
    id
    evidences(
      ensemblIds: [$ensemblId]
```

- It just so happens that `TargetDiseaseSize` will generate inputs that satisfy this requirement. To see what else is
  available consider other case classes which extend GqlCase.

##### Adding new inputs

- The GraphQL test are using generators to create inputs for the queries. The generators themselves are defined
  in `GqlItTestInputs.scala` and read from files in `/test/resources/gqpInputs`.
- The starting point for the input lists were those used by Checkomatic to identify useful targets and diseases to test
  against. To add more inputs add them to the resource files.

# Copyright

Copyright 2014-2018 Biogen, Celgene Corporation, EMBL - European Bioinformatics Institute, GlaxoSmithKline, Takeda
Pharmaceutical Company and Wellcome Sanger Institute

This software was developed as part of the Open Targets project. For more information please
see: http://www.opentargets.org

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
