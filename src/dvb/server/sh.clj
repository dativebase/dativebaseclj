(ns dvb.server.sh
  (:require [clojure.java.shell :as sh]))

(defn lint-openapi
  ([] (lint-openapi "resources/public/openapi/api.yaml"))
  ([path]
   (let [{:keys [exit out] :as r}
         (sh/sh "spectral" "lint" path)]
     (when-not (= 0 exit)
       (println out)
       (throw (ex-info "OpenAPI YAML file is invalid" r))))))

(comment

  (lint-openapi)

)
