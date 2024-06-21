(ns dvb.server.http.openapi.serialize
  "Functions for serializing a Clojure map representing an OpenAPI schema to
   well-formed OpenAPI v3 YAML or JSON."
  (:require [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [clojure.string :as str]
            [dvb.common.openapi.serialize :as serialize]
            [dvb.common.openapi.spec :as spec]
            [dvb.server.utils :as utils]
            [dvb.server.log :as log]))

(defn clojure-openapi->json-string
  "Given a Clojure map `clojure-openapi` that is a well-formed OpenAPI object,
   convert it to a JSON string and return said string."
  [clojure-openapi]
  (-> clojure-openapi
      serialize/clj->openapi
      (json/generate-string {:pretty true})))

(defn clojure-openapi->json-on-disk
  "Given a Clojure map `clojure-openapi` that is a well-formed OpenAPI object,
   convert it to a JSON string and write it to disk at `path`."
  [clojure-openapi path]
  (spit path (clojure-openapi->json-string clojure-openapi)))

(defn clojure-openapi->yaml-string
  "Given a Clojure map `clojure-openapi` that is a well-formed OpenAPI object,
   convert it to a YAML string and return said string."
  [clojure-openapi]
  (-> clojure-openapi
      serialize/clj->openapi
      (yaml/generate-string :flow-style :block)))

(defn clojure-openapi->disk
  "Given a Clojure map `clojure-openapi` that is a well-formed OpenAPI object,
   convert it to a YAML string and write it to disk at `path`."
  [clojure-openapi path]
  (spit path (clojure-openapi->yaml-string clojure-openapi)))

(defn -main
  "Entrypoint for tools.deps alias `openapi`, eg `clj -X:openapi`.
   This lets us serialize the Clojure OpenAPI spec at `common.openapi.spec/api` to
   YAML and write it to disk under `resources/`."
  ([] (-main "{}"))
  ([opts]
   (let [opts (utils/parse-main-opts opts)
         [format-k writer] (if (= :json (:format opts))
                             [:json clojure-openapi->json-on-disk]
                             [:yaml clojure-openapi->disk])
         filename (or (:name opts) "api")
         format-h (str/upper-case (name format-k))
         openapi-path (format "resources/public/openapi/%s.%s" filename (name format-k))]
     (log/info (format "Converting OpenAPI spec at `dvb.common.openapi.spec/api` to %s and writing it to disk at %s."
                       format-h openapi-path))
     (try (writer spec/api openapi-path)
          (log/info (format "Converted OpenAPI spec at `dvb.common.openapi.spec/api` to %s and wrote it to disk at %s."
                            format-h openapi-path))
          (catch Exception _
            (log/error (format "Failed to convert Clojure OpenAPI specification to %s." format-h))
            (java.lang.System/exit 1))))))
