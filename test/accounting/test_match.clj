(ns accounting.test-match
  (:require [accounting.match :as m]
            [accounting.util :as u]
            [accounting.context :as con]))

(defn x-1 []
  (u/pp (m/bank-rules #{:bank/anz-coy} con/current-rules)))

(defn x-2 []
  (->> con/current-rules
       (take 3)
       u/pp))




