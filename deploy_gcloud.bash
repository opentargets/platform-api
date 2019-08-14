#!/bin/bash

gcloud --project=open-targets-eu-dev app deploy \
    --promote \
    -v $(git describe --abbrev=0 \
    --tags | sed "s:\.:-:g")
