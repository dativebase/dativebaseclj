(ns dvb.server.http.operations.delete-plan
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.db.users :as db.users]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log])
  (:import (java.util UUID)))

(defn- validate
  [{:as existing-plan :keys [olds] plan-id :id}]
  (when-not existing-plan
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:entity-type :plan
             :entity-id plan-id
             :operation :delete-plan})))
  (when (seq olds)
    (let [message "Refusing to delete a plan that is supporting OLDs. Remove the OLDs from the plan first."
          data {:message message
                :plan-id plan-id
                :olds olds
                :operation :delete-plan}]
      (log/warn message data)
      (throw (errors/error-code->ex-info :plan-with-olds-not-delible data)))))

(defn handle [{:keys [database]}
              {:as ctx {plan-id :plan_id} :path}]
  (let [plan-id (UUID/fromString plan-id)
        authenticated-user-id (utils/security-user-id ctx)
        authenticated-user (db.users/get-user-with-plans database
                                                         authenticated-user-id)
        log-ctx {:plan-id plan-id
                 :authenticated-user-id authenticated-user-id}]
    (log/info "Deleting a plan." log-ctx)
    (authorize/authorize ctx)
    (let [existing-plan (db.plans/get-plan-with-olds-and-members database plan-id)]
      (validate existing-plan)
      (authorize/authorize-mutate-plan
       :delete-plan existing-plan authenticated-user)
      (try (let [deleted-plan (edges/plan-clj->api
                               (db.plans/delete-plan
                                database
                                {:id plan-id
                                 :updated-by authenticated-user-id}))]
             (log/info "Deleted a plan." log-ctx)
             {:status 200
              :headers {}
              :body deleted-plan})
           (catch Exception e
             (throw (errors/error-code->ex-info
                     :entity-deletion-internal-error
                     (merge log-ctx {:entity-type :plan
                                     :entity-id plan-id})
                     e)))))))
