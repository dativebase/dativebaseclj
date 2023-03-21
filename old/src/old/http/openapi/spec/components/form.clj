(ns old.http.openapi.spec.components.form
  "OpenAPI specs for form-related components.

   - `Form`
   - `FormWrite`
   - `PageOfForms`

   A 200 response to `GET /forms` will contain a JSON response body that
   conforms to `PageOfForms`. It is a collection of `Form` objects, with metadata
   to support opaque pagination. For details on the opaque token pagination
   strategy encoded by this response schema, see

   - https://developer.twitter.com/en/docs/twitter-api/pagination")

;; `Form`
(def form
  {:type :object
   :properties
   {:id {:type :string
         :format :uuid
         :description "The unique identifier of the form. This is a UUID that is generated by the OLD."
         :example "f9a19ceb-fca4-4e96-a5cb-cf1e329763a0"}
    :transcription {:type :string
                    :description "The orthographic transcription of the form."
                    :example "Les chiens mangaient."}}
   :required [:id
              :transcription]
   :example {:id "f9a19ceb-fca4-4e96-a5cb-cf1e329763a0"
             :transcription "Les chiens mangaient."}})

;; `FormWrite`
(def form-write
  {:type :object
   :properties
   {:transcription {:type :string
                    :description "The orthographic transcription of the form."
                    :example "Les chiens mangaient."}}
   :required [:transcription]
   :example {:transcription "Les chiens mangaient."}})

;; `PageOfForms`
(def page-of-forms
  {:type :object
   :properties {:data {:type :array
                       :description "The forms returned as response to a GET index type request."
                       :items {:$ref "#/components/schemas/Form"}}
                :meta {:type :object
                       :properties {:current-token {:type :string
                                                    :description "An opaque token that can be used to re-request the current page of forms. To request the page matching this token, pass the token as the value of pagination_token in the request query parameters, e.g., ?pagination_token=<PAGINATION_TOKEN>."
                                                    :example "6140w"}
                                    :next-token {:type :string
                                                 :description "An opaque token that can be used to request the next page of forms that match the filter parameters of the current request. If there is no next page, then this property will be absent. To request the page matching this token, pass the token as the value of pagination_token in the request query parameters, e.g., ?pagination_token=<PAGINATION_TOKEN>."
                                                 :example "8140w"}
                                    :previous-token {:type :string
                                                     :description "An opaque token that can be used to request the previous page of forms that match the filter parameters of the current request. If there is no previous page, then this property will be absent. To request the page matching this token, pass the token as the value of pagination_token in the request query parameters, e.g., ?pagination_token=<PAGINATION_TOKEN>."
                                                     :example "9140w"}}
                       :example {:current-token "6140w"
                                 :next-token "8140w"}}}
   :required [:data
              :meta]
   :example {:data [{:id "f9a19ceb-fca4-4e96-a5cb-cf1e329763a0"
                     :transcription "Les chiens mangaient."}]
             :meta {:next-token "8140w"}}})
