(ns dvb.common.openapi.paths.forms
  (:require [dvb.common.openapi.paths.common :as common]))

(def form-path
  {:get
   {:operation-id :show-form
    :summary "Return the form with the provided ID."
    :description "Return the form with the provided ID."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request for a specific form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :delete
   {:operation-id :delete-form
    :summary "Delete the form with the provided ID."
    :description "Delete the form with the provided ID. This is a soft delete. The form data are not actually removed from the database. However, the system will behave as though the form no longer exists."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The deleted form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to delete the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :put
   {:operation-id :update-form
    :summary "Update the form with the provided ID."
    :description "Update the form with the provided ID using the JSON payload of the request."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :request-body
    {:description "The payload representing the desired new state of the form. This payload must conform to the schema FormUpdate."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormUpdate"}}}}
    :responses
    (assoc common/common-path-responses
           "200" {:description "The updated form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to update the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def forms-path
  {:get
   {:operation-id :index-forms
    :summary "Return all forms matching the supplied query."
    :description "Return all forms matching the supplied query and pagination parameters."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/pageQueryParam"}
                 {:$ref "#/components/parameters/itemsPerPageQueryParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "A page of forms."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
           "400" {:description "The request for forms was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}
   :post
   {:operation-id :create-form
    :summary "Create a new form."
    :description "Create a new form then return the created form."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The payload to create a form. This payload must conform to the schema FormWrite."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormWrite"}}}}
    :responses
    (assoc common/common-path-responses
           "201" {:description "The created form, including server-side-generated values such as the ID."
                  :content {:application-json {:schema {:$ref "#/components/schemas/Form"}}}}
           "400" {:description "The request to create a new form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def new-form-path
  {:get
   {:operation-id :new-form
    :summary "Return the data needed to create a new form."
    :description "Return the data needed to create a new form."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The data needed to create a new form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/NewFormData"}}}})}})

(def search-forms-path
  {:post
   {:operation-id :search-forms
    :summary "Perform a search over the forms in this OLD."
    :description "Perform a search over the forms in this OLD."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}]
    :request-body
    {:description "The query for searching over a set of forms. This payload must conform to the schema FormsSearch."
     :required true
     :content {:application-json {:schema {:$ref "#/components/schemas/FormsSearch"}}}}
    :responses
    (assoc common/common-path-responses
           "200" {:description "The set of forms matching the search query in the request."
                  :content {:application-json {:schema {:$ref "#/components/schemas/PageOfForms"}}}}
           "400" {:description "The search request was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})

(def edit-form-path
  {:get
   {:operation-id :edit-form
    :summary "Return the data needed to update an existing form."
    :description "Return the data needed to update an existing form."
    :tags [:Forms]
    :parameters [{:$ref "#/components/parameters/acceptJSONHeaderParam"}
                 {:$ref "#/components/parameters/oldSlugPathParam"}
                 {:$ref "#/components/parameters/formIDParam"}]
    :responses
    (assoc common/common-path-responses
           "200" {:description "The data needed to update the specified form."
                  :content {:application-json {:schema {:$ref "#/components/schemas/EditFormData"}}}}
           "400" {:description "The request for the data needed to update the specified form was invalid."
                  :content {:application-json {:schema {:$ref "#/components/schemas/ErrorBadRequest400"}}}})}})
