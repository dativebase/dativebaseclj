(ns dvb.common.openapi.spec.components.plan
  (:require [dvb.common.openapi.spec.components.common :as c]))

;; `Plan`
(def plan
  (let [id (c/id-property "plan")
        tier {:type :string
              :description "The tier of the plan. One of free, subscriber or supporter."
              :example "free"}
        created-at (c/created-at-property "plan")
        updated-at (c/updated-at-property "plan")
        destroyed-at (c/destroyed-at-property "plan")
        created-by (c/created-by-property "plan")
        updated-by (c/updated-by-property "plan")]
    {:type :object
     :properties {:id id
                  :tier tier
                  :created-at created-at
                  :updated-at updated-at
                  :destroyed-at destroyed-at
                  :created-by created-by
                  :updated-by updated-by}
     :required [:id
                :tier
                :created-at
                :updated-at
                :destroyed-at
                :created-by
                :updated-by]
     :example {:id (:example id)
               :tier (:example tier)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)
               :created-by (:example created-by)
               :updated-by (:example updated-by)}}))

;; `PlanWrite`
(def plan-write
  (-> plan
      (update :properties
              (fn [properties] (select-keys properties [:tier])))
      (assoc :required [:tier])
      (update :example
              (fn [example] (select-keys example [:tier])))))
