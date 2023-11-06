#!/bin/bash
set -ev
if [ "${TRAVIS_PULL_REQUEST}" = "true" ] || [ "${TRAVIS_TAG}" != "" ]; then
  if [ "${TRAVIS_TAG}" != "" ]; then
      docker_tag_label="${TRAVIS_TAG}"
    else
      docker_tag_label="${TRAVIS_BRANCH}"
    fi
  docker pull "${QUAY_REPO}:${docker_tag_label}" || true
  docker build --pull --cache-from "${QUAY_REPO}:${docker_tag_label}" --tag "${QUAY_REPO}" . || docker build .
  docker login -u="${QUAY_USER}" -p="${QUAY_PASSWORD}" quay.io
  git_sha="${TRAVIS_COMMIT}"
  docker tag "${QUAY_REPO}" "${QUAY_REPO}:${docker_tag_label}"
  docker tag "${QUAY_REPO}" "${QUAY_REPO}:${TRAVIS_COMMIT}-${docker_tag_label}"
  docker push "${QUAY_REPO}:${docker_tag_label}" && docker push "${QUAY_REPO}:${git_sha}-${docker_tag_label}"
  if [ "${TRAVIS_BRANCH}" = "master" ]; then
    docker tag "${QUAY_REPO}:${TRAVIS_BRANCH}" "${QUAY_REPO}:latest"
    docker push "${QUAY_REPO}:latest"
  fi
fi
