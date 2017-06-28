(ns accounting.test-seaweed
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.data.meta.seaweed :as seasoft-meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.seaweed :as data]
            [accounting.seasoft-context :as con]
            [accounting.api :as api]
            [accounting.seasoft-context :as seasoft-con]
            [accounting.time :as t]))

(defn show-unmatched-bank-statement-lines []
  (-> (c/records-without-single-rule-match seasoft-con/seasoft-bank-statements (remove seasoft-con/officeworks? seasoft-con/current-rules))
      first
      u/pp))

(defn show-all-transactions-from-ten-accounts []
  (->> (c/account-grouped-transactions seasoft-con/seasoft-bank-statements seasoft-con/current-rules)
       (take 10)
       u/pp))

(defn ordered-by-highest-transaction-volume []
  (->> (c/account-grouped-transactions seasoft-con/seasoft-bank-statements seasoft-con/current-rules)
       c/accounts-summary
       (sort-by (comp - u/abs second))
       u/pp))

(defn show-trial-balance []
  (u/pp (c/trial-balance seasoft-con/seasoft-bank-statements seasoft-con/current-rules api/seasoft-splits data/ye-2016)))

(defn show-bank-balances []
  (-> (c/trial-balance seasoft-con/seasoft-bank-statements seasoft-con/current-rules api/seasoft-splits data/ye-2016)
      (select-keys seasoft-meta/bank-accounts)
      u/pp))