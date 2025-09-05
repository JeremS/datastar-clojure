(ns tasks.cljdoc
  (:require
   [babashka.fs    :as fs]
   [babashka.tasks :as t]
   [clojure.string :as string]))


(def cljdoc-dir ".cljdoc-preview")

(defn home-dir [] (str (fs/home)))
(defn cwd      [] (str (fs/cwd)))

(defn git-rev []
  (-> (t/shell {:out :string} "git" "rev-parse" "HEAD")
      :out
      string/trim))


(defn try-docker-cmd [cmd]
  (try
    (t/shell {:out :string} cmd "--help")
    cmd
    (catch Exception _ nil)))


(def docker-cmd
  (or (try-docker-cmd "docker")
      (try-docker-cmd "podman")))


(defn start-server! []
  (fs/create-dirs cljdoc-dir)

  (t/shell
    docker-cmd "run"
    "--rm"
    "--publish" "8000:8000"
    "--volume" (str (home-dir) "/.m2:/root/.m2")
    "--volume" "./.cljdoc-preview:/app/data"
    "--platform" "linux/amd64"
    "cljdoc/cljdoc"))


(def libs
  #{:sdk :http-kit :ring :malli-schemas :brotli})


;; FIX: Get the version form somewhere
(defn ingest! [lib]
  (when (contains? libs lib)
    (t/shell
      docker-cmd "run"
      "--rm"
      "--volume" (str (home-dir) "/.m2:/root/.m2")
      "--volume" (str (cwd) ":/repo-to-import") 
      "--volume" "./.cljdoc-preview:/app/data"
      "--platform" "linux/amd64"
      "--entrypoint" "clojure"
      "cljdoc/cljdoc" "-Sforce" "-M:cli" "ingest"
      "--project" (str "dev.data-star.clojure/" (name lib))
      "--version" "1.0.0-RC1"
      "--git" "/repo-to-import"
      "--rev" (git-rev))))


(defn clean! []
  (fs/delete-tree cljdoc-dir))
