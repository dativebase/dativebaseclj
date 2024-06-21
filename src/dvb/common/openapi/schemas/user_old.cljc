(ns dvb.common.openapi.schemas.user-old
  (:require [dvb.common.openapi.schemas.common :as c]))

(def role
  {:type :string
   :enum ["viewer"
          "contributor"
          "administrator"]
   :description "The role of the referenced user on the referenced OLD."
   :example "contributor"})

;; `UserOLD`
(def user-old
  (let [id c/user-old-id-property
        user-id (assoc c/entity-id-property
                       :description "The ID of the user with the specified role on the referenced OLD."
                       :example "8c5b541b-54e9-432b-9408-d274bb6015f3")
        old-slug {:type :string
                  :description "The slug of the OLD for which the referenced user has the specified role."
                  :example "lan-old"}
        role role
        created-at (c/created-at-property "user OLD")
        updated-at (c/updated-at-property "user OLD")
        destroyed-at (c/destroyed-at-property "user OLD")
        created-by (c/created-by-property "user OLD")
        updated-by (c/updated-by-property "user OLD")]
    {:type :object
     :properties {:id id
                  :user-id user-id
                  :old-slug old-slug
                  :role role
                  :created-at created-at
                  :updated-at updated-at
                  :destroyed-at destroyed-at
                  :created-by created-by
                  :updated-by updated-by}
     :required [:id
                :user-id
                :old-slug
                :role
                :created-at
                :updated-at
                :destroyed-at
                :created-by
                :updated-by]
     :example {:id (:example id)
               :user-id (:example user-id)
               :old-slug (:example old-slug)
               :role (:example role)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)
               :created-by (:example created-by)
               :updated-by (:example updated-by)}}))

;; `UserOLDWrite`
(def user-old-write
  (let [uow-keys [:user-id :old-slug :role]]
    (-> user-old
        (update :properties (fn [properties] (select-keys properties uow-keys)))
        (assoc :required uow-keys)
        (update :example (fn [example] (select-keys example uow-keys))))))

;; `UserOLDUpdate`
(def user-old-update
  (let [uou-keys [:role]]
    (-> user-old
        (update :properties (fn [properties] (select-keys properties uou-keys)))
        (assoc :required uou-keys)
        (update :example (fn [example] (select-keys example uou-keys))))))
