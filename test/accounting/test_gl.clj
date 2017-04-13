(ns accounting.test-gl
  (:require [accounting.gl :as g]))

(defn x-1 []
  (g/apply-trans g/general-ledger g/example-transaction-capital))
