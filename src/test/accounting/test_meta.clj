(ns accounting.test-meta
  (:require [accounting.data.meta.common :as m]
            [clojure.test :as test]))

(test/deftest csv-file-name
  (test/is (= ((m/bank-period->file-name (m/human-meta :seaweed)) :bank/amp {:period/tax-year 2017 :period/quarter :q3})
              "/home/chris/state/Google Drive/data/ATO/Tax Year 2017/Q3/Q3_AMP_TransactionHistory.csv")))
