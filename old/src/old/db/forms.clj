(ns old.db.forms
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [hugsql.core :as hugsql]
            [old.db.events :as events]
            [old.db.utils :as utils]))

(declare count-forms*
         create-form*
         delete-form*
         get-form*
         get-forms*
         update-form*)

(hugsql/def-db-fns "sql/forms.sql")

(defn get-form [db-conn id] (get-form* db-conn {:id id}))

(defn- form-row->form-entity [form-row]
  (-> form-row
      utils/db-row->entity
      (set/rename-keys {:created_by_user_id :created-by-user-id
                        :old_slug :old-slug})))

(defn- mutate [mutation db-conn form]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [[db-form] ((case mutation
                       :create create-form*
                       :update update-form*
                       :delete delete-form*) tconn form)
          form (form-row->form-entity db-form)]
      (events/create-event tconn (utils/mutation form "forms"))
      form)))

(def create-form (partial mutate :create))

(def update-form (partial mutate :update))

(def delete-form (partial mutate :delete))

(defn get-history [db-conn form]
  (events/get-history db-conn (:old-slug form) "forms" (:id form)))

(defn get-forms
  ([db-conn old-slug] (get-forms db-conn old-slug 10))
  ([db-conn old-slug limit] (get-forms db-conn old-slug limit 0))
  ([db-conn old-slug limit offset]
   (mapv form-row->form-entity
         (get-forms* db-conn {:old-slug old-slug
                              :limit limit
                              :offset offset}))))

(defn count-forms [db-conn old-slug]
  (:form-count (count-forms* db-conn {:old-slug old-slug})))
