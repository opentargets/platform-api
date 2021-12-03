#!/bin/bash
set -ev
if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_TAG}" != "" ]; then
  docker pull "${QUAY_REPO}:${TRAVIS_BRANCH}" || true
  docker build --pull --cache-from "${QUAY_REPO}:${TRAVIS_BRANCH}" --tag "${QUAY_REPO}" . || docker build .
  docker login -u="${QUAY_USER}" -p="${QUAY_PASSWORD}" quay.io
  git_sha="${TRAVIS_COMMIT}"
  docker tag "${QUAY_REPO}" "${QUAY_REPO}:${TRAVIS_BRANCH}"
  docker tag "${QUAY_REPO}" "${QUAY_REPO}:${TRAVIS_COMMIT}-${TRAVIS_BRANCH}"
  docker push "${QUAY_REPO}:${TRAVIS_BRANCH}" && docker push "${QUAY_REPO}:${git_sha}-${TRAVIS_BRANCH}"
  if [ "${TRAVIS_BRANCH}" = "master" ]; then
    docker tag "${QUAY_REPO}:${TRAVIS_BRANCH}" "${QUAY_REPO}:latest"
    docker push "${QUAY_REPO}:latest"
  fi
fi
