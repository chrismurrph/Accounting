(ns accounting.test-time
  (:require [accounting.time :as t]))

(defn x-1 []
  (t/long-date-str->date "21 Mar 2017"))

(defn x-2 []
  (t/short-date-str->date "31/03/2017"))

(def current-period
  {:period/tax-year 2017
   :period/quarter  :q2})

(defn x-3 []
  (->> (t/end-period-moment current-period)
       t/format-time))

;;
;; I don't know why t/equal? exists:
;;
(defn x-4 []
  (let [date-1 (t/long-date-str->date "22 Mar 2017")
        date-2 (t/long-date-str->date "22 Mar 2017")]
    [(= date-1 date-2) (t/equal? date-1 date-2)]))

;;
;; Normal set operations work too
;;
(defn x-5 []
  (let [date-1 (t/long-date-str->date "21 Mar 2017")
        date-2 (t/long-date-str->date "22 Mar 2017")
        date-3 (t/long-date-str->date "23 Mar 2017")
        dates #{date-1 date-2 date-3}
        date-4 (t/long-date-str->date "24 Mar 2017")]
    (dates date-4)))


