(ns accounting.data.common
  (:require [clojure.string :as s]
            [accounting.time :as t]))

(defn -starts-with? [starts-with]
  (fn [field-value]
    (s/starts-with? field-value starts-with)))

(defn -ends-with? [ends-with]
  (fn [field-value]
    (s/ends-with? field-value ends-with)))

(defn -equals? [equals]
  (fn [field-value]
    (= field-value equals)))

(defn -day-of-month? [day-of-month]
  (fn [field-value]
    (= (t/day-of-month field-value) day-of-month)))

(defn -contains? [includes]
  (fn [field-value]
    (s/includes? field-value includes)))

(defn -not-starts-with? [starts-with]
  (fn [field-value]
    ((complement s/starts-with?) field-value starts-with)))

(defn -not-ends-with? [ends-with]
  (fn [field-value]
    ((complement s/ends-with?) field-value ends-with)))

(defn -not-equals? [equals]
  (fn [field-value]
    ((complement =) field-value equals)))

(defn -not-contains? [includes]
  (fn [field-value]
    ((complement s/includes?) field-value includes)))

(defn -less-than? [less-than]
  (fn [field-value]
    (< field-value less-than)))

(defn -greater-than? [greater-than]
  (fn [field-value]
    (> field-value greater-than)))

;;
;; Nots are gonna help if have many rules going to one (so they are OR-ed) bank / account combo, and
;; we don't want two rules to match, which comes out as an error.
;; Whether the user s/be allowed proper logic with brackets is an open question. You probably can do
;; a UI for that.
;; Hmm - automatically excluding a whole AND of conditions, for possibly many others, sounds tricky.
;; Take comfort in the fact that Google, Xero and others have never done this well.
;; Hmm -many going to same place (bank / account combo) is not a problem and s/not be flagged as such
;;
(def condition-functions
  {
   :starts-with     -starts-with?
   :ends-with       -ends-with?
   :equals          -equals?
   :day-of-month    -day-of-month?
   :contains        -contains?
   :not-starts-with -not-starts-with?
   :not-ends-with   -not-ends-with?
   :not-equals      -not-equals?
   :not-contains    -not-contains?
   :less-than       -less-than?
   :greater-than    -greater-than?})

(def condition-types
  (into #{} (keys condition-functions)))

(defn put-field-first-condition [rule condition]
  (assert (condition-types (first condition)))
  (into [(:field rule)] condition))

(defn field-into-conditions [rule]
  (assert (map? rule))
  (if (:field rule)
    (-> rule
        (update :conditions (fn [conds]
                              (mapv #(put-field-first-condition rule %1) conds)))
        (dissoc :field))
    rule))

;; Want rules to just be in the form [{}{}...]
(defn canonicalise-rules [rules-in]
  (->> rules-in
       (mapcat (fn [[[source-bank target-account] v]]
                 (map #(assoc % :rule/source-bank source-bank :rule/target-account target-account) v)))
       (mapv field-into-conditions)))

