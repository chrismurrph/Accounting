(ns accounting.seasoft-context
  (:require [accounting.data.meta.common :as meta]
            [accounting.match :as m]
            [accounting.data.seaweed :as seasoft-d]
            [accounting.util :as u]
            [accounting.core :as c]
            [accounting.data.meta.periods :as periods]
            [accounting.time :as t]))

(def quarter->rules
  {{:period/tax-year 2017
    :period/quarter  :q1} seasoft-d/q1-2017-rules
   {:period/tax-year 2017
    :period/quarter  :q2} seasoft-d/q2-2017-rules
   {:period/tax-year 2017
    :period/quarter  :q3} seasoft-d/q3-2017-rules})

(def -all-three-quarters [{:period/tax-year 2017
                           :period/quarter  :q1}
                          {:period/tax-year 2017
                           :period/quarter  :q2}
                          {:period/tax-year 2017
                           :period/quarter  :q3}])

(def total-range (take 3 -all-three-quarters))

;;
;; As used/read by the program every rule object is a hash-map.
;;
;; Permanent and temporal rules are merged here. This is what we want for matching rules, but is no good
;; for writing them. Permanent rules really don't come with a date!
;; CRAP! Those other data structures were just so it is easy for me to manually enter the data.
;; It look each of these is a map object, some of which have a :period. So it is correct to read and
;; write these, and this format will go in the database.
;;
(def current-rules
  (let [initial-rules (merge-with (comp vec concat) seasoft-d/permanent-rules (mapcat quarter->rules total-range))]
    (->> initial-rules
         m/canonicalise-rules)))

(defn show-rule-keys []
  (distinct (mapcat keys accounting.seasoft-context/current-rules)))

(def officeworks-conditions [[:out/desc :starts-with "OFFICEWORKS"] [:out/desc :equals "POST   APPIN LPO          APPIN"]])

(defn officeworks? [m]
  (first (filter (fn [[k v]]
                   (and (= (u/probe-off k "K:") :conditions) (= (u/probe-off v "V:") officeworks-conditions))) m)))

(defn show-officeworks-rules [rules]
  (filter officeworks? rules))

(def seasoft-current-range total-range)
(def seasoft-bank-accounts (-> meta/human-meta :seaweed :bank-accounts))
(def seasoft-bank-statements (let [bank-accounts (set seasoft-bank-accounts)
                                   bank-records (c/import-bank-records! :seaweed seasoft-current-range bank-accounts)]
                               {:bank-records bank-records :bank-accounts bank-accounts}))

(defn write-all-edn []
  (u/write-edn "seaweed.edn" (mapv t/civilize-joda current-rules)))

(defn read-all-edn []
  (let [just-read (mapv t/wildify-java (u/read-edn "seaweed.edn"))]
    ;(assert (= seasoft-con/current-rules just-read))
    just-read))

#_(defn bank-statements-of-period [year quarter]
  (assert (number? year))
  (assert (keyword? quarter))
  (assert (quarter periods/quarters-set) quarter)
  (let [period (periods/make-quarter year quarter)
        bank-accounts (set seasoft-bank-accounts)
        bank-records (c/import-bank-records! :seaweed [period] bank-accounts)]
    {:bank-records bank-records :bank-accounts bank-accounts}))

#_(defn rules-of-period [year quarter]
  (assert (number? year))
  (assert (keyword? quarter))
  (let [period (periods/make-quarter year quarter)
        initial-rules (merge-with (comp vec concat) seasoft-d/permanent-rules (apply concat (map quarter->rules [period])))]
    (->> initial-rules
         m/canonicalise-rules
         u/probe-off)))

