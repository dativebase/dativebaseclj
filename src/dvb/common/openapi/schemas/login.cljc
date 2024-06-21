(ns dvb.common.openapi.schemas.login)

;; `Login`
(def login
  {:type :object
   :properties
   {:email {:type :string
            :description "The email of a registered user."
            :example "handle@emailserver.com"}
    :password {:type :string
               :format :password
               :description "The password of the account with the matching the email."}}
   :required [:email :password]})
