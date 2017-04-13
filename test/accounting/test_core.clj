(ns accounting.test-core
  (:require [accounting.core :as c]
            [accounting.meta :as meta]
            [accounting.util :as u]
            [accounting.rules :as r]
            [accounting.gl :as gl]
            [accounting.rules-data :as d]
            [accounting.context :as con]))

(defn x-2 []
  (-> (c/first-without-single-rule-match (set meta/bank-accounts) con/current-periods-range con/current-rules)
      u/pp))

(defn x-3 []
  (->> (c/account-grouped-transactions (set meta/bank-accounts) con/current-periods-range con/current-rules)
       (take 10)
       u/pp))

(defn x-4 []
  (->> (c/account-grouped-transactions (set meta/bank-accounts) con/current-periods-range con/current-rules)
       (c/accounts-summary)
       (sort-by (comp - u/abs second))
       u/pp))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            (set meta/bank-accounts)
                            con/current-periods-range
                            con/current-rules)
                          ;(remove #(= :investigate-further (first %)))
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          )]
    (u/pp (reduce gl/apply-trans gl/general-ledger transactions))))