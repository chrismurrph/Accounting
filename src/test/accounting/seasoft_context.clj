(ns accounting.seasoft-context
  (:require [accounting.data.meta.seaweed :as meta]
            [accounting.match :as m]
            [accounting.data.seaweed :as seasoft-d]
            [accounting.util :as u]))

(def quarter->rules
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

(def total-range -all-three-quarters)

(def current-rules
  (let [initial-rules (merge-with (comp vec concat) seasoft-d/permanent-rules (apply concat (map quarter->rules [:q1 :q2 :q3])))]
    (->> initial-rules
         m/canonicalise-rules)))

(defn rules-at [year quarter]
  )
