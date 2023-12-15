(ns dvb.common.openapi.spec.components.user)

;; `User`
(def user
  {:type :object
   :properties
   {:id {:type :string
         :format :uuid
         :description "The ID of the user."
         :example "77b075f8-5bd5-4ff9-a714-ca8fdd1a7796"}
    :first-name {:type :string
                 :description "The first name of the user."
                 :example "Anne"}
    :last-name {:type :string
                :description "The last name of the user."
                :example "Boleyn"}
    :email {:type :string
            :format :email
            :description "The email address of the user."
            :example "ab@gmail.com"}
    :created-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the user was created."
                 :example "2023-08-20T01:34:11.780Z"}
    :updated-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the user was last updated."
                 :example "2023-08-20T01:34:11.780Z"}
    :destroyed-at {:type :string
                   :format :date-time
                   :nullable true
                   :description "The timestamp of when the user was destroyed; NULL if the user has not been destroyed."
                   :example nil}}
   :required [:id
              :first-name
              :last-name
              :email
              :created-at
              :updated-at
              :destroyed-at]
   :example {:id "acddbb6a-31f6-41ac-90af-557d64082bcf"
             :first-name "Anne"
             :last-name "Boleyn"
             :email "ab@gmail.com"
             :created-at "2023-08-20T01:34:11.780Z"
             :updated-at "2023-08-20T01:34:11.780Z"
             :destroyed-at nil}})
