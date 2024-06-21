(ns dvb.common.openapi.schemas.user-plan
  (:require [dvb.common.openapi.schemas.common :as c]))

(def role
  {:type :string
   :enum ["manager"
          "member"]
   :description "The role of the referenced user on the referenced plan."
   :example "manager"})

;; `UserPlan`
(def user-plan
  (let [id c/user-plan-id-property
        user-id (assoc c/entity-id-property
                       :description "The ID of the user with the specified role on the referenced plan."
                       :example "8c5b541b-54e9-432b-9408-d274bb6015f3")
        plan-id (assoc c/entity-id-property
                       :description "The ID of the plan for which the referenced user has the specified role."
                       :example "a521a8e9-b7f7-4f84-8a4a-79b4a0377c57")
        role role
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

;; `UserPlanUpdate`
(def user-plan-update
  (let [upu-keys [:role]]
    (-> user-plan
        (update :properties (fn [properties] (select-keys properties upu-keys)))
        (assoc :required upu-keys)
        (update :example (fn [example] (select-keys example upu-keys))))))
