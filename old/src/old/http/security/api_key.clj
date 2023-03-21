(ns old.http.security.api-key
  (:require [old.http.openapi.errors :as errors]))

(def test-secret-key "olddative")

;; TODO: If we are responsible for security checks at the application level,
;; then create a real implementation for this `handle` fn!
(defn handle
  [_application api-key]
  (if (= test-secret-key api-key)
    {:api-key api-key
     :type :machine-user
     :roles [:*]}
    (throw (errors/error-code->ex-info :unauthenticated))))
