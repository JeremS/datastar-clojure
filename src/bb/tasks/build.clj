(ns tasks.build
  (:require
    [babashka.fs    :as fs]
    [babashka.tasks :as t]))

;; -----------------------------------------------------------------------------
;; Lib dirs
;; -----------------------------------------------------------------------------
(def sdk-dir                  "sdk")
(def sdk-adapter-http-kit-dir "sdk-adapter-http-kit")
(def sdk-adapter-ring-dir     "sdk-adapter-ring")
(def sdk-brotli-dir           "sdk-brotli")
(def sdk-malli-schemas-dir    "sdk-malli-schemas")


(def sdk-lib-dirs
  [sdk-dir
   sdk-adapter-ring-dir
   sdk-adapter-http-kit-dir
   sdk-brotli-dir
   sdk-malli-schemas-dir])


(def maven-dir
  (str (fs/path (fs/home) ".m2" "repository" "dev" "data-star" "clojure")))


(defn clean-maven-dir!
  "Deletes `~/.m2/repository/dev/data-star/clojure`."
  []
  (fs/delete-tree maven-dir))


;; -----------------------------------------------------------------------------
;; Tasks
;; -----------------------------------------------------------------------------
(defn lib-bump! [dir component]
  (when-not (contains? #{"major" "minor" "patch"} component)
    (println (str "ERROR: First argument must be one of: major, minor, patch. Got: " (or component "nil")))
    (System/exit 1))
  (t/shell {:dir dir} (str "neil version " component " --no-tag")))


(defn lib-set-version! [dir version]
  (t/shell {:dir dir} (str "neil version set " version " --no-tag")))


(defn lib-install! [dir]
  (t/clojure {:dir dir} "-T:build install"))


(defn lib-clean! [dir]
  (t/clojure {:dir dir} "-T:build clean"))


(defn lib-jar! [dir]
  (t/clojure {:dir dir} "-T:build jar"))


(defn lib-publish! [dir]
  (t/clojure {:dir dir} "-T:build deploy"))

