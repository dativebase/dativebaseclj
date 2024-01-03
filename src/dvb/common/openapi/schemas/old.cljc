(ns dvb.common.openapi.schemas.old
  (:require [dvb.common.openapi.schemas.common :as c]))

;; `OLD`
(def old
  (let [slug {:type :string
              :description "The unique slug of the OLD. This should typically be an external identifier for a language. For example, 'fra' for French as that is the ISO 639-3 code for that language; see https://en.wikipedia.org/wiki/French_language."
              :example "fra"}
        name {:type :string
              :description "A human-readable or descriptive name for the OLD. Typically, this is based on the language targeted by the OLD. For example, 'French OLD'."
              :example "French OLD"}
        created-at (c/created-at-property "OLD")
        updated-at (c/updated-at-property "OLD")
        destroyed-at (c/destroyed-at-property "OLD")
        created-by (c/created-by-property "OLD")
        updated-by (c/updated-by-property "OLD")]
    {:type :object
     :properties
     {:slug slug
      :name name
      :created-at created-at
      :updated-at updated-at
      :destroyed-at destroyed-at
      :created-by created-by
      :updated-by updated-by}
     :required [:slug
                :name
                :created-at
                :updated-at
                :destroyed-at
                :created-by
                :updated-by]
     :example {:slug (:example slug)
               :name (:example name)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)
               :created-by (:example created-by)
               :updated-by (:example updated-by)}}))

;; `OLDWrite`
(def old-write
  (-> old
      (update :properties
              (fn [properties] (select-keys properties [:slug :name])))
      (assoc :required [:slug :name])
      (update :example
              (fn [example] (select-keys example [:slug :name])))))

;; `OLDUpdate`
(def old-update
  (-> old-write
      (update :properties
              (fn [properties] (select-keys properties [:name])))
      (assoc :required [:name])
      (update :example
              (fn [example] (select-keys example [:name])))))

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
