(ns accounting.data.test-seaweed
  (:require [accounting.data.seaweed :as seaweed]
            [clojure.test :as test]))

(test/deftest tb-zero-from-xero
  (let [expected 0M
        values (remove #(= 0M %) (vals seaweed/xero-tb-ye-2016))
        should-be-zero (reduce + values)
        num-values (count values)]
    (test/is (= 37 (count values)) (str "Expected 37 entries but have: " num-values))
    (test/is (bigdec? should-be-zero))
    (test/is (= expected should-be-zero) (str "Either bad types or not zero: " (type expected) " " (type should-be-zero) " " should-be-zero))))

(test/deftest irrelevant-excluded
  (let [expected 0M
        irrelevant-excluded (dissoc seaweed/xero-tb-ye-2016 [:asset/banking-correction :bank/amp :bank/anz-visa :liab/trash])
        should-be-zero (reduce + (vals irrelevant-excluded))]
    (test/is (= expected should-be-zero) (str "irrelevant entries: " irrelevant-excluded " not zero: " should-be-zero))))
