(ns accounting.test-seaweed
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.seaweed :as data]
            [accounting.seasoft-context :as con]
            [accounting.api :as api]))

(defn show-unmatached-rules []
  (-> (c/records-without-single-rule-match api/bank-statements api/current-rules)
      first
      u/pp))

(defn show-all-transactions-from-ten-accounts []
  (->> (c/account-grouped-transactions api/bank-statements api/current-rules)
       (take 10)
       u/pp))

(defn ordered-by-highest-transaction-volume []
  (->> (c/account-grouped-transactions api/bank-statements api/current-rules)
       c/accounts-summary
       (sort-by (comp - u/abs second))
       u/pp))

(defn show-trial-balance []
  (u/pp (c/trial-balance api/bank-statements api/current-rules api/splits data/ye-2016)))