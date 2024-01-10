(ns dvb.common.openapi.schemas.old
  (:require [dvb.common.openapi.schemas.common :as c]
            [dvb.common.openapi.schemas.user-old :as user-old]))

(def slug
  {:type :string
   :description "The unique slug of the OLD. This should typically be an external identifier for a language. For example, 'fra' for French as that is the ISO 639-3 code for that language; see https://en.wikipedia.org/wiki/French_language."
   :example "fra"})

(def old-name
  {:type :string
   :description "A human-readable or descriptive name for the OLD. Typically, this is based on the language targeted by the OLD. For example, 'French OLD'."
   :example "French OLD"})

;; `OLD`
(def old
  (let [plan-id (assoc c/nullable-entity-id-property
                       :description "The ID of the plan that covers this OLD."
                       :example "0669c124-e05f-445f-9598-e4821c38f70d")
        created-at (c/created-at-property "OLD")
        updated-at (c/updated-at-property "OLD")
        destroyed-at (c/destroyed-at-property "OLD")
        created-by (c/created-by-property "OLD")
        updated-by (c/updated-by-property "OLD")
        users {:type :array
               :description "The users which have access to this OLD."
               :items {:$ref "#/components/schemas/UserOfOLD"}
               :example []}]
    {:type :object
     :properties
     {:slug slug
      :name old-name
      :plan-id plan-id
      :created-at created-at
      :updated-at updated-at
      :destroyed-at destroyed-at
      :created-by created-by
      :updated-by updated-by
      :users users}
     :required [:slug
                :name
                :plan-id
                :created-at
                :updated-at
                :destroyed-at
                :created-by
                :updated-by]
     :example {:slug (:example slug)
               :name (:example old-name)
               :plan-id (:example plan-id)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)
               :created-by (:example created-by)
               :updated-by (:example updated-by)
               :users (:example users)}}))

;; `OLDOfUser`
(def old-of-user
  {:type :object
   :properties {:role user-old/role
                :user-old-id c/user-old-id-property
                :slug slug
                :name old-name}
   :required [:role
              :user-old-id
              :slug
              :name]
   :example {:role (:example user-old/role)
             :user-old-id (:example c/user-old-id-property)
             :slug (:example slug)
             :name (:example old-name)}})

;; `OLDWrite`
(def old-write
  (-> old
      (update :properties
              (fn [properties] (select-keys properties
                                            [:slug
                                             :name
                                             :plan-id])))
      (assoc :required [:slug :name])
      (update :example
              (fn [example] (select-keys example [:slug
                                                  :name
                                                  :plan-id])))))

;; `OLDUpdate`
(def old-update
  (-> old-write
      (update :properties
              (fn [properties] (select-keys properties [:name
                                                        :plan-id])))
      (dissoc :required)
      (update :example
              (fn [example] (select-keys example [:name
                                                  :plan-id])))))

;; `:PageOfOLDs`
(def page-of-olds
  (c/page-of-entities-schema
   "OLDs"
   "OLD"
   (:example old)
   {:count-description "The count of all OLDs in DativeBase."
    :page-description
    (str "The 0-based index of the page of OLDs being returned. This value only"
         " makes sense given a count of OLDs in DativeBase and the value of"
         " items-per-page.")}))
