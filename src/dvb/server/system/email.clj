(ns dvb.server.system.email
  "For sending emails."
  (:refer-clojure :exclude [send])
  (:require [com.stuartsierra.component :as component]
            [dvb.server.log :as log]))

(defprotocol IEmail
  "For, you know, sending email"
  (send [this to-address email-body] "Get now: the current timestamp"))

(defrecord Email []
  IEmail
  (send [_this to-address email-body]
    (println "Sending an email.")
    (println "To:" to-address)
    (printf "Body:\n%s\n" email-body)
    (log/info "Sending an email"
              {:to-address to-address
               :email-body email-body}))
  component/Lifecycle
  (start [c] c)
  (stop [c] c))

(defn make-email []
  (map->Email {}))

(comment

  (def e (make-email))

  (send e
        "abc@gmail.com"
        "Hello, Abc,\n\nHow are you?\n\n- Sender")

)
