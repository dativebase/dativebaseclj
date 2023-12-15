(ns dvb.server.time
  (:import (java.time.format DateTimeFormatter)
           (java.time LocalDate Instant)
           (java.io FileWriter)))

(defn parse-date [string]
  (LocalDate/parse string))

(defn parse-time [string]
  (and string (-> (.parse (DateTimeFormatter/ISO_INSTANT) string)
                  Instant/from)))

;; The following two dispatches make java.time.Instant print with a reader macro,
;; e.g., (java-time.api/instant) => #time/inst "2023-11-11T16:16:58.821829Z"
(defmethod print-method Instant
  [inst out]
  (.write out (str "#time/inst \"" (.toString inst) "\"") ))

(defmethod print-dup Instant
  [inst out]
  (.write out (str "#time/inst \"" (.toString inst) "\"") ))

;; The following two dispatches make java.time.LocalDate print with a reader macro,
;; e.g., (java-time.api/local-date) => #time/ld "2023-11-11"
(defmethod print-method LocalDate
  [^LocalDate date ^FileWriter out]
  (.write out (str "#time/ld \"" (.toString date) "\"")))

(defmethod print-dup LocalDate
  [^LocalDate date ^FileWriter out]
  (.write out (str "#time/ld \"" (.toString date) "\"")))
