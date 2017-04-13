(ns accounting.test-meta
  (:require [accounting.meta :as m]))

(defn x-1 []
  (m/bank-period->file-name :bank/amp {:period/tax-year 2017 :period/quarter :q3}))
