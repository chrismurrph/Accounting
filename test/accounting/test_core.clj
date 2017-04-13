(ns accounting.test-core
  (:require [accounting.core :as c]
            [accounting.meta :as meta]
            [accounting.util :as u]
            [accounting.rules :as r]
            [accounting.gl :as gl]))

(def amp first)
(def coy second)
(def visa u/third)
(def -current {:bank   (amp meta/bank-accounts)
               :period {:period/year    2017
                        :period/quarter :q3}})
(def periods [(:period -current)])

(defn x-2 []
  (-> (c/first-without-single-rule-match (set meta/bank-accounts) periods)
      u/pp))

(defn x-3 []
  (->> (c/account-grouped-transactions (set meta/bank-accounts) periods)
       (take 10)
       u/pp))

(defn x-4 []
  (->> (c/account-grouped-transactions (set meta/bank-accounts) periods)
       (c/accounts-summary)
       (sort-by (comp - u/abs second))
       u/pp))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            (set meta/bank-accounts)
                            periods
                            r/current-rules)
                          (remove #(= :investigate-further (first %)))
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          )]
    (u/pp (reduce gl/apply-trans gl/general-ledger transactions))))