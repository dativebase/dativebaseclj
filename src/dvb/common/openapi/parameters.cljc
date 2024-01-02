(ns dvb.common.openapi.parameters
  (:require [dvb.common.utils :as u]))

(def uuid-string-regex (str "^"
                            "[a-f0-9]{8}-"
                            "[a-f0-9]{4}-"
                            "[a-f0-9]{4}-"
                            "[a-f0-9]{4}-"
                            "[a-f0-9]{12}$"))

(defn id-param [resource]
  {:name (keyword (str resource "_id"))
   :in :path
   :description (u/format "The ID of the referenced %s." resource)
   :required true
   :schema {:type :string
            :pattern uuid-string-regex}})

(def parameters
  {:acceptJSONHeaderParam {:name :accept
                           :in :header
                           :description "The content type of the response body that the client will accept. This API requires that `application/json` be supplied here. Otherwise, a 406 response status will be returned."
                           :required true
                           :schema {:type :string
                                    :enum ["application/json"]}}
   :oldSlugPathParam {:name :old_slug
                      :in :path
                      :description "The slug of the target OLD."
                      :required true
                      :schema {:type :string
                               :pattern "^[a-zA-Z0-9_-]+$"}}
   :planIDParam (id-param "plan")
   :userIDParam (id-param "user")
   :formIDParam (id-param "form")
   :userRegistrationKeyParam {:name :user_registration_key
                              :in :path
                              :description "The registration key for the user that is being activated."
                              :required true
                              :schema {:type :string
                                       :pattern uuid-string-regex}}
   :pageQueryParam {:name :page
                    :in :query
                    :description "The 0-based index of the page of forms requested. The first page is 0."
                    :required false
                    :schema {:type :integer
                             :default 0}}
   :itemsPerPageQueryParam {:name :items-per-page
                            :in :query
                            :description "The maximum number of items to return in each page of forms."
                            :required false
                            :schema {:type :integer
                                     :enum [1 5 10 50]
                                     :default 10}}
   :includePlansBooleanQueryParam {:name :include-plans
                                   :in :query
                                   :description "Whether or not to include the plans of the returned user(s)."
                                   :required false
                                   :schema {:type :boolean
                                            :default false}}})
