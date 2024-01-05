(ns dvb.server.http.operations.update-old
  (:require [dvb.common.edges :as edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.db.plans :as db.plans]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.log :as log]
            [dvb.server.http.operations.utils :as utils]))

(defn authorize-old-plan
  "Authorize the authenticated user to update this OLD to be paid for under the
  referenced plan."
  [database old-update authenticated-user]
  (when-let [plan-id (:plan-id old-update)]
    (let [plan (db.plans/get-plan-with-members database plan-id)]
      (when-not (authorize/user-authorized-to-manage-plan? authenticated-user plan)
        (let [data {:authenticated-user-id (:id authenticated-user)
                    :plan-id (:id plan)
                    :plan-members (:members plan)
                    :operation-id :update-old}]
          (log/warn "Authenticated user is not authorized to pay for this OLD under this plan."
                    data)
          (throw (errors/error-code->ex-info :unauthorized data)))))))

(defn validate [old-update existing-old old-slug]
  (when-not existing-old
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:entity-type :old
             :old-slug old-slug
             :operation :update-old})))
  (when (= old-update (select-keys existing-old (keys old-update)))
    (throw (errors/error-code->ex-info
            :no-changes-in-update
            {:request-payload old-update
             :entity-type :old
             :old-slug old-slug
             :operation :update-old}))))

(defn handle [{:keys [database]}
              {:as ctx old-update :request-body {old-slug :old_slug} :path}]
  (let [{:as authenticated-user authenticated-user-id :id} (utils/security-user ctx)]
    (log/info "Updating an OLD.")
    (authorize/authorize ctx)
    (authorize-old-plan database old-update authenticated-user)
    (let [old-update (edges/old-api->clj old-update)
          existing-old (db.olds/get-old database old-slug)
          update-fn (partial db.olds/update-old database)]
      (validate old-update existing-old old-slug)
      (try
        {:status 200
         :headers {}
         :body (-> existing-old
                   (merge old-update
                          {:updated-by authenticated-user-id})
                   (update :plan-id identity)
                   update-fn
                   edges/old-clj->api)}
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-update-internal-error
                  {:entity-type :old
                   :entity-to-update old-update}
                  e)))))))
