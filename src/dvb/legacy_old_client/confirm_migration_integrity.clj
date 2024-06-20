(ns dvb.legacy-old-client.confirm-migration-integrity
  "To confirm the integrity of the migration from DO to INT."
  (:require [dvb.legacy-old-client.core :as oc]))

(def do-old-url "https://app.onlinelinguisticdatabase.org")

(def int-old-url "https://dative.test.ivdnt.org/olds")

(defn old-url [server old-name]
  (str (if (= :do server) do-old-url int-old-url) "/" old-name "/"))

(def olds
  "This is from `SHOW DATABASES` run on the DO server on June 12, 2024.
  These dbs were removed from the list: mysql, information_schema, and
  performance_schema."
  ["aceold"
   "analyse_avec_informateur_2018old"
   "analyse_avec_informateur_2024"
   "animacyold"
   "anwold"
   "basistestold"
   "batumi_kartuliold"
   "benold"
   "bhmold"
   "blaold"
   "bniold"
   "boaold"
   "byvold"
   "cacold"
   "cakold"
   "cceold"
   "cggold"
   "cherokeetestold"
   "cldold"
   "cleftsold"
   "comold"
   "cooold"
   "crdold"
   "crkold"
   "ctuold"
   "demo"
   "demo2"
   "demoold"
   "devold"
   "dgaold"
   "dpold"
   "ebuold"
   "field_methods_uiuc_sp2020old"
   "gitold"
   "gitoldtmp"
   "glaold"
   "glk101old"
   "glk201old"
   "guzold"
   "hatold"
   "hmnold"
   "ibo_fall_2022"
   "igala2018old"
   "igala2021old"
   "ike_field_methodsold"
   "ike_genesisold"
   "ike_hiver_2015old"
   "ikeold"
   "ikeold2020old"
   "ikuold"
   "innuinaqtunold"
   "irkold"
   "jal401old"
   "jenneke_testold"
   "kab2019old"
   "kabold"
   "kamold"
   "katold"
   "kavonold"
   "khmold"
   "kichukaold"
   "kikold"
   "kiow1265old"
   "kty_old"
   "kutold"
   "kwk_uvic_2017old"
   "kwkold"
   "laiold"
   "lingala2021old"
   "linold"
   "luaold"
   "lugold"
   "maku1279old"
   "malagasy_mcgill_2020old"
   "malagasy_uoft_lec_5101old"
   "merold"
   "mgzold"
   "micold"
   "mlgold"
   "moh_kor"
   "moh_mcgill"
   "mohold"
   "morold"
   "mypold"
   "nasold"
   "ndhold"
   "nepali_umn_2019old"
   "nepold"
   "newariold"
   "nglold"
   "ntkold"
   "nusold"
   "nvoold"
   "nya2old"
   "nyaold"
   "nyyold"
   "ojiold"
   "okaold"
   "quechuaold"
   "rkmold"
   "run_mcgill_2022old"
   "runold"
   "saudi_arabic"
   "sgcold"
   "sghold"
   "snaold"
   "somold"
   "swahili_variation"
   "swaold"
   "tekeold"
   "tglold"
   "thfold"
   "thkold"
   "tliold"
   "togold"
   "transylvanian_saxonold"
   "trcold"
   "tsoold"
   "tumold"
   "tvuold"
   "wolof_martinaold"
   "yale_fall_2016_field_methodsold"
   "yaoold"
   "zgaold"])

;; Test suite. The following should all be identical between two OLDs:
;; - DONE. form counts
;; - DONE. first page of forms
;; - DONE. last page of forms
;; - DONE. file counts
;; - DONE. first page of files
;; - DONE. last page of files
;; - DONE. user counts
;; - DONE. set of all users

