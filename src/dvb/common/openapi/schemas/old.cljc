(ns dvb.common.openapi.schemas.old)

;; `OLD`
(def old
  {:type :object
   :properties
   {:slug {:type :string
           :description "The unique slug of the OLD. This should typically be an external identifier for a language. For example, 'fra' for French as that is the ISO 639-3 code for that language; see https://en.wikipedia.org/wiki/French_language."
           :example "fra"}
    :name {:type :string
           :description "A human-readable or descriptive name for the OLD. Typically, this is based on the language targeted by the OLD. For example, 'French OLD'."
           :example "French OLD"}
    :created-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the OLD was created."
                 :example "2023-08-20T01:34:11.780Z"}
    :updated-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the OLD was last updated."
                 :example "2023-08-20T01:34:11.780Z"}
    :destroyed-at {:type :string
                   :format :date-time
                   :nullable true
                   :description "The timestamp of when the OLD was destroyed; NULL if the OLD has not been destroyed."
                   :example nil}}
   :required [:slug
              :name
              :created-at
              :updated-at
              :destroyed-at]
   :example {:slug "fra"
             :name "French OLD"
             :created-at "2023-08-20T01:34:11.780Z"
             :updated-at "2023-08-20T01:34:11.780Z"
             :destroyed-at nil}})
