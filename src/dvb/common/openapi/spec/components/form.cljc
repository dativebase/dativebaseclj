(ns dvb.common.openapi.spec.components.form
  "OpenAPI specs for form-related components.

   - `Form`
   - `FormWrite`
   - `PageOfForms`

   A 200 response to `GET /forms` will contain a JSON response body that
   conforms to `PageOfForms`. It is a collection of `Form` objects, with metadata
   to support opaque pagination. For details on the opaque token pagination
   strategy encoded by this response schema, see

   - https://developer.twitter.com/en/docs/twitter-api/pagination"
  (:require [dvb.common.openapi.spec.components.common :as c]))

;; `Form`
(def form
  (let [id c/form-id-property
        transcription {:type :string
                       :description "The orthographic transcription of the form."
                       :example "Les chiens mangaient."}
        created-at (c/created-at-property "form")
        updated-at (c/updated-at-property "form")
        destroyed-at (c/destroyed-at-property "form")
        created-by (c/created-by-property "form")
        updated-by (c/updated-by-property "form")]
    {:type :object
     :properties
     {:id id
      :transcription transcription
      :old-slug c/form-old-slug-property
      :created-at created-at
      :updated-at updated-at
      :destroyed-at destroyed-at
      :created-by created-by
      :updated-by updated-by}
     :required [:id
                :transcription
                :old-slug
                :created-by
                :updated-by
                :created-at
                :updated-at
                :destroyed-at]
     :example {:id (:example id)
               :transcription (:example transcription)
               :old-slug (:example c/form-old-slug-property)
               :created-by (:example created-by)
               :updated-by (:example updated-by)
               :created-at (:example created-at)
               :updated-at (:example updated-at)
               :destroyed-at (:example destroyed-at)}}))

;; `FormWrite`
(def form-write
  {:type :object
   :properties
   {:transcription {:type :string
                    :description "The orthographic transcription of the form."
                    :example "Les chiens mangeaient."}}
   :required [:transcription]
   :example {:transcription "Les chiens mangeaient."}})

;; `PageOfForms`
(def page-of-forms
  {:type :object
   :properties {:data {:type :array
                       :description "The forms returned as response to a GET index type request."
                       :items {:$ref "#/components/schemas/Form"}}
                :meta {:type :object
                       :properties {:count {:type :integer
                                            :description "The count of all forms in this OLD."
                                            :example 1234}
                                    :page {:type :integer
                                           :description "The 0-based index of the page of forms being returned. This value only makes sense given a count of forms in the target OLD and the value of items-per-page."
                                           :default 0
                                           :example 0}
                                    :items-per-page {:type :integer
                                                     :enum [1 5 10 50]
                                                     :description "The maximum number of forms in a page of forms."
                                                     :default 10
                                                     :example 10}}
                       :required [:count :page :items-per-page]
                       :example {:count 1234
                                 :page 0
                                 :items-per-page 10}}}
   :required [:data
              :meta]
   :example {:data [(:example form)]
             :meta {:count 1234
                    :page 0
                    :items-per-page 1}}})

;; `NewFormData`
;; TODO: add more data to this ...
;; - grammaticalities
;; - elicitation_methods
;; - tags
;; - syntactic_categories
;; - speakers
;; - users
;; - sources
(def new-form-data
  {:type :object
   :properties
   {:grammaticalities
    {:type :array
     :description "The available grammaticalities that can be specified for forms in this OLD."
     :items {:type :string
             :description "A grammaticality"
             :example "*"}}}
   :required [:grammaticalities]
   :example {:grammaticalities ["*"]}})

;; `EditFormData`
(def edit-form-data new-form-data)

(def example-forms-search
  {:operator :and
   :complement
   [{:resource :translation
     :attribute :transcription
     :operator :like
     :value "1"}
    {:operator :not
     :complement
     {:resource :form
      :attribute :morpheme-break
      :operator :regex
      :value "[28][5-7]"}}
    {:operator :or
     :complement
     [{:resource :form
       :attribute :datetime-modified
       :operator :less-than
       :value "2012-03-01T00:00:00"}
      {:resource :form
       :attribute :datetime-modified
       :operator :greater-than
       :value "2012-01-01T00:00:00"}]}]})

;; TODO
;; `FormsSearch`
(def forms-search
  {:type :object
   :properties
   {:query
    {:type :object
     :properties
     {:filter {:type :object}
      :order-by {:type :object}}
     :required [:filter]}
    :paginator {:type :object}}
   :required [:query]
   :example {:query {:filter {}}}})
