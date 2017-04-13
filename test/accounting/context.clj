(ns accounting.context
  (:require [accounting.meta :as meta]
            [accounting.rules :as r]
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
               :temp-rules (quarter->rules current-quarter)
               })
(def current-periods-range [(:period -current)])

(def current-rules
  (let [initial-rules (r/merge-permanent-with (:temp-rules -current))]
    (->> initial-rules
         r/canonicalise-rules)))
