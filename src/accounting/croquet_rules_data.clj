(ns accounting.croquet-rules-data
  (:require [accounting.meta.croquet :as meta]
            [accounting.util :as u]
            [accounting.time :as t]))

(def bendigo (first meta/bank-accounts))

(def rules
  {[bendigo :income/membership-fees] [{:logic-operator :and
                                       :conditions     [[:out/desc :starts-with "DIRECT CREDIT"]
                                                        [:out/amount :equals 235.00M]]}]})


