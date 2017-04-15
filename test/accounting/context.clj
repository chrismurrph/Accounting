(ns accounting.context
  (:require [accounting.meta :as meta]
            [accounting.match :as m]
            [accounting.rules-data :as d]
            [accounting.util :as u]))

(def quarter->rules
  {:q1 d/q1-2017-rules
   :q2 d/q2-2017-rules
   :q3 d/q3-2017-rules})

(def current-quarter :q1)
(def -current {:period     {:period/tax-year 2017
                            :period/quarter  current-quarter}
               :temp-rules (quarter->rules current-quarter)})
(def -current-periods-range [(:period -current)])

(def -all-three-quarters [{:period/tax-year 2017
                           :period/quarter  :q1}
                          {:period/tax-year 2017
                           :period/quarter  :q2}
                          {:period/tax-year 2017
                           :period/quarter  :q3}
                          ])

(def current-range -all-three-quarters)

;; Want rules to just be in the form [{}{}...]
(defn canonicalise-rules [rules-in]
  (mapcat (fn [[[source-bank target-account] v]]
            (map #(assoc % :rule/source-bank source-bank :rule/target-account target-account) v)) rules-in))

(defn merge-permanent-with [quarter-only-rules]
  (merge-with (comp vec concat) d/permanent-rules quarter-only-rules))

(def current-rules
  (let [initial-rules (merge-permanent-with (apply concat (map quarter->rules [:q1 :q2 :q3])))]
    (->> initial-rules
         canonicalise-rules)))
