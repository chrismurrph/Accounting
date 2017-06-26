(ns accounting.api
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.seasoft-context :as con]
            [accounting.data.seaweed :as data]))

(def example
  {:exp/motor-vehicle 602.63M,
   :exp/bank-fee 82.90M,
   :exp/advertising 20.00M,
   :exp/petrol 287.48M,
   :personal/anz-visa -3456.61M,
   :bank/amp 51.60M})

(defn make-ledger-item [idx [kw amount]]
  (assert (keyword? kw) (str "Expect a keyword but got: " kw ", has type: " (type kw)))
  {:db/id idx :ledger-item/name (name kw) :ledger-item/type ((comp keyword namespace) kw) :ledger-item/amount amount})

(defn ->ledger-items [m]
  (->> m
       (partition 2)
       (mapcat identity)
       (map-indexed make-ledger-item)
       vec))

(def current-range (-> con/total-range first vector))
(def bank-accounts (-> meta/human-meta :seaweed :bank-accounts))
(def bank-statements (let [bank-accounts (set bank-accounts)
                           bank-records (c/import-bank-records! :seaweed current-range bank-accounts)]
                       {:bank-records bank-records :bank-accounts bank-accounts}))
(def current-rules con/current-rules)
(def splits (-> meta/human-meta :seaweed :splits))

(defn trial-balance-report [year period]
  (-> (c/trial-balance bank-statements current-rules splits data/ye-2016)
      ->ledger-items))

(def rep->fn
  {:report/profit-and-loss (fn [_ _] [(make-ledger-item 0 [:dummy-entry 1000])])
   :report/balance-sheet (fn [_ _] [(make-ledger-item 1 [:dummy-entry 1001])])
   :report/big-items-first (fn [_ _] [(make-ledger-item 2 [:dummy-entry 1002])])
   :report/trial-balance   trial-balance-report})

(defn fetch-report [query year period report]
  (assert (= 3 (count query)))
  ((report rep->fn) year period))
