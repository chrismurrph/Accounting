(ns accounting.test-seaweed
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.seaweed :as data]
            [accounting.seasoft-context :as con]))

(def current-range (-> con/total-range first vector))
(def current-rules con/current-rules)
(def bank-accounts (-> meta/human-meta :seaweed :bank-accounts))
(def splits (-> meta/human-meta :seaweed :splits))

(def bank-statements (let [bank-accounts (set bank-accounts)
                           bank-records (c/import-bank-records! :seaweed current-range bank-accounts)]
                       {:bank-records bank-records :bank-accounts bank-accounts}))

(defn show-unmatached-rules []
  (-> (c/records-without-single-rule-match bank-statements current-rules)
      first
      u/pp))

(defn show-all-transactions-from-ten-accounts []
  (->> (c/account-grouped-transactions bank-statements current-rules)
       (take 10)
       u/pp))

(defn ordered-by-highest-transaction-volume []
  (->> (c/account-grouped-transactions bank-statements current-rules)
       c/accounts-summary
       (sort-by (comp - u/abs second))
       u/pp))

(defn show-trial-balance []
  (u/pp (c/trial-balance bank-statements current-rules splits data/ye-2016)))