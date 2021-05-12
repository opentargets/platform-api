# Open Targets Platform API BETA (experimental)

Experimental GraphQL API.

## Requirement
SBT (Scala)
Java 1.8 or later
Play Framework

ES server: 7.2
Eg. localhost:9200  (tunnelling or locally installed)


## How to use

## How to deploy to Google AppEngine 

Promote the deployed version to receive all traffic or deploy an AppEngine specific version.

#### Deploy and set as default traffic

The first step is tag to the new release. 

```bash
git tag -a 0.46.4 -m "Release 0.46.4"
git push origin 0.46.4
git push --tags
```

The file Dockerfile contains the instruction to build and copy the jar.
To create the distribution 

```sbt dist```

The final step is running deploy script

```
   bash deploy_gcloud.bash
```

#### Deployed AppEng version

The file Dockerfile contains the instruction to build and copy the jar.
To create a local distribution run the following command:

```sbt dist```

Create locally the app adding a specific version name 
otherwise if you do not specify a version, one will be generated for you.
Eg. hpo-1-0

```
gcloud --project=open-targets-eu-dev app deploy \
    --no-promote \
    -v hpo-1-0

```

## Sangria caches

This application uses Sangria as a GraphQL wrapper and uses deferred resolver
caches to improve query times. In cases where the data is updated in Elasticsearch
it will not be available on the front-end if it has previously been cached.

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

Note, make sure you have access to ElasticSearch on a configured port!

```
gcloud beta compute ssh --zone "europe-west1-d" [some es instance] --tunnel-through-iap -- -L 9200:localhost:9200
```

1. Get the files: run `sbt getGqlFiles` to retrieve all '*.gql' files from the front-end repository and copy them to the
   `test/resources/gqlQueries` directory.
2. Run tests `sbt testOnly testOnly controllers.GqlTest`

#### Maintaining up to date

Since the FE and BE are developed independently, it's worth checking what has changed since we last tested. After
running the sbt task to update the gql files run: `git status -u test/resources/gqlQueries`. This will output any new
files for which we need to create tests.

It is also worth running `git diff test/resources/gqlQueries` to see if any previously configured tests require
updating.

#### Adding new tests

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
