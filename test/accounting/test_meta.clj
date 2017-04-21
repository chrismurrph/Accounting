(ns accounting.test-meta
  (:require [accounting.meta.common :as m]))

;; :bank/amp {:period/tax-year 2017 :period/quarter :q3}

(defn x-1 []
  (m/bank-period->file-name (m/human-meta :seaweed)))
