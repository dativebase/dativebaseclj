(ns dvb.legacy-old-client.print
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]))

;; Vector of 2/3-tuple vectors consisting of:
;; - the keyword attribute of a form,
;; - the keyword shorthand label for that attribute, and
;; - optionally, the function for accessing the attribute;
;; If the optional function is absent, the keyword is the function.
(def pformat-attrs
  [[:id :ID]
   [:narrow_phonetic_transcription :NP]
   [:phonetic_transcription :BP]
   [:transcription :TR (fn [form] (str (:grammaticality form) (:transcription form)))]
   [:morpheme_break :MB]
   [:morpheme_gloss :MG]
   [:translations :TL (fn [form]
                        (str/join
                          ", "
                          (map (fn [tl]
                                 (str (let [gr (:grammaticality tl)]
                                        (if (seq gr) (str gr " ") ""))
                                      "\u2018"
                                      (:transcription tl)
                                      "\u2019"))
                               (:translations form))))]
   [:syntactic_category_string :SCS]
   [:break_gloss_category :BGC]
   [:syntactic_category :SC]])

(defn pformat
  "Return a pretty-formatted string representation of the input form."
  [form]
  (->> (reduce (fn [agg [attr label func]]
                 (let [getter (or func attr)
                       val (getter form)]
                   (if (seq (str/trim (str val)))
                     (conj agg (format "%-5s%s" (str (name label) ":") val))
                     agg)))
               []
               pformat-attrs)
       ((fn [parts] (conj parts "")))
       (str/join "\n")))

(defn print-form
  "Pretty print an OLD form to the terminal."
  [form]
  (println (pformat form)))

(defn print-resource
  [resource rsrc-kw]
  (({:form print-form} rsrc-kw pprint/pprint) resource))

