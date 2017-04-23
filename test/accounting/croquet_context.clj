(ns accounting.croquet-context
  (:require [accounting.data.meta.croquet :as meta]
            [accounting.match :as m]
            [accounting.data.croquet :as croquet-d]
            [accounting.util :as u]))

(def month->rules
  {:feb croquet-d/feb-rules
   ;:mar croquet-d/mar-rules
   })

(def current-range [{:period/year  2017
                     :period/month :feb}
                    {:period/year  2017
                     :period/month :mar}
                    ])

(def current-rules
  (let [initial-rules (merge-with (comp vec concat) croquet-d/permanent-rules (apply concat (map month->rules [:feb :mar])))]
    (->> initial-rules
         m/canonicalise-rules
         u/probe-off)))
