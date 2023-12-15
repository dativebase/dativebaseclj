(ns dvb.server.db.test-queries
  (:require [hugsql.core :as hugsql]))

(declare delete-all-events*
         delete-all-forms*
         delete-all-machine-users*
         delete-all-olds*
         delete-all-api-keys*
         delete-all-users*
         delete-all-users-olds*)

(hugsql/def-db-fns "sql/test.sql")

(def delete-all-events delete-all-events*)
(def delete-all-forms delete-all-forms*)
(def delete-all-machine-users delete-all-machine-users*)
(def delete-all-olds delete-all-olds*)
(def delete-all-api-keys delete-all-api-keys*)
(def delete-all-users delete-all-users*)
(def delete-all-users-olds delete-all-users-olds*)

(defn delete-all-the-things [db-conn]
  (delete-all-forms db-conn)
  (delete-all-machine-users db-conn)
  (delete-all-api-keys db-conn)
  (delete-all-users-olds db-conn)
  (delete-all-users db-conn)
  (delete-all-olds db-conn)
  (delete-all-events db-conn))
