(ns dvb.server.http.operations.create-plan
  (:require [clojure.string :as str]
            [dvb.common.openapi.errors :as errors]
            [dvb.common.edges :as edges]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (org.postgresql.util PSQLException)))

(defn- create-plan [database plan-to-create]
  (let [data {:entity-type :plan
              :entity-to-create plan-to-create}
        throw-500 (fn [e] (throw (errors/error-code->ex-info
                                  :entity-creation-internal-error
                                  data e)))]
    (try
      (db.plans/create-plan database plan-to-create)
      (catch PSQLException e
        (if (str/includes? (ex-message e) "plans_email_key")
          (throw (errors/error-code->ex-info
                  :unique-email-constraint-violated
                  data))
          (throw-500 e)))
      (catch Exception e (throw-500 e)))))

(defn handle [{:keys [database]}
              {:as ctx plan-write :request-body}]
  (log/info "Creating a plan.")
  (authorize/authorize ctx)
  (let [created-by (utils/security-user-id ctx)
        response {:status 201
                  :headers {}
                  :body (->> (assoc plan-write
                                    :created-by created-by
                                    :updated-by created-by)
                             edges/plan-api->clj
                             (create-plan database)
                             edges/plan-clj->api)}]
    (log/info "Created a plan.")
    response))
