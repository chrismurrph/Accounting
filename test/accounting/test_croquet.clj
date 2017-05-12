(ns accounting.test-croquet
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.croquet :as d]
            [accounting.match :as m]
            [accounting.croquet-context :as con]
            [clojure.test :as test]))

(def current-range con/current-range)
(def current-rules con/current-rules)
(def croquet-bank-accounts (-> meta/human-meta :croquet :bank-accounts))
(def croquet-splits (-> meta/human-meta :croquet :splits))

;; Now s/always be kept in data so that recalc-date can be changed
;;(def ledgers (-> meta/human-meta :croquet :ledgers))

(defn unmatched-croquet-records []
  (let [unmatched-records (c/records-without-single-rule-match :croquet (set croquet-bank-accounts) current-range current-rules)]
    (->> unmatched-records
         (take 10)
         (cons (count unmatched-records)))))

(defn show-unmatched-croquet-records []
  (->> (unmatched-croquet-records)
       reverse
       u/pp))

(test/deftest croquet-unmatches
  (test/is (= (unmatched-croquet-records) '(0))))

(test/deftest croquet-trial-balance
            (let [tb (c/trial-balance :croquet croquet-bank-accounts current-range current-rules croquet-splits d/data)]
              (test/is (= (count tb) 30))
              (test/is (= (:exp/insurance tb) 106.71M))))

(defn trial-balance []
  (u/pp (c/trial-balance :croquet croquet-bank-accounts current-range current-rules croquet-splits d/data)))