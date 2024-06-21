(ns dvb.common.openapi.schemas.old-access-request
  (:require [dvb.common.openapi.schemas.common :as c]))

;; `OLDAccessRequest`
(def old-access-request
  (let [id c/old-access-request-id-property
        user-id (assoc c/entity-id-property
                       :description "The ID of the user requesting access to the target OLD."
                       :example "460b2daa-4839-4732-8368-2ed6179d5c52")
        old-slug {:type :string
                  :description "The slug of the OLD to which the user is requesting access."
                  :example "fra"}
        status {:type :string
                :enum ["pending"
                       "approved"
                       "rejected"
                       "retracted"]
                :description "The status of the OLD access request."
                :example "pending"}
        created-at (c/created-at-property "old access request")]
    {:type :object
     :properties
     {:id id
      :user-id user-id
      :old-slug old-slug
      :status status
      :created-at created-at}
     :required [:id
                :user-id
                :old-slug
                :status
                :created-at]
     :example {:id (:example id)
               :user-id (:example user-id)
               :old-slug (:example old-slug)
               :status (:example status)
               :created-at (:example created-at)}}))

;; `OLDAccessRequestWrite`
(def old-access-request-write
  (-> old-access-request
      (update :properties (fn [p] (select-keys p [:user-id :old-slug])))
      (assoc :required [:user-id :old-slug])
      (update :example (fn [e] (select-keys e [:user-id :old-slug])))))

;; `OLDAccessRequests`
(def old-access-requests
  {:type :array
   :description "The pending access requests for a specified OLD."
   :items {:$ref "#/components/schemas/OLDAccessRequest"}
   :example []})

