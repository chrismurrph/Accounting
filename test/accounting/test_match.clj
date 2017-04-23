(ns accounting.test-match
  (:require [accounting.match :as m]
            [accounting.util :as u]
            [accounting.seasoft-context :as seasoft-con]
            [accounting.croquet-context :as croquet-con]
            [accounting.time :as t]
            [accounting.data.meta.croquet :as meta]))

(def example-transaction-capital
  #:out{:date         (t/long-date-str->date "21 Mar 2017"),
        :amount       -400.00M,
        :desc         "ANZ INTERNET BANKING FUNDS TFER TRANSFER 546251 TO      CHRISTOPHER MURP",
        :src-bank     :bank/anz-coy,
        :dest-account :capital/drawings})

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

(defn x-1 []
  (u/pp (m/bank-rules #{:bank/anz-coy} seasoft-con/current-rules)))

(defn x-2 []
  (->> seasoft-con/current-rules
       first
       u/pp))

(defn x-3 []
  (->> croquet-con/current-rules
       (take 1)
       u/pp))

;; Expected to not match
(defn x-4 []
  (->> croquet-con/current-rules
       first
       (m/match example-transaction-capital)
       u/pp))

;; Expect to
(defn x-5 []
  (->> example-and-rule
       (m/match example-payment)
       u/pp))

;; Expect not
(defn x-6 []
  (assert (->> example-and-rule
               (m/match wrong-amount-payment)
               nil?)))

;; Expect to match
(defn x-7 []
  (assert (->> example-or-rule
               (m/match wrong-amount-payment)
               u/probe-on)))

;; Expect no match
(defn x-8 []
  (assert (->> example-or-rule
               (m/match wrong-both-payment)
               nil?)))




