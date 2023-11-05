(ns old.http.utils
  (:require [clojure.string :as str]))

(defn deep-merge
  "See https://clojuredocs.org/clojure.core/merge"
  [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)
              v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      (last vs))))

(defn- fix-keys [fixer fixable? x]
  (cond
    (map? x) (->> x
                  (map (fn [[k v]] [(if (fixable? k) (fixer k) k)
                                    (fix-keys fixer fixable? v)]))
                  (into {}))
    (coll? x) (->> x
                   (map #(fix-keys fixer fixable? %))
                   (into (empty x)))
    :else x))

(defn- keywordizable? [x]
  (and (not (re-find #"^[0-9]" x))
       (not (str/includes? x "."))))

(defn snake-str->kebab-kw [k] (keyword (str/replace k #"_" "-")))

(defn kebab-kw->snake-str [k] (str/replace (name k) #"-" "_"))

(def keywordize-keys (partial fix-keys keyword keywordizable?))

(def kebab-keywordize-keys (partial fix-keys snake-str->kebab-kw keywordizable?))

(def snake-stringify-keys (partial fix-keys kebab-kw->snake-str keyword?))

(defn keyword-lower-case [k] (-> k name str/lower-case keyword))

(defn parse-query-string
  "Parse an HTTP query string like `limit=25&offset=75` to a map from keywords
   to strings, e.g., `{:limit 25 :offset 75}`."
  [query-string]
  (when query-string
    (->> (str/split query-string #"&")
         (map (fn [kv] (let [[k v] (str/split kv #"=" 2)]
                         [(snake-str->kebab-kw k) v])))
         (into {}))))
