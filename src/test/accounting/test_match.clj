(ns accounting.test-match
  (:require [accounting.match :as m]
            [accounting.util :as u]
            [accounting.seasoft-context :as seasoft-con]
            [accounting.croquet-context :as croquet-con]
            [accounting.time :as t]
            [accounting.data.meta.croquet :as meta]
            [clojure.test :as test]))

(def example-transaction-liab
  #:out{:date         (t/long-date-str->date "21 Mar 2017"),
        :amount       -400.00M,
        :desc         "ANZ INTERNET BANKING FUNDS TFER TRANSFER 546251 TO      CHRISTOPHER MURP",
        :src-bank     :bank/anz-coy,
        :dest-account :liab/drawings})

(def example-payment
  {:out/desc   "DIRECT CREDIT blah"
   :out/amount 235.00M})

(def wrong-amount-payment
  {:out/desc   "DIRECT CREDIT blah"
   :out/amount 233.00M})

(def wrong-both-payment
  {:out/desc   "blah DIRECT CREDIT blah"
   :out/amount 233.00M})

(def bendigo (first meta/bank-accounts))
(def example-single-rule
  {:logic-operator :single
   :conditions     [[:out/desc :starts-with "DIRECT CREDIT"]]})
(def example-and-rule
  {:logic-operator :and
   :conditions     [[:out/desc :starts-with "DIRECT CREDIT"]
                    [:out/amount :equals 235.00M]]})
(def example-or-rule
  {:logic-operator :or
   :conditions     [[:out/desc :starts-with "DIRECT CREDIT"]
                    [:out/amount :equals 235.00M]]})

(defn view-anz-coy-rules []
  (u/pp (m/bank-rules #{:bank/anz-coy} seasoft-con/current-rules)))

(defn view-first-seaweed-rule []
  (->> seasoft-con/current-rules
       first
       u/pp))

(defn view-first-croquet-rule []
  (->> croquet-con/current-rules
       (take 1)
       u/pp))

;; Expected to not match
(test/deftest bad-match-1
            (test/is (nil? (->> croquet-con/current-rules
                                first
                                (m/match example-transaction-liab)))))

;; Expect to
(test/deftest good-match-1
            (test/is (= (->> example-and-rule
                             (m/match example-payment)) example-and-rule)))

;; Expect not
(test/deftest bad-match-2
            (test/is (nil? (->> example-and-rule
                                (m/match wrong-amount-payment)))))

;; Expect to match
(test/deftest good-match-2
            (test/is (= (->> example-or-rule
                             (m/match wrong-amount-payment)
                             u/probe-on) example-or-rule)))

;; Expect no match
(test/deftest bad-match-3
            (test/is (nil? (->> example-or-rule
                                (m/match wrong-both-payment)))))




