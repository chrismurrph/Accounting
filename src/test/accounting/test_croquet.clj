(ns accounting.test-croquet
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.data.croquet :as d]
            [accounting.match :as m]
            [accounting.croquet-context :as croquet-con]
            [clojure.test :as test]
            [accounting.api :as api]))

;; Now s/always be kept in data so that recalc-date can be changed
;;(def ledgers (-> meta/human-meta :croquet :ledgers))

(def croquet-current-rules croquet-con/current-rules)

#_(defn unmatched-croquet-records []
  (let [unmatched-records (c/records-without-single-rule-match croquet-con/croquet-bank-statements croquet-current-rules)]
    (->> unmatched-records
         (take 10)
         (cons (count unmatched-records)))))

#_(defn show-unmatched-croquet-records []
  (->> (unmatched-croquet-records)
       reverse
       u/pp))

#_(test/deftest croquet-unmatches
  (test/is (= (unmatched-croquet-records) '(0))))

#_(test/deftest croquet-trial-balance
  (let [{:keys [exp/insurance exp/alcohol] :as tb} (c/trial-balance croquet-con/croquet-bank-statements croquet-current-rules api/croquet-splits d/data)]
    (test/is (= (count tb) 30))
    (test/is (= insurance 106.71M))
    (test/is (= alcohol 355.78M))))

#_(defn show-trial-balance []
  (u/pp (c/trial-balance croquet-con/croquet-bank-statements croquet-current-rules api/croquet-splits d/data)))