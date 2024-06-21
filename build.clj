(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))

(def basis (b/create-basis {:project "deps.edn"}))
(def app-name "dativebase")
(def uber-file-name (format "%s/%s-standalone.jar" build-folder app-name))

(defn clean [_]
  (b/delete {:path "target"})
  (println (format "Build folder \"%s\" removed" build-folder)))

(defn uber [_]
  (clean nil)
  ;; copy resources
  (b/copy-dir {:src-dirs   ["resources"]
               :target-dir jar-content})
  ;; compile the Clojure code
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir jar-content})
  ;; create the uber file
  (b/uber {:class-dir jar-content
           :uber-file uber-file-name
           :basis     basis
           ;; here we specify the entry point for uberjar:
           :main     'dvb.server.core})
  (println (format "Uber file created: \"%s\"" uber-file-name)))

