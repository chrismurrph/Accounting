(ns accounting.test-seaweed
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.seaweed :as d]
            [accounting.seasoft-context :as con]))

(def current-range con/current-range)
(def current-rules con/current-rules)
(def bank-accounts (-> meta/human-meta :seaweed :bank-accounts))
(def splits (-> meta/human-meta :seaweed :splits))

(defn x-2 []
  (-> (c/records-without-single-rule-match :seaweed (set bank-accounts) current-range current-rules)
      first
      u/pp))

(defn x-3 []
  (->> (c/account-grouped-transactions :seaweed (set bank-accounts) current-range current-rules)
       (take 10)
       u/pp))

(defn x-4 []
  (->> (c/account-grouped-transactions :seaweed (set bank-accounts) current-range current-rules)
       (c/accounts-summary)
       (sort-by (comp - u/abs second))
       u/pp))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            :seaweed
                            (set bank-accounts)
                            current-range
                            current-rules)
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          )]
    (u/pp (reduce (partial gl/apply-trans {:splits splits}) d/general-ledger transactions))))