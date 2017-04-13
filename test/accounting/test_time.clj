(ns accounting.test-time
  (:require [accounting.time :as t]))

(defn x-1 []
  (t/long-date-str->date "21 Mar 2017"))

(defn x-2 []
  (t/short-date-str->date "31/03/2017"))

(def current-period
  {:period/year    2017
   :period/quarter :q2})

(defn x-3 []
  (->> (t/end-period-moment current-period)
       t/format-time))


