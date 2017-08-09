(ns accounting.test-time
  (:require [accounting.time :as t]
            [clojure.test :as test]))

(defn x-1 []
  (t/long-date-str->date "21 Mar 2017"))

(defn x-2 []
  (t/short-date-str->date "31/03/2017"))

(def current-period
  {:period/tax-year 2017
   :period/quarter  :q2})

(defn x-3 []
  (->> (t/end-actual-period-moment current-period)
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

(test/deftest test-within
  (let [act-period #:actual-period{:year 2018, :quarter :q1, :type :quarterly}
        slot #:time-slot{:start-at (t/short-date-str->date "04/07/2017")
                         :end-at (t/short-date-str->date "15/07/2017")}]
    (test/is (t/intersects? act-period slot))))

(test/deftest test-within-no-end
  (let [act-period #:actual-period{:year 2017, :quarter :q1, :type :quarterly}
        slot #:time-slot{:start-at (t/short-date-str->date "04/07/2017")
                         :end-at nil}]
    (test/is (not (t/intersects? act-period slot)))))


