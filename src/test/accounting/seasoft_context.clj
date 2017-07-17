(ns accounting.seasoft-context
  (:require [accounting.data.meta.common :as meta]
            [accounting.match :as m]
            [accounting.data.seaweed :as seasoft-d]
            [accounting.util :as u]
            [accounting.core :as c]
            [accounting.data.meta.periods :as periods]
            [accounting.time :as t]
            [accounting.data.common :as dc]))

(def quarter->rules
  {{:actual-period/tax-year 2017
    :actual-period/quarter  :q1} seasoft-d/q1-2017-rules
   {:actual-period/tax-year 2017
    :actual-period/quarter  :q2} seasoft-d/q2-2017-rules
   {:actual-period/tax-year 2017
    :actual-period/quarter  :q3} seasoft-d/q3-2017-rules})

(def -all-four-quarters [{:actual-period/tax-year 2017
                          :actual-period/quarter  :q1}
                         {:actual-period/tax-year 2017
                          :actual-period/quarter  :q2}
                         {:actual-period/tax-year 2017
                          :actual-period/quarter  :q3}
                         {:actual-period/tax-year 2017
                          :actual-period/quarter  :q4}])

(def total-range (take 4 -all-four-quarters))

(def officeworks-conditions [[:out/desc :starts-with "OFFICEWORKS"] [:out/desc :equals "POST   APPIN LPO          APPIN"]])

(defn officeworks-rule? [m]
  (first (filter (fn [[k v]]
                   (and (= (u/probe-off k "K:") :conditions) (= (u/probe-off v "V:") officeworks-conditions))) m)))

(defn show-officeworks-rules [rules]
  (filter officeworks-rule? rules))

;;
;; If we really need Joda dates we are gonna have to convert on the client. They don't go
;; across the wire.
;;
(defn read-all-edn []
  (let [just-read (mapv t/wildify-java-old (u/read-edn "seaweed.edn"))]
    ;(assert (= seasoft-con/current-rules just-read))
    just-read))

(defn mark-permanents-permanent [rules-m]
  (->> rules-m
       (map (fn [[k v]]
              [k (mapv #(assoc % :rule/permanent? true) v)]))
       (into {})))

(defn get-rules [disk?]
  (if disk?
    (read-all-edn)
    (->> (merge-with (comp vec concat)
                     (mark-permanents-permanent seasoft-d/permanent-rules)
                     (mapcat quarter->rules total-range))
         dc/canonicalise-rules)))

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
  (atom (->> (get-rules false)
             ;; Task is for UI to create this rule again, and conj it onto the atom
             ;; When that's done nothing will come from the server, and Client s/show "All complete" message
             ;; Then replace below rule with others to get the UI correct
             ;; Later read directly from file into atom, and each time after conj write the atom
             (remove officeworks-rule?)
             )))

(defn show-rule-keys []
  (distinct (mapcat keys @current-rules)))

(def seasoft-current-range total-range)
(def seasoft-bank-accounts (-> meta/human-meta :seaweed :bank-accounts))
#_(def seasoft-bank-statements (let [bank-accounts (set seasoft-bank-accounts)
                                     bank-records (c/import-bank-records! :seaweed seasoft-current-range bank-accounts)]
                                 {:bank-records bank-records :bank-accounts bank-accounts}))

(defn write-all-edn []
  (u/write-edn "seaweed.edn" (mapv t/civilize-joda @current-rules)))
