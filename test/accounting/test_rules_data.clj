(ns accounting.test-rules-data
  (:require [accounting.data.seaweed :as rd]
            [accounting.util :as u]))

(defn x-1 []
  (u/pp rd/q3-2017-rules))
