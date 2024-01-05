(ns dvb.server.http.operations.create-old
  (:require [clojure.string :as str]
            [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.plans :as db.plans]
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

(defn authorize-old-plan
  "Authorize the authenticated user to create this OLD under the referenced
  plan."
  [database old-write authenticated-user]
  (when-let [plan-id (:plan-id old-write)]
    (let [plan (db.plans/get-plan-with-members database plan-id)]
      (when-not (authorize/user-authorized-to-manage-plan? authenticated-user plan)
        (let [data {:authenticated-user-id (:id authenticated-user)
                    :plan-id (:id plan)
                    :plan-members (:members plan)
                    :operation-id :create-old}]
          (log/warn "Authenticated user is not authorized to pay for this OLD under this plan."
                    data)
          (throw (errors/error-code->ex-info :unauthorized data)))))))

(defn handle [{:keys [database]}
              {:as ctx old-write :request-body}]
  (let [authenticated-user (utils/security-user ctx)
        authenticated-user-id (:id authenticated-user)]
    (log/info "Creating an OLD.")
    (authorize/authorize ctx)
    (authorize-old-plan database old-write authenticated-user)
    (let [creator (partial create-old database)
          response {:status 201
                    :headers {}
                    :body (-> old-write
                              edges/old-api->clj
                              (update :plan-id identity)
                              (assoc :created-by authenticated-user-id
                                     :updated-by authenticated-user-id)
                              creator
                              edges/old-clj->api)}]
      (log/info "Created an OLD.")
      response)))
