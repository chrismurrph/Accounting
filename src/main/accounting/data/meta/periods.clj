(ns accounting.data.meta.periods
  (:require [clojure.string :as s]))

(def months [:jan :feb :mar :apr :may :jun :jul :aug :sep :oct :nov :dec])
(def months-set (set months))
(def quarters [:q1 :q2 :q3 :q4])
(def quarters-set (set quarters))

(defn quarter->str [quarter]
  (-> quarter name s/capitalize))

(defn month->str [month]
  (-> month name s/capitalize))

(defn make-quarter [year quarter]
  (assert (integer? year))
  (assert (keyword? quarter))
  (assert (quarter quarters-set))
  {:period/tax-year year
   :period/quarter  quarter})

(defn make-month [year month]
  (assert (integer? year))
  (assert (keyword? month))
  (assert (month months-set))
  {:period/year  year
   :period/month month})

