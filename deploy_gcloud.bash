#!/bin/bash

# Script to update the gcloud deployment.
#
# Running this script **automatically** promotes the latest version of master with the tag of the last committed
# tag on the repository.

gcloud --project=open-targets-eu-dev app deploy \
    --promote \
    -v $(git describe --abbrev=0 \
    --tags | sed "s:\.:-:g")
