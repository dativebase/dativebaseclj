(ns old.http.operations.utils)

(defn declojurify-form [form]
  (-> form
      (update :id str)))

(defn api-key-user-id [ctx]
  (-> ctx :security :api-key :user :id))
