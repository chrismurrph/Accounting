(ns accounting.seasoft-context
  (:require [accounting.meta.seaweed :as meta]
            [accounting.match :as m]
            [accounting.seaweed-rules-data :as seasoft-d]
            [accounting.croquet-rules-data :as croquet-d]
            [accounting.util :as u]))

(def seasoft-quarter->rules
  {:q1 seasoft-d/q1-2017-rules
   :q2 seasoft-d/q2-2017-rules
   :q3 seasoft-d/q3-2017-rules})

(def -all-three-quarters [{:period/tax-year 2017
                           :period/quarter  :q1}
                          {:period/tax-year 2017
                           :period/quarter  :q2}
                          {:period/tax-year 2017
                           :period/quarter  :q3}
                          ])

(def current-range -all-three-quarters)

(defn merge-seasoft-permanent-with [quarter-only-rules]
  (merge-with (comp vec concat) seasoft-d/permanent-rules quarter-only-rules))

(def current-seaweed-rules
  (let [initial-rules (merge-seasoft-permanent-with (apply concat (map seasoft-quarter->rules [:q1 :q2 :q3])))]
    (->> initial-rules
         m/canonicalise-rules)))
