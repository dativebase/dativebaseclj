(ns dvb.server.http.utils.pagination
  (:require [dvb.common.openapi.errors :as errors]))

(defn page-count [item-count items-per-page]
  (max 1 (inc (quot (dec item-count) items-per-page))))

(defn offset
  "Return (maybe offset) for the given pagination inputs. If the inputs are
   inconsistent, return (nothing error)."
  [{:keys [page items-per-page item-count]}]
  (let [page-count (page-count item-count items-per-page)]
    (if (>= page page-count)
      [nil :inputs-invalid]
      [(* items-per-page page) nil])))

(defn offset!
  "Return an integer offset given three integer inputs: page, items-per-page and
   item-count. Throw :inconsistent-pagination-inputs if the inputs are
   inconsistent."
  [page items-per-page item-count]
  (let [[offset error] (offset {:page page
                                :items-per-page items-per-page
                                :item-count item-count})]
    (when error
      (throw (errors/error-code->ex-info :inconsistent-pagination-inputs
                                         {:page page
                                          :items-per-page items-per-page
                                          :item-count item-count})))
    offset))
