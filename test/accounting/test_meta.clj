(ns accounting.test-meta
  (:require [accounting.meta :as m]))

(defn x-1 []
  (m/bank-period->file-name :bank/amp {:period/year 2017 :period/quarter :q3}))
