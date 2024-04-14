(ns dvb.common.edges.forms
  (:require [clojure.set :as set]
            [dvb.common.edges.common :as common]))

(def clj->api-rename-keys {})
(def api->clj-rename-keys (set/map-invert clj->api-rename-keys))
(def clj->pg-rename-keys clj->api-rename-keys)
(def pg->clj-rename-keys api->clj-rename-keys)
(def pg->clj-coercions {:old-slug keyword})
(def clj->pg-coercions {:old-slug name})
(def fe-db->api-coercions {})

(def config
  {:clj->api-rename-keys clj->api-rename-keys
   :api->clj-rename-keys api->clj-rename-keys
   :clj->pg-rename-keys clj->pg-rename-keys
   :pg->clj-rename-keys pg->clj-rename-keys
   :pg->clj-coercions pg->clj-coercions
   :clj->pg-coercions clj->pg-coercions
   :fe-db->api-coercions fe-db->api-coercions
   :api->clj-coercions (merge common/api->clj-coercions
                              pg->clj-coercions)
   :clj->api-coercions (merge common/clj->api-coercions
                              clj->pg-coercions)
   :resource-schema :Form
   :resource-write-schema :FormWrite})

(def clj->pg (partial common/clj->pg config))
(def pg->clj (partial common/pg->clj config))
(def fe-db->api (partial common/fe-db->api config))
(def write-fe-db->api (partial common/write-fe-db->api config))
(def clj->api (partial common/clj->api config))
(def write-clj->api (partial common/write-clj->api config))
(def update-clj->api write-clj->api)
(def api->clj (partial common/api->clj config))
(def show-api->clj (partial common/show-api->clj api->clj))
(def index-api->clj (partial common/index-api->clj api->clj))
