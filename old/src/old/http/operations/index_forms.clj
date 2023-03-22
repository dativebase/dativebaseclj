(ns old.http.operations.index-forms)

(defn handle [_application _ctx]
  {:status 200
   :headers {}
   :body {:data [{:id (str (java.util.UUID/randomUUID))
                  :transcription "Где"}]
          :meta {}}})
