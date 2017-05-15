(ns accounting.test-statements
  (:require [accounting.statements :as s]
            [accounting.data.seaweed :as d]
            [accounting.util :as u]))

(def example-1 {"Accountancy Fees" :exp/accounting-expense})
(def example-2 {"Bookkeeping Fees" nil})
(def example-3 {"Depreciation" (s/add [:exp/plant-equip-deprec
                                       :exp/low-value-pool-deprec])})
(def example-report [example-1 example-2 example-3])

(defn x-1 []
  (apply (s/->line d/xero-tb-ye-2016) example-1))

(defn x-2 []
  (apply (s/->line d/xero-tb-ye-2016) example-2))

(defn x-3 []
  (apply (s/->line d/xero-tb-ye-2016) example-3))

(defn x-4 []
  (let [sum-fn (s/sum-section d/xero-tb-ye-2016)]
    (sum-fn example-report)))

#_(defn report-body-1 []
  (let [pos-report-fn (s/->report-section d/xero-tb-ye-2016 +)
        neg-report-fn (s/->report-section d/xero-tb-ye-2016 -)]
    (u/pp (mapcat (fn [[headings sign]]
                    (let [f (condp = sign
                              + pos-report-fn
                              - neg-report-fn)]
                      (f headings)))
                  [[s/income-headings -] [s/expenditure-headings +]]))))

(defn report-body-2 []
  (let [tb d/xero-tb-ye-2016
        report-tb-section (s/->report-section tb)
        pos-report-fn (report-tb-section +)
        neg-report-fn (report-tb-section -)
        income-body (neg-report-fn s/income-headings)
        expenditure-body (pos-report-fn s/expenditure-headings)
        [income-total income-line] (((s/sum-section tb) -) s/income-headings)
        [expenditure-total expenditure-line] (((s/sum-section tb) +) s/expenditure-headings)
        profit (+ (- income-total) (- expenditure-total))
        ]
    (u/pp (concat
            (conj income-body income-line)
            (conj expenditure-body expenditure-line)
            [((u/left-pad-spaces 40) (u/no-dec-pl (str profit)))]))))
