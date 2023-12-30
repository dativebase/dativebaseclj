(ns dvb.common.openapi.spec.components.plan)

;; `Plan`
(def plan
  {:type :object
   :properties
   {:id {:type :string
         :format :uuid
         :description "The ID of the plan."
         :example "77b075f8-5bd5-4ff9-a714-ca8fdd1a7796"}
    :tier {:type :string
           :description "The tier of the plan. One of free, subscriber or supporter."
           :example "free"}
    :created-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the plan was created."
                 :example "2023-08-20T01:34:11.780Z"}
    :updated-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the plan was last updated."
                 :example "2023-08-20T01:34:11.780Z"}
    :destroyed-at {:type :string
                   :format :date-time
                   :nullable true
                   :description "The timestamp of when the plan was destroyed; NULL if the plan has not been destroyed."
                   :example nil}}
   :required [:id
              :tier
              :created-at
              :updated-at
              :destroyed-at]
   :example {:id "acddbb6a-31f6-41ac-90af-557d64082bcf"
             :tier "free"
             :created-at "2023-08-20T01:34:11.780Z"
             :updated-at "2023-08-20T01:34:11.780Z"
             :destroyed-at nil}})

;; `PlanWrite`
(def plan-write
  (-> plan
      (update :properties
              (fn [properties]
                (-> properties
                    (dissoc :id :created-at :updated-at :destroyed-at))))
      (assoc :required [:tier]
             :example {:tier "free"})))
