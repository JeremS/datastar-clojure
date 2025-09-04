#!/bin/bash

podman run \
  --rm \
  --publish 8000:8000 \
  --volume "/home/jeremy/.m2:/root/.m2" \
  --volume ./.cljdoc-preview:/app/data \
  --platform linux/amd64 \
  cljdoc/cljdoc