(defn olds-identical? [{:keys [old-name username password]}]
  (let [do-url (old-url :do old-name)
        do-c (oc/make-old-client {:url do-url :username username :password password})
        int-url (old-url :int old-name)
        int-c (oc/make-old-client {:url int-url :username username :password password})

        ;; First Page of forms equal?
        items-per-page 10
        do-page-1-of-forms-response
        (oc/oc-get do-c "forms" {:page 1 :items_per_page items-per-page})
        int-page-1-of-forms-response
        (oc/oc-get int-c "forms" {:page 1 :items_per_page items-per-page})
        first-page-forms-equal? (= do-page-1-of-forms-response
                                   int-page-1-of-forms-response)

        ;; Last Page of forms equal?
        do-paginator (:paginator do-page-1-of-forms-response)
        do-count (:count do-paginator)
        int-paginator (:paginator int-page-1-of-forms-response)
        int-count (:count int-paginator)
        do-last-page (int (Math/floor (/ do-count (float items-per-page))))
        int-last-page (int (Math/floor (/ int-count (float items-per-page))))
        do-last-page-of-forms-response
        (oc/oc-get do-c "forms" {:page do-last-page :items_per_page items-per-page})
        int-last-page-of-forms-response
        (oc/oc-get int-c "forms" {:page int-last-page :items_per_page items-per-page})
        last-page-forms-equal? (= do-last-page-of-forms-response
                                  int-last-page-of-forms-response)

        ;; First Page of files equal?
        do-page-1-of-files-response
        (oc/oc-get do-c "files" {:page 1 :items_per_page items-per-page})
        int-page-1-of-files-response
        (oc/oc-get int-c "files" {:page 1 :items_per_page items-per-page})
        first-page-files-equal? (= do-page-1-of-files-response
                                   int-page-1-of-files-response)

        ;; Last Page of files equal?
        do-file-paginator (:paginator do-page-1-of-files-response)
        do-file-count (:count do-file-paginator)
        int-file-paginator (:paginator int-page-1-of-files-response)
        int-file-count (:count int-file-paginator)
        do-last-file-page (int (Math/floor (/ do-file-count (float items-per-page))))
        int-last-file-page (int (Math/floor (/ int-file-count (float items-per-page))))
        do-last-page-of-files-response
        (oc/oc-get do-c "files" {:page do-last-file-page :items_per_page items-per-page})
        int-last-page-of-files-response
        (oc/oc-get int-c "files" {:page int-last-file-page :items_per_page items-per-page})
        last-page-files-equal? (= do-last-page-of-files-response
                                  int-last-page-of-files-response)

        ;; First page of 100 users equal?
        do-first-page-users-response
        (oc/oc-get do-c "users" {:page 1 :items_per_page 100})
        int-first-page-users-response
        (oc/oc-get int-c "users" {:page 1 :items_per_page 100})
        first-page-users-equal? (= do-first-page-users-response
                                   int-first-page-users-response)
        have-all-users? (< (count (:items do-first-page-users-response)) 100)]
    {:identical? (every? true? [first-page-forms-equal?
                                last-page-forms-equal?
                                first-page-files-equal?
                                last-page-files-equal?
                                first-page-users-equal?
                                have-all-users?])
     :old-name old-name
     :evaluations
     {:first-page-forms-equal? first-page-forms-equal?
      :last-page-forms-equal? last-page-forms-equal?
      :first-page-files-equal? first-page-files-equal?
      :last-page-files-equal? last-page-files-equal?
      :first-page-users-equal? first-page-users-equal?
      :have-all-users? have-all-users?}
     :data
     {:do-page-1-of-forms-response do-page-1-of-forms-response
      :do-last-page-of-forms-response do-last-page-of-forms-response
      :do-page-1-of-files-response do-page-1-of-files-response
      :do-last-page-of-files-response do-last-page-of-files-response
      :do-first-page-users-response do-first-page-users-response
      :int-page-1-of-forms-response int-page-1-of-forms-response
      :int-last-page-of-forms-response int-last-page-of-forms-response
      :int-page-1-of-files-response int-page-1-of-files-response
      :int-last-page-of-files-response int-last-page-of-files-response
      :int-first-page-users-response int-first-page-users-response}}))

(comment

  (def username "jdunham")

  password
  ;; (def password "REDACTED")

  (def old-name "blaold")

  (def result (olds-identical? {:old-name old-name
                                :username username
                                :password password}))

  (dissoc result :data)
  {:identical? true
   :old-name "blaold"
   :evaluations {:first-page-forms-equal? true
                 :last-page-forms-equal? true}}

  ;; Here I manually changed data in the INT blaold. Now I expect the comparison
  ;; to fail:

  (def result-after (olds-identical? {:old-name old-name
                                      :username username
                                      :password password}))
  (dissoc result-after :data)
  {:identical? false,
   :old-name "blaold",
   :evaluations {:first-page-forms-equal? false, :last-page-forms-equal? true}}

  ;; Run the test on okaold

  (def result-oka (olds-identical? {:old-name "okaold"
                                    :username username
                                    :password password}))

  (dissoc result-oka :data)

  (->> olds
       (filter (partial not= "blaold"))
       (drop-while (fn [x] (not= "demo" x)))

       )

  (def analysis
    (->> olds
         (filter (fn [o] (not (some #{o} ["blaold" "demo"]))))
         (take 200)
         (map (fn [old-name]
                [old-name
                 (try
                   (olds-identical? {:old-name old-name
                                     :username username
                                     :password password})
                   (catch Exception e
                     {:old-name old-name
                      :exception true}))]))
         (into {})))

  (->> analysis
       (map (juxt key (comp :identical? val)))
       (into {}))

  (->> analysis keys)

  (->> analysis
       (map (comp :identical? val))
       frequencies)
  {nil 97, true 25}

  (def analysis-2
    (->> olds
         (filter (fn [o] (not (some #{o} ["blaold" "demo"]))))
         (take 200)
         (map (fn [old-name]
                [old-name
                 (let [old-analysis (get analysis old-name)
                       run? (not (:exception old-analysis))]
                   (if run?
                     old-analysis
                     (try
                       (olds-identical? {:old-name old-name
                                         :username username
                                         :password password})
                       (catch Exception e
                         {:old-name old-name
                          :exception true}))))]))
         (into {})))

  (->> analysis-2
       (map (comp :identical? val))
       frequencies)

  {nil 97, true 25}


)
