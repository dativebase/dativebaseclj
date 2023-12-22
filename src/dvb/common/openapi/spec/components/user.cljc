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
    :is-superuser {:type :boolean
                   :description "Boolean indicating whether the user is a superuser. Only superusers can do things like create new OLDs and new users."
                   :example false
                   :default false}
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
              :destroyed-at
              :is-superuser]
   :example {:id "acddbb6a-31f6-41ac-90af-557d64082bcf"
             :first-name "Anne"
             :last-name "Boleyn"
             :email "ab@gmail.com"
             :is-superuser false
             :created-at "2023-08-20T01:34:11.780Z"
             :updated-at "2023-08-20T01:34:11.780Z"
             :destroyed-at nil}})

;; `UserWrite`
(def user-write
  (-> user
      (update :properties
              (fn [properties]
                (-> properties
                    (dissoc :id :created-at :updated-at :destroyed-at)
                    (assoc :password
                           {:type :string
                            :description "The password for the to-be-created user. This can never again be fetched from the API."
                            :example "8#$(6)496!8@{}sadfoiuqwerjasdfjlASDFASDFASDF"}))))
      (assoc :required [:first-name
                        :last-name
                        :email
                        :password
                        :is-superuser]
             :example {:first-name "Anne"
                       :last-name "Boleyn"
                       :email "ab@gmail.com"
                       :password "long-arbitrary-lots-of-entropy"
                       :is-superuser false})))

;; `UserUpdate`
(def user-update (dissoc user-write :required))

;; `PageOfUsers`
(def page-of-users
  {:type :object
   :properties
   {:data {:type :array
           :description "The users returned as response to a GET index type request."
           :items {:$ref "#/components/schemas/User"}}
    :meta {:type :object
           :properties {:count {:type :integer
                                :description "The count of all users in the database."
                                :example 1234}
                        :page {:type :integer
                               :description "The 0-based index of the page of users being returned. This value only makes sense given a count of users and the value of items-per-page."
                               :default 0
                               :example 0}
                        :items-per-page {:type :integer
                                         :enum [1 5 10 50]
                                         :description "The maximum number of users in a page of users."
                                         :default 10
                                         :example 10}}
           :required [:count :page :items-per-page]
           :example {:count 1234
                     :page 0
                     :items-per-page 10}}}
   :required [:data
              :meta]
   :example {:data [(:example user)]
             :meta {:count 1234
                    :page 0
                    :items-per-page 1}}})

(def new-user-data
  {:type :object
   :properties {}
   :example {}})

(def edit-user-data new-user-data)
