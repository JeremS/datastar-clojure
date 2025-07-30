# A tour of the SDK

Datastar allows you to control a web page from the backend. To do so it uses
http responses to either patch the current dom, or update signals.

In order to send patches Datastar provides 2 options:

- Return `text/html` or `application/json` HTTP response to patch the DOM or
  signals
- Start a Server Sent Events stream with a `text/event-stream` response and
  send SSE events to patch the page.

The Clojure SDK provides helpers when using the SSE options.

## Simple hello world

Let's start with a Datastar hello world. To use the SDK we make use of 2
namespaces:

```clojure
(require '[starfederation.datastar.clojure.api :as d*]                  ;; 1
         '[starfederation.datastar.clojure.adapter.http-kit :as hk-gen]);; 2

```

1. The core API
2. The specific API for a ring adapter, in this case Http-kit

We can imagine a page with the following HTML:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field"></p>
</div>
```

Here we have a button that will call the `/'say-hello'` endpoint when clicked.
We can have the following handler for this endpoint:

```clojure
(require '[some.hiccup.library :refer [html]])

(defn simple-hello [request]                             ;; 1
  (hk-gen/->sse-response request                         ;; 2
    {hk-gen/on-open                                      ;; 3
     (fn [sse-gen]                                       ;; 4
       (d*/patch-elements! sse-gen
         (html[:p {:id "hello-field"} "Hello world!"]))  ;; 5
       (d*/close-sse! sse-gen))}))                       ;; 6

```

1. we declare a standard ring handler which is a function of the HTTP request
2. the handler returns a SSE response prepared by the SDK function
3. we setup a callback that will be called once the SSE stream is opened
4. the callback receives a `sse-gen` which is our SSE connection
5. using the SDK's `patch-elements` function we send a HTML patch to the browser
6. we close the connection

When Datastar gets the patch it will morph the DOM to be:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field">Hello world!</p>
</div>
```

## Chunked hello world

The previous example could have been accomplished without the use of the SDK
since we only sent one patch. However, using SSE we could just as well chunk the
response. Consider this handler:

```clojure
(require '[some.hiccup.library :refer [html]])

(defn chunked-hello [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]

       (d*/patch-elements! sse-gen
         (html[:p {:id "hello-field"} "Hello"]))

       (Thread/sleep 1000)

       (d*/patch-elements! sse-gen
         (html[:p {:id "hello-field"} "Hello world!"]))

       (d*/close-sse! sse-gen))}))

```

Here we send 2 events. The first will morph the DOM into:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field">Hello</p>
</div>
```

The second:

```HTML
<div>
  <button data-on-click="@get(/'say-hello')">Say hello</button>
  <p id="hello-field">Hello world!</p>
</div>
```

The example on [the datastar homepage](https://data-star.dev/) is build
similarly to this. It helps illustrate the possibilities using the SSE. We
can sent multiple patches, do work between patches and we can keep the connection
alive for however long we want.

## Barebones broadcast

Speaking of keeping the connection alive, a simple broadcast system can be
implemented with the following code:

```clojure
(def !connections (atom #{}))                ;; 1


(defn subscribe-handler [request]            ;; 2
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))    ;; 3

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))})) ;; 4


(defn broadcast-elements! [elements]         ;;5
  (doseq [c @!connections]
    (d*/patch-elements! c elements)))

```

1. we keep an atom contains the open connections (SSE streams)
2. we have a ring handler to setup the SSE stream
3. when the handler is called the `sse-gen` is added to `!connections`
4. when the connections is closed we remove it from `!connections`
5. the broadcast function will sent a html elements for all connected browser to
   merge

In this example we do not automatically close the `sse-gen`. Depending on the
adapter you use it will be kept alive until either the client closes the SSE
connection or your code does it somewhere else.

We can imagine the following html:

```HTML
<body data-on-load="@get('/subscribe')">
</body>
```

Here the web page will call the `/subscribe` endpoint on load, this endpoint
being routed to our `subscribe-handler`.

## Fat updates and compression

Long lived connections open interesting possibilities. A common pattern when
using Datastar is to keep one SSE stream open and push updates when relevant
event occurred on the server.

We can have a page setup this way:

```HTML
<body data-on-load="/updates">
  <div id="main">
    Imagine a complex UI here
  </div>
</body>
```

And code similar to our broadcast example:

```clojure
;; Broadcasting logic
(def !connections (atom #{}))


(defn updates-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))}))


(defn broadcast-frame! [frame]
  (doseq [c @!connections]
    (d*/patch-elements! c frame)))


;; Renders the whole main content of the page
(defn render-frame [state]
  (html
    [:div#main "Do something with the state here"])


;; The state the rendering is based on
(def !state (atom {:some-complex "state"}))


(add-watch !state ::watch
  (fn [_k _ref old new]
    (when-not (identical? old new)
      (let [frame (render-frame new)]
        (broadcast-frame! frame))))))

```

When this page load, the `/updates` endpoint is called setting up a long lived
SSE connection. When `!state` changes we broadcast a re-render of the whole
main content of the page without.

Using fat updates instead of fine grained ones might seem wasteful at first.
However SSE streams compress really, really well. Also, this pattern gives us
a very simple model. We don't need to keep track of which fine grained
updates may not have gone through risking a page only partially updated.

To use compression in this example we just need to use an option of the
`->sse-response` function. Our `update-handler` would look like this

```clojure
(defn update-handler [request]
  (hk-gen/->sse-response request
    {hk-gen/on-open
     (fn [sse-gen]
       (swap! !connections conj sse-gen))

     hk-gen/on-close
     (fn [sse-gen status]
       (swap! !connections disj sse-gen))

     ;; We add a write profile here to enable gzip compression
     hk-gen/write-profile hk-gen/gzip-profile}))

```

For more about the compression option and this write profile concept,
checkout the [relevant docs](./Write-profiles.md)
