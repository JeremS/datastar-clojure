# Datastar http-kit adapter

## Installation

Install using clojars deps coordinates:

[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/http-kit.svg)](https://clojars.org/dev.data-star.clojure/http-kit)

Don't forget, you need the base SDK also:
[![Clojars Project](https://img.shields.io/clojars/v/dev.data-star.clojure/sdk.svg)](https://clojars.org/dev.data-star.clojure/sdk)

> [!IMPORTANT]
> This library adds, and needs a dependency to Http-kit as recent as the current
> `v2.9.0-beta2`. We do not recommend using older versions (`v2.8.1` being the
> current stable), it does not work properly with SSE.
