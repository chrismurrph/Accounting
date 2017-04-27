(ns accounting.data.test-croquet
  (:require [accounting.data.croquet :as data]
            [accounting.time :as t]
            [accounting.util :as u]))

(defn x-1 []
  (let [begin (t/short-date-str->date "31/01/2017")
        end (t/short-date-str->date "21/02/2017")
        within? (t/get-within :when)]
    (->> data/receive-cash
         (within? begin end)
         u/pp)))
