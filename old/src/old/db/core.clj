(ns old.db.core
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]
            [hugsql.adapter :as adapter :refer [result-one result-many]]
            [hugsql.core :refer [hugsql-result-fn]]))

(defn result-one-snake->kebab
  "Converts a single key formatted in snake_case to kebab-case"
  [this result options]
  (->> (result-one this result options)
       (csk-extras/transform-keys csk/->kebab-case-keyword)))

(defn result-many-snake->kebab
  "Converts multiple keys formatted in snake_case to kebab-case"
  [this result options]
  (->> (result-many this result options)
       (map (partial csk-extras/transform-keys csk/->kebab-case-keyword))))

(defmethod hugsql-result-fn :one-kebab [_sym] `result-one-snake->kebab)
(defmethod hugsql-result-fn :many-kebab [_sym] `result-many-snake->kebab)

(defn return-kebab [db-return-values]
  (csk-extras/transform-keys csk/->kebab-case-keyword
                             (first db-return-values)))
