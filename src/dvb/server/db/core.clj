(ns dvb.server.db.core
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as csk-extras]
            [clojure.java.jdbc :as jdbc]
            [hugsql.adapter :as adapter :refer [result-one result-many]]
            [hugsql.core :refer [hugsql-result-fn]])
  (:import (java.sql Date Timestamp)))

;; See https://andersmurphy.com/2019/08/03/clojure-using-java-time-with-jdbc.html
(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _ _]
    (.toInstant v))
  java.sql.Date
  (result-set-read-column [v _ _]
    (.toLocalDate v)))

;; See https://andersmurphy.com/2019/08/03/clojure-using-java-time-with-jdbc.html
(extend-protocol jdbc/ISQLValue
  java.time.Instant
  (sql-value [v]
    (Timestamp/from v))
  java.time.LocalDate
  (sql-value [v]
    (Date/valueOf v)))

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
