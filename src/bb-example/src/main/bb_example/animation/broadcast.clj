(ns bb-example.animation.broadcast
  (:require
    [bb-example.animation.rendering      :as rendering]
    [bb-example.animation.state          :as state]
    [starfederation.datastar.clojure.api :as d*]))


(defn send-frame! [sse frame]
  (try
    (d*/patch-elements! sse frame)
    (catch Exception e
      (println e))))


(defn broadcast-new-frame! [frame]
  (let [sses @state/!conns]
    (doseq [sse sses]
      (send-frame! sse frame))))


(defn install-watch! []
  (add-watch state/!state ::watch
             (fn [_k _ref old new]
               (when-not (identical? old new)
                 (let [frame (rendering/render-content new)]
                  (broadcast-new-frame! frame))))))

