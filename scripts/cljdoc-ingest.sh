#!/bin/bash

podman run \
  --rm \
  --volume $(pwd):/repo-to-import \
  --volume "$HOME/.m2:/root/.m2" \
  --volume ./.cljdoc-preview:/app/data \
  --platform linux/amd64 \
  --entrypoint clojure \
  cljdoc/cljdoc -Sforce -M:cli ingest \
  --project dev.data-star.clojure/sdk \
  --version 1.0.0-RC1 \
  --git /repo-to-import \
  --rev $(git rev-parse HEAD)
