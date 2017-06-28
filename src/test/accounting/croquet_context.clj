(ns accounting.croquet-context
  (:require [accounting.data.meta.common :as meta]
            [accounting.match :as m]
            [accounting.data.croquet :as croquet-d]
            [accounting.util :as u]
            [accounting.core :as c]
            [accounting.data.meta.periods :as periods]))

(def month->rules
  {{:period/year  2017
    :period/month :feb} croquet-d/feb-rules
   {:period/year  2017
    :period/month :mar} croquet-d/mar-rules
   })

(def total-range [{:period/year  2017
                   :period/month :feb}
                  {:period/year  2017
                   :period/month :mar}
                  ])

(def current-rules
  (let [initial-rules (merge-with (comp vec concat) croquet-d/permanent-rules (apply concat (map month->rules total-range)))]
    (->> initial-rules
         m/canonicalise-rules
         u/probe-off)))

(def croquet-current-range total-range)
(def croquet-bank-accounts (-> meta/human-meta :croquet :bank-accounts))
(def croquet-bank-statements (let [bank-accounts (set croquet-bank-accounts)
                                   bank-records (c/import-bank-records! :croquet croquet-current-range bank-accounts)]
                               {:bank-records bank-records :bank-accounts bank-accounts}))

#_(defn bank-statements-of-period [year month]
  (assert (number? year))
  (assert (keyword? month))
  (assert (month periods/months-set) month)
  (let [period (periods/make-month year month)
        bank-accounts (set croquet-bank-accounts)
        bank-records (c/import-bank-records! :croquet [period] bank-accounts)]
    {:bank-records bank-records :bank-accounts bank-accounts}))

#_(defn rules-of-period [year month]
  (assert (number? year))
  (assert (keyword? month))
  (let [period (periods/make-month year month)
        initial-rules (merge-with (comp vec concat) croquet-d/permanent-rules (apply concat (map month->rules [period])))]
    (->> initial-rules
         m/canonicalise-rules
         u/probe-off)))
