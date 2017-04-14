(ns accounting.context
  (:require [accounting.meta :as meta]
            [accounting.match :as m]
            [accounting.rules-data :as d]
            [accounting.util :as u]))

(def quarter->rules
  {:q2 d/q2-2017-rules
   :q3 d/q3-2017-rules})

(def amp first)
(def coy second)
(def visa u/third)

(def current-quarter :q2)
(def -current {:bank       (amp meta/bank-accounts)
               :period     {:period/tax-year    2017
                            :period/quarter current-quarter}
               :temp-rules (quarter->rules current-quarter)})
(def current-periods-range [(:period -current)])

(defn canonicalise-rules [rules-in]
  (mapcat (fn [[[source-bank target-account] v]]
            (map #(assoc % :rule/source-bank source-bank :rule/target-account target-account) v)) rules-in))

(defn merge-permanent-with [quarter-only-rules]
  (merge-with (comp vec concat) d/permanent-rules quarter-only-rules))

(def current-rules
  (let [initial-rules (merge-permanent-with (:temp-rules -current))]
    (->> initial-rules
         canonicalise-rules)))
