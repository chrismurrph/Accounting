(ns accounting.test-seaweed
  (:require [accounting.core :as c]
            [accounting.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.seaweed-rules-data :as d]
            [accounting.seasoft-context :as con]))

(def current-range con/current-range)
(def current-rules con/current-seaweed-rules)
(def seasoft-bank-accounts (let [{:keys [bank-accounts]} (meta/human-meta :seaweed)]
                             bank-accounts))

(defn x-2 []
  (-> (c/first-without-single-rule-match :seaweed (set seasoft-bank-accounts) current-range current-rules)
      u/pp))

(defn x-3 []
  (->> (c/account-grouped-transactions :seaweed (set seasoft-bank-accounts) current-range current-rules)
       (take 10)
       u/pp))

(defn x-4 []
  (->> (c/account-grouped-transactions :seaweed (set seasoft-bank-accounts) current-range current-rules)
       (c/accounts-summary)
       (sort-by (comp - u/abs second))
       u/pp))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            :seaweed
                            (set seasoft-bank-accounts)
                            current-range
                            current-rules)
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          )]
    (u/pp (reduce gl/apply-trans gl/general-ledger transactions))))