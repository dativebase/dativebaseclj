(ns dvb.common.openapi.spec.components.user-plan
  (:require [dvb.common.openapi.spec.components.common :as c]))

;; `UserPlan`
(def user-plan
  (let [id (c/id-property "user plan")
        user-id (assoc c/entity-id-property
                       :description
                       "The ID of the user with the specified role on the referenced plan.")
        plan-id (assoc c/entity-id-property
                       :description
                       "The ID of the plan for which the referenced user has the specified role.")
        role {:type :string
              :enum ["manager"
                     "member"]
              :description "The role of the referenced user on the referenced plan."
              :example "manager"}
        created-at (c/created-at-property "user plan")
        updated-at (c/updated-at-property "user plan")
        destroyed-at (c/destroyed-at-property "user plan")
        created-by (c/created-by-property "user plan")
        updated-by (c/updated-by-property "user plan")]
    {:type :object
     :properties {:id id
                  :user-id user-id
                  :plan-id plan-id
                  :role role
                  :created-at created-at
                  :updated-at updated-at
                  :destroyed-at destroyed-at
                  :created-by created-by
                  :updated-by updated-by}
     :required [:id
                :user-id
                :plan-id
                :role
                :created-at
                :updated-at
                :destroyed-at
                :created-by
                :updated-by]
     :example {:id (:example id)
               :user-id (:example user-id)
               :plan-id (:example plan-id)
               :role (:example role)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)
               :created-by (:example created-by)
               :updated-by (:example updated-by)}}))

;; `UserPlanWrite`
(def user-plan-write
  (let [upw-keys [:user-id :plan-id :role]]
    (-> user-plan
        (update :properties (fn [properties] (select-keys properties upw-keys)))
        (assoc :required upw-keys)
        (update :example (fn [example] (select-keys example upw-keys))))))
