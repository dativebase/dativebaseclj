(ns dvb.server.http.operations.show-form
  (:require [dvb.server.db.forms :as db.forms]
            [dvb.server.http.authorize :as authorize]
            [dvb.server.http.operations.utils.declojurify :as declojurify]
            [dvb.server.log :as log]))

(defn handle
  [{:as _application :keys [database]}
   {:as ctx {old-slug :old_slug form-id :form_id} :path}]
  (log/info "Showing a form." {:form-id form-id :old-slug old-slug})
  (authorize/authorize ctx)
  (if-let [form (db.forms/get-form database form-id)]
    {:status 200
     :headers {}
     :body (-> form
               (select-keys [:id
                             :transcription
                             :old-slug
                             :created-at
                             :updated-at
                             :created-by])
               declojurify/form)}
    {:status 404
     :headers {}
     :body
     {:errors
      [{:message "The referenced form could not be found. Please ensure that the supplied identifier is correct."
        :error-code "entity-not-found"}]}}))
