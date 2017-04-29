(ns accounting.test-croquet
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.croquet :as d]
            [accounting.match :as m]
            [accounting.croquet-context :as con]))

(def current-range con/current-range)
(def current-rules con/current-rules)
(def croquet-bank-accounts (-> meta/human-meta :croquet :bank-accounts))
(def croquet-splits (-> meta/human-meta :croquet :splits))

;; Now s/always be kept in data so that recalc-date can be changed
;;(def ledgers (-> meta/human-meta :croquet :ledgers))

(defn x-2 []
  (let [unmatched-records (c/records-without-single-rule-match :croquet (set croquet-bank-accounts) current-range current-rules)]
    (->> unmatched-records
         (take 10)
         (cons (count unmatched-records))
         reverse
         u/pp)))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            :croquet
                            (set croquet-bank-accounts)
                            current-range
                            current-rules)
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          c/compact-transactions
                          )]
    (u/pp (reduce (partial gl/apply-trans {:splits croquet-splits}) d/data transactions))))