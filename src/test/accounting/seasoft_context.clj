(ns accounting.seasoft-context
  (:require [accounting.data.meta.common :as meta]
            [accounting.match :as m]
            [accounting.data.seaweed :as seasoft-d]
            [accounting.util :as u]
            [accounting.core :as c]
            [accounting.data.meta.periods :as periods]))

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

(def current-rules
  (let [initial-rules (merge-with (comp vec concat) seasoft-d/permanent-rules (mapcat quarter->rules total-range))]
    (->> initial-rules
         m/canonicalise-rules)))

(def seasoft-current-range total-range)
(def seasoft-bank-accounts (-> meta/human-meta :seaweed :bank-accounts))
(def seasoft-bank-statements (let [bank-accounts (set seasoft-bank-accounts)
                                   bank-records (c/import-bank-records! :seaweed seasoft-current-range bank-accounts)]
                               {:bank-records bank-records :bank-accounts bank-accounts}))

(defn bank-statements-of-period [year quarter]
  (assert (number? year))
  (assert (keyword? quarter))
  (assert (quarter periods/quarters-set) quarter)
  (let [period (periods/make-quarter year quarter)
        bank-accounts (set seasoft-bank-accounts)
        bank-records (c/import-bank-records! :seaweed [period] bank-accounts)]
    {:bank-records bank-records :bank-accounts bank-accounts}))

(defn rules-of-period [year quarter]
  (assert (number? year))
  (assert (keyword? quarter))
  (let [period (periods/make-quarter year quarter)
        initial-rules (merge-with (comp vec concat) seasoft-d/permanent-rules (apply concat (map quarter->rules [period])))]
    (->> initial-rules
         m/canonicalise-rules
         u/probe-off)))

