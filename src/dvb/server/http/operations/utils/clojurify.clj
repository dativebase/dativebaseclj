(ns dvb.server.http.operations.utils.clojurify
  (:require [clojure.set :as set]))

(defn user [user*]
  (-> user*
      (set/rename-keys {:is-superuser :is-superuser?})))
