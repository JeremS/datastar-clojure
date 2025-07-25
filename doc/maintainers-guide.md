# Maintainers Guide

## Directory structure

- `sdk`: the source folder for the main SDK
- `sdk-adapter-*`: source folders for adapter specific code
- `sdk-malli-schemas`: self explanatory...
- `sdk-brotli`: brotli write profiles
- `src/bb`: tasks used run a repl, tests...
- `src/bb-example`: bb examples
- `src/dev`: dev utils, examples
- `src/test`: centralized tests for all the libraries
- `test-resources`: self explanatory

## bb tasks

### Release tasks

- `bb bump-version patch/minor/major`: to bump a version component across all libs
- `bb set-version x.x.x`: to set the version component across all libs
- `bb jar:all`: Build jars artifacts for all of the libs
- `bb install:all`: Build jars artifacts for all of the libs and installs them locally
- `bb jar:<lib-name>`: Build jars artifacts for one of the libs
- `bb clean`: Clean all build artifacts
- `bb publish:all`: Publish the artifacts to clojars.org

### Development tasks `bb run dev`

- `bb run dev`: start a repl with the dev nss, the test nss the malli schemas,
  ring-jetty and Http-kit on the classpath
- `bb run dev:rj9a`: same as basic dev task expect for ring-jetty being replaced
  with rj9a.
- `bb run dev:bb`: start a bb repl with the core SDK, http-kit adapter and
  malli-schemas in the classpath.

> [!note]
> You can add additional deps aliases when calling these tasks:
> `bb run dev :debug` will add a Flowstorm setup

### Test tasks `bb run test`

- `bb run test:all`: run all test for the SDK, the Http-kit adapter and the
  ring adapter using ring-jetty.
- `bb run test:rj9a`: run all test for the SDK and the ring adapter using rj9a.
- `bb run test:bb`: run unit tests for the SDK in Babashka.
- `bb run test:SDK-common`: start the server used to run the
  [SDKs' common tests](https://github.com/starfederation/datastar/tree/develop/sdk/tests).

## Release

- The library artifacts are published to Clojars (http://clojars.org) under the `dev.data-star.clojure` namespace.
- The Clojars account and deploy token are managed by Ben Croker, and added to this repo as GitHub action secrets:
  - Secret name: `CLOJARS_USERNAME`
    Value: _the clojars account username_
  - Secret name: `CLOJARS_PASSWORD`
    Value: _the clojars deploy token_
- The libraries' versions are bumped in lockstep so that there is no confusion over which version of the common lib should be used with an adapter lib.

The Github Actions [CI workflow for clojure](../.github/workflows/release-sdk.yml) will always run the tests and produce jar artifacts.

Triggering a deployment to clojars is a manual process. A Datastar core contributor must trigger the `Release Clojure SDK` workflow with the `publish` input boolean set to `true`.

**Release process:**

1. Use `bb set-version` or `bb bump-version` to update the library versions in lockstep
2. Commit those changes and push to GitHub
3. A core contributor must trigger the workflow manually setting `publish` to `true`

## Test

### Running tests

- for the unit and smoke tests see the bb tasks.
- for the generic bash SDK tests:
  1. Start the test server with `bb run test:sdk-common`
  2. Run `go run github.com/starfederation/datastar/sdk/tests/cmd/datastar-sdk-tests@latest`

### webdriver config

Tests resources contains a test.config.edn file. It contains a map whose keys
are:

- `:drivers`: [etaoin](https://github.com/clj-commons/etaoin) webdriver types to run
- `:webdriver-opts`: a map of webdriver type to webriver specific options
