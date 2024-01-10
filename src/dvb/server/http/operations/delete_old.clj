(ns dvb.server.http.operations.delete-old
  (:require [dvb.common.edges.olds :as old-edges]
            [dvb.common.openapi.errors :as errors]
            [dvb.server.db.olds :as db.olds]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils :as utils]
            [dvb.server.log :as log]))

(defn validate [existing-old old-slug]
  (when-not existing-old
    (throw (errors/error-code->ex-info
            :entity-not-found
            {:entity-type :old
             :old-slug old-slug
             :operation :delete-old}))))

(defn handle [{:keys [database]}
              {:as ctx {old-slug :old_slug} :path}]
  (let [{:as authenticated-user authenticated-user-id :id}
        (utils/security-user ctx)
        log-ctx {:authenticated-user-id authenticated-user-id
                 :old-slug old-slug}]
    (log/info "Deleting an OLD." log-ctx)
    (authorize/authorize ctx)
    (let [existing-old (db.olds/get-old-with-users database old-slug)]
      (validate existing-old old-slug)
      (authorize/authorize-mutate-old
       :delete-old existing-old authenticated-user)
      (try
        (let [deleted-old (old-edges/clj->api (db.olds/delete-old
                                               database
                                               {:slug old-slug
                                                :updated-by authenticated-user-id}))]
          (log/info "Deleted an OLD." log-ctx)
          {:status 200
           :headers {}
           :body deleted-old})
        (catch Exception e
          (throw (errors/error-code->ex-info
                  :entity-deletion-internal-error
                  {:entity-type :old
                   :old-slug old-slug}
                  e)))))))
