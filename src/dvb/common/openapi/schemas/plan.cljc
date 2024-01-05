(ns dvb.common.openapi.schemas.plan
  (:require [dvb.common.openapi.schemas.common :as c]
            [dvb.common.openapi.schemas.user-plan :as user-plan]))

(def tier
  {:type :string
   :enum ["free"
          "subscriber"
          "supporter"]
   :description "The tier of the plan. One of free, subscriber or supporter."
   :example "supporter"})

(def write-tier
  {:type :string
   :enum ["free"]
   :description "The tier of the plan during initial plan creation. When a plan is created, its tier must start out as free. Only a billing event can elevate the plan tier."
   :example "free"})

;; `Plan`
(def plan
  (let [id c/plan-id-property
        created-at (c/created-at-property "plan")
        updated-at (c/updated-at-property "plan")
        destroyed-at (c/destroyed-at-property "plan")
        created-by (c/created-by-property "plan")
        updated-by (c/updated-by-property "plan")
        members {:type :array
                 :description "The users which are members of this plan."
                 :items {:$ref "#/components/schemas/MemberOfPlan"}
                 :example []}]
    {:type :object
     :properties {:id id
                  :tier tier
                  :created-at created-at
                  :updated-at updated-at
                  :destroyed-at destroyed-at
                  :created-by created-by
                  :updated-by updated-by
                  :members members}
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
               :updated-by (:example updated-by)
               :members (:example members)}}))

;; PlanOfUser
(def plan-of-user
  (let [id c/plan-id-property
        user-plan-id c/user-plan-id-property]
    {:type :object
     :properties {:id id
                  :user-plan-id user-plan-id
                  :role user-plan/role
                  :tier tier}
     :required [:id
                :user-plan-id
                :role
                :tier]
     :example {:id (:example id)
               :user-plan-id (:example user-plan-id)
               :role (:example user-plan/role)
               :tier (:example tier)}}))

;; `PlanWrite`
(def plan-write
  (-> plan
      (assoc :properties {:tier write-tier}
             :required [:tier]
             :example {:tier (:example write-tier)})))

;; `Plans`
(def plans
  {:type :array
   :description "An array of plans."
   :items {:$ref "#/components/schemas/Plan"}})
