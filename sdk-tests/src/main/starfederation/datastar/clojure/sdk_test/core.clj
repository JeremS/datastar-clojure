(ns starfederation.datastar.clojure.sdk-test.core
  (:require
    [charred.api                                  :as charred]
    [clojure.set                                  :as set]
    [reitit.ring.middleware.parameters            :as rrm-params]
    [reitit.ring.middleware.multipart             :as rrm-multi-params]
    [reitit.ring                                  :as rr]
    [starfederation.datastar.clojure.adapter.ring :refer [->sse-response on-open]]
    [starfederation.datastar.clojure.api          :as d*]))

;; -----------------------------------------------------------------------------
;; JSON / Datastar signals utils
;; -----------------------------------------------------------------------------
(def ^:private bufSize 1024)
(def read-json (charred/parse-json-fn {:async? false :bufsize bufSize}))

(defn get-signals [req]
  (-> req d*/get-signals read-json))


;; -----------------------------------------------------------------------------
;; Parsing / reformatting received events
;; -----------------------------------------------------------------------------
(def events-json-key "events")

(defn get-events [signals]
  (get signals events-json-key))


(def event-type-json-key "type")


(defn get-event-type [event]
  (get event event-type-json-key))



(def str->datastar-opt
  ;; SSE
  {"eventId"       d*/id
   "retryDuration" d*/retry-duration

   ;; patch elements
   "selector"          d*/selector
   "mode"         d*/patch-mode
   "useViewTransition" d*/use-view-transition

   ;; patch signals
   "onlyIfMissing" d*/only-if-missing

   ;; execute script
   "autoRemove" d*/auto-remove
   "attributes" d*/attributes})

(def options (set (keys str->datastar-opt)))

;; -----------------------------------------------------------------------------
;; Testing code: We want to send back the event that we received as datastar
;; signal values in the HTTP request
;; -----------------------------------------------------------------------------
(defn patch-elements! [sse event]
  (let [elements (get event "elements")
        opts (-> event
                 (select-keys options)
                 (set/rename-keys str->datastar-opt))]
    (d*/patch-elements! sse elements opts)))


(defn remove-element! [sse event]
  (let [selector (get event "selector")
        opts (-> event
                 (select-keys options)
                 (set/rename-keys str->datastar-opt))]
    (d*/remove-element! sse selector opts)))


(defn patch-signals! [sse event]
  (let [signals (-> (get event "signals")
                    (->> (into (sorted-map))) ;; for the purpose of the test, keys need to be ordered
                    (charred/write-json-str))

        signals-raw (get event "signals-raw")

        signals-r (if signals-raw signals-raw signals)

        opts (-> event
                 (select-keys options)
                 (set/rename-keys str->datastar-opt))]
    (d*/patch-signals! sse signals-r opts)))


(defn remove-signals! [sse event]
  (let [paths (get event "paths")
        opts (-> event
                 (select-keys options)
                 (set/rename-keys str->datastar-opt))]
    (d*/patch-signals! sse paths opts)))


(defn execute-script! [sse event]
  (let [script (get event "script")
        opts (-> event
                 (select-keys options)
                 (set/rename-keys str->datastar-opt)
                 (update d*/attributes update-keys keyword))]
    (d*/execute-script! sse script opts)))



(def dispatch
  {"patchElements"  patch-elements!
   "removeElements" remove-element!
   "patchSignals"   patch-signals!
   "removeSignals"  remove-signals!
   "executeScript"  execute-script!})


(defn send-event-back! [sse event]
  (let [type (get-event-type event)]
    ((dispatch type) sse event)))


(defn send-events-back! [sse req]
  (let [signals (get-signals req)]
    (doseq [event (get-events signals)]
      (send-event-back! sse event))))


;; -----------------------------------------------------------------------------
;; Setting up the endpoint for the shell tests
;; -----------------------------------------------------------------------------
(defn test-handler [req]
  (->sse-response req
    {on-open
     (fn [sse]
       (d*/with-open-sse sse
         (send-events-back! sse req)))}))


(def routes
  [["/test" {:handler test-handler
             :parameters {:multipart true}
             :middleware [rrm-multi-params/multipart-middleware]}]])


(def router (rr/router routes))


(def handler
  (rr/ring-handler router
                   (rr/create-default-handler)
                   {:middleware [rrm-params/parameters-middleware]}))

