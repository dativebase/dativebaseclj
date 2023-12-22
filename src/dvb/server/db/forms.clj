(ns dvb.server.db.forms
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [dvb.server.db.events :as events]
            [dvb.server.db.utils :as utils]))

(declare count-forms*
         create-form*
         delete-form*
         get-form*
         get-forms*
         update-form*)

(hugsql/def-db-fns "sql/forms.sql")

(defn get-form [db-conn id] (get-form* db-conn {:id id}))

(defn- mutate [mutation db-conn form]
  (jdbc/with-db-transaction [tconn db-conn]
    (let [form ((case mutation
                  :create create-form*
                  :update update-form*
                  :delete delete-form*) tconn form)]
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
   (vec (get-forms* db-conn {:old-slug old-slug
                             :limit limit
                             :offset offset}))))

(defn count-forms [db-conn old-slug]
  (:form-count (count-forms* db-conn {:old-slug old-slug})))
