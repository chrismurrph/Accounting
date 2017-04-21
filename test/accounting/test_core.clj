(ns accounting.test-core
  (:require [accounting.core :as c]
            [accounting.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.seaweed-rules-data :as d]
            [accounting.context :as con]))

(def seasoft-bank-accounts (let [{:keys [bank-accounts]} (meta/human-meta :seaweed)]
                             bank-accounts))

(defn x-2 []
  (-> (c/first-without-single-rule-match (set seasoft-bank-accounts) con/current-range con/current-rules)
      u/pp))

(defn x-3 []
  (->> (c/account-grouped-transactions (set seasoft-bank-accounts) con/current-range con/current-rules)
       (take 10)
       u/pp))

(defn x-4 []
  (->> (c/account-grouped-transactions (set seasoft-bank-accounts) con/current-range con/current-rules)
       (c/accounts-summary)
       (sort-by (comp - u/abs second))
       u/pp))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            (set seasoft-bank-accounts)
                            con/current-range
                            con/current-rules)
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          )]
    (u/pp (reduce gl/apply-trans gl/general-ledger transactions))))