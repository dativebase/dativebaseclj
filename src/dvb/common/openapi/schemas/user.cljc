(ns dvb.common.openapi.schemas.user
  (:require [dvb.common.openapi.schemas.common :as c]
            [dvb.common.openapi.schemas.user-old :as user-old]
            [dvb.common.openapi.schemas.user-plan :as user-plan]))

(def registration-status
  {:type :string
   :enum ["pending"
          "registered"
          "deactivated"]
   :description "The registration status of the user. All users start out as pending. Successful activation transitions the user to registered. If the user is deactivated, its registration status becomes deactivated."
   :example "registered"})

;; `User`
(def user
  (let [id c/user-id-property
        created-at (c/created-at-property "user")
        updated-at (c/updated-at-property "user")
        destroyed-at (c/destroyed-at-property "user")
        created-by (c/nullable-created-by-property "user")
        updated-by (c/nullable-updated-by-property "user")
        first-name {:type :string
                    :description "The first name of the user."
                    :example "Anne"}
        last-name {:type :string
                   :description "The last name of the user."
                   :example "Boleyn"}
        email {:type :string
               :format :email
               :description "The email address of the user."
               :example "ab@gmail.com"}
        is-superuser {:type :boolean
                      :description "Boolean indicating whether the user is a superuser. Only superusers can do things like create new OLDs and new users."
                      :example false
                      :default false}
        plans {:type :array
               :description "The plans to which this user has access."
               :items {:$ref "#/components/schemas/PlanOfUser"}
               :example []}]
    {:type :object
     :properties
     {:id id
      :created-at created-at
      :updated-at updated-at
      :destroyed-at destroyed-at
      :created-by created-by
      :updated-by updated-by
      :is-superuser is-superuser
      :email email
      :last-name last-name
      :first-name first-name
      :plans plans
      :registration-status registration-status}
     :required [:id
                :first-name
                :last-name
                :created-at
                :updated-at
                :destroyed-at
                :created-by
                :updated-by]
     :example {:id (:example id)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)
               :created-by (:example created-by)
               :updated-by (:example updated-by)
               :is-superuser (:example is-superuser)
               :email (:example email)
               :last-name (:example last-name)
               :first-name (:example first-name)
               :plans (:example plans)
               :registration-status (:example registration-status)}}))

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

;; MemberOfPlan
(def member-of-plan
  (let [id c/user-id-property
        user-plan-id c/user-plan-id-property]
    {:type :object
     :properties {:id id
                  :user-plan-id user-plan-id
                  :role user-plan/role}
     :required [:id
                :user-plan-id
                :role]
     :example {:id (:example id)
               :user-plan-id (:example user-plan-id)
               :role (:example user-plan/role)}}))

;; UserOfOLD
(def user-of-old
  (let [id c/user-id-property
        user-old-id c/user-old-id-property]
    {:type :object
     :properties {:id id
                  :user-old-id user-old-id
                  :role user-old/role}
     :required [:id
                :user-old-id
                :role]
     :example {:id (:example id)
               :user-old-id (:example user-old-id)
               :role (:example user-old/role)}}))
