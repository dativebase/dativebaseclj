(ns dvb.server.entities.olds
  (:require [com.stuartsierra.component.repl :as component.repl]
            [dvb.server.db.olds :as db.olds]))

(defn get-normalized-representation-of-old [_db-conn _old-slug]
  :TODO)

;; Scratch code below:
(comment

  (def db (:database component.repl/system))

  (db.olds/get-olds db) ;; ( ,,, )

  (db.olds/count-olds db) ;; 851

  (def target-old-slug "42081e2242e94b8c927295890644d3f9")

  (def target-old-1 (first (db.olds/get-olds db 1 850)))

  [{:updated-at #time/inst "2024-02-16T14:18:06.967569Z",
    :slug "42081e2242e94b8c927295890644d3f9",
    :name "lY6eNICH7Ah92x7F9d3Om74",
    :plan-id nil,
    :updated-by #uuid "bf9e8bb5-fd90-4020-9737-22432874d412",
    :created-by #uuid "bf9e8bb5-fd90-4020-9737-22432874d412",
    :destroyed-at nil,
    :inserted-at #time/inst "2024-02-16T14:18:06.967569Z",
    :created-at #time/inst "2024-02-16T14:18:06.967569Z"}]

  (= target-old-slug (:slug target-old-1)) ;; true

  (def target-old-2 (db.olds/get-old db target-old-slug))

  (= (keyword target-old-slug)
     (:slug target-old-2)) ;; true

  (get-normalized-representation-of-old :db-conn :slug)

)
