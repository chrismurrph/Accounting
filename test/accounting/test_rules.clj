(ns accounting.test-rules
  (:require [accounting.rules :as r]
            [accounting.util :as u]))

(defn x-1 []
  (u/pp (r/bank-rules #{:bank/anz-coy} r/current-rules)))

(defn x-2 []
  (->> r/current-rules
       (take 3)
       u/pp))




