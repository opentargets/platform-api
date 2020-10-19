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

The first step is tag to the new release. 

```bash
git tag -a 0.46.3 -m "Release 0.46.3"
git push origin 0.46.3
git push --tags
```

The file Dockerfile contains the instruction to build and copy the jar.
To create the distribution 

```sbt dist```


The final step is running deploy script 
```
bash deploy_gcloud.bash 0.46.3
```


# Copyright

Copyright 2014-2018 Biogen, Celgene Corporation, EMBL - European Bioinformatics Institute, GlaxoSmithKline, Takeda Pharmaceutical Company and Wellcome Sanger Institute

This software was developed as part of the Open Targets project. For more information please see: http://www.opentargets.org

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
