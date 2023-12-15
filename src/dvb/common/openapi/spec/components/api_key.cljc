(ns dvb.common.openapi.spec.components.api-key)

;; `APIKey`
(def api-key
  {:type :object
   :properties
   {:id {:type :string
         :format :uuid
         :description "The ID of the API key. This must be sent as the value of the X-APP-ID header in order to authenticate."
         :example "0a8c5614-b544-4484-b06b-e24991135a20"}
    :user-id {:type :string
              :format :uuid
              :description "The ID of the user to which the API key belongs."
              :example "07d3bea2-5935-45e7-b8ed-68c221ed1ffb"}
    :key {:type :string
          :description "The unencrypted key of the API key. This will only be returned once: in the response body of a successful POST /login request."
          :example "ab57ba0a-e7e8-4a2e-9f70-42b094abd90f"}
    :created-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the API key was created."
                 :example "2023-08-20T01:34:11.780Z"}
    :expires-at {:type :string
                 :format :date-time
                 :description "The timestamp of when the API key expires. This is typically two hours after API key creation."
                 :example "2023-08-20T03:34:11.780Z"}}
   :required [:id
              :user-id
              :key
              :created-at
              :expires-at]
   :example {:id "0a8c5614-b544-4484-b06b-e24991135a20"
             :user-id "f13cf3fe-dc85-4d66-a4b6-45aee976927b"
             :key "ab57ba0a-e7e8-4a2e-9f70-42b094abd90f"
             :created-at "2023-08-20T01:34:11.780Z"
             :expires-at "2023-08-20T03:34:11.780Z"}})
