(ns dvb.common.edges.users
  (:require [clojure.set :as set]
            [dvb.common.edges.common :as common]
            [dvb.common.edges.olds-of-user :as olds-of-user]
            [dvb.common.edges.plans-of-user :as plans-of-user]
            [dvb.common.utils :as u]))

(def clj->api-rename-keys {:is-superuser? :is-superuser})
(def api->clj-rename-keys (set/map-invert clj->api-rename-keys))
(def clj->pg-rename-keys clj->api-rename-keys)
(def pg->clj-rename-keys api->clj-rename-keys)
(def pg->clj-coercions {:registration-status keyword
                        :plans plans-of-user/pgs->cljs
                        :olds olds-of-user/pgs->cljs})
(def clj->pg-coercions {:registration-status name})

(def config
  {:clj->api-rename-keys clj->api-rename-keys
   :api->clj-rename-keys api->clj-rename-keys
   :clj->pg-rename-keys clj->pg-rename-keys
   :pg->clj-rename-keys pg->clj-rename-keys
   :pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge (assoc common/api->clj-coercions
                                     :registration-key u/str->uuid)
                              pg->clj-coercions
                              {:plans plans-of-user/apis->cljs})

   :clj->api-coercions (merge (assoc common/clj->api-coercions
                                     :registration-key u/uuid->str
                                     :plans plans-of-user/cljs->apis)
                              clj->pg-coercions)
   :resource-schema :User
   :resource-write-schema :UserWrite
   :resource-update-schema :UserUpdate})

(def clj->api (partial common/clj->api config))
(def write-clj->api (partial common/write-clj->api config))
(def update-clj->api (partial common/update-clj->api config))
(def api->clj (partial common/api->clj config))
(def pg->clj (partial common/pg->clj config))
(def clj->pg (partial common/clj->pg config))
(def show-api->clj (partial common/show-api->clj api->clj))
(def index-api->clj (partial common/index-api->clj api->clj))

(defn user-password-reset-api->clj [user-password-reset]
  (-> user-password-reset
      (common/perform-coercions {:secret-key u/str->uuid})
      (select-keys (-> common/schemas :UserPasswordReset :properties keys))))

(defn user-password-reset-clj->api [user-password-reset]
  (-> user-password-reset
      (common/perform-coercions {:secret-key u/uuid->str})
      (select-keys (-> common/schemas :UserPasswordReset :properties keys))))
