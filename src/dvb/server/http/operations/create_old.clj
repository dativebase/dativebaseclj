(ns dvb.server.http.operations.create-old
  (:require [clojure.string :as str]
            [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (org.postgresql.util PSQLException)))

(defn- create-old [database old-to-create]
  (let [data {:entity-type :old
              :entity-to-create old-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.olds/create-old database old-to-create)
      (catch PSQLException e
        (if (str/includes? (ex-message e) "olds_pkey")
          (throw (errors/error-code->ex-info
                  :unique-slug-constraint-violated
                  data))
          (throw-500 e)))
      (catch Exception e (throw-500 e)))))

(defn handle [{:keys [database]}
              {:as ctx old-write :request-body}]
  (let [created-by (utils/security-user-id ctx)]
    (log/info "Creating an OLD.")
    (authorize/authorize ctx)
    (let [creator (partial create-old database)
          response {:status 201
                    :headers {}
                    :body (-> old-write
                              edges/old-api->clj
                              (assoc :created-by created-by
                                     :updated-by created-by)
                              creator
                              edges/old-clj->api)}]
      (log/info "Created an OLD.")
      response)))
