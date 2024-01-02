(ns dvb.common.openapi.schemas.plan
  (:require [dvb.common.openapi.schemas.common :as c]
            [dvb.common.openapi.schemas.user-plan :as user-plan]))

(def tier
  {:type :string
   :description "The tier of the plan. One of free, subscriber or supporter."
   :example "free"})

;; `Plan`
(def plan
  (let [id c/plan-id-property
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

;; PlanOfUser
(def plan-of-user
  (let [id c/plan-id-property]
    {:type :object
     :properties {:id id
                  :role user-plan/role
                  :tier tier}
     :required [:id
                :role
                :tier]
     :example {:id (:example id)
               :role (:example user-plan/role)
               :tier (:example tier)}}))

;; `PlanWrite`
(def plan-write
  (-> plan
      (update :properties
              (fn [properties] (select-keys properties [:tier])))
      (assoc :required [:tier])
      (update :example
              (fn [example] (select-keys example [:tier])))))

;; `Plans`
(def plans
  {:type :array
   :description "An array of plans."
   :items {:$ref "#/components/schemas/Plan"}})
