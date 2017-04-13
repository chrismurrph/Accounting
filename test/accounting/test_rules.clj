(ns accounting.test-rules
  (:require [accounting.rules :as r]
            [accounting.util :as u]
            [accounting.context :as con]))

(defn x-1 []
  (u/pp (r/bank-rules #{:bank/anz-coy} con/current-rules)))

(defn x-2 []
  (->> con/current-rules
       (take 3)
       u/pp))




