(ns accounting.test-rules-data
  (:require [accounting.data.seaweed :as rd]
            [accounting.util :as u]))

(defn show-period-specific-seaweed-rules []
  (u/pp rd/q3-2017-rules))
