(ns dvb.common.edges.payments
  (:require [dvb.common.edges.common :as common]))

(def pg->clj-coercions {:roundup-slug keyword
                        :status keyword})

(def clj->pg-coercions {:roundup-slug name
                        :status name})

(def config
  {:pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              clj->pg-coercions)
   :resource-schema :Payment
   :resource-write-schema :PaymentWrite})

(def clj->api (partial common/clj->api config))
(def clj->pg (partial common/clj->pg config))
(def write-clj->api (partial common/write-clj->api config))
(def update-clj->api write-clj->api)
(def api->clj (partial common/api->clj config))
(def pg->clj (partial common/pg->clj config))
(def show-api->clj (partial common/show-api->clj api->clj))
(def index-api->clj (partial common/index-api->clj api->clj))
