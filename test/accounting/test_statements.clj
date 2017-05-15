(ns accounting.test-statements
  (:require [accounting.statements :as s]
            [accounting.data.seaweed :as d]
            [accounting.util :as u]))

(def ex-1 {"Accountancy Fees" :exp/accounting-expense})
(def ex-2 {"Bookkeeping Fees" nil})
(def ex-3 {"Depreciation" (s/add [:exp/plant-equip-deprec
                                  :exp/low-value-pool-deprec])})
(def ex-report [ex-1 ex-2 ex-3])

(defn x-1 []
  (apply (s/->line d/xero-tb-ye-2016) ex-1))

(defn x-2 []
  (apply (s/->line d/xero-tb-ye-2016) ex-2))

(defn x-3 []
  (apply (s/->line d/xero-tb-ye-2016) ex-3))

(defn x-4 []
  (let [pos-report-fn (s/->report-section d/xero-tb-ye-2016 +)
        neg-report-fn (s/->report-section d/xero-tb-ye-2016 -)]
    (u/pp (mapcat (fn [[headings sign]]
                    (let [f (condp = sign
                              + pos-report-fn
                              - neg-report-fn)]
                      (f headings)))
                  [[s/income-headings -] [s/expenditure-headings +]]))))
