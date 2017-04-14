(ns accounting.time
  (:require [accounting.util :as u]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(def str->month
  {"Jan" 1
   "Feb" 2
   "Mar" 3
   "Apr" 4
   "May" 5
   "Jun" 6
   "Jul" 7
   "Aug" 8
   "Sep" 9
   "Oct" 10
   "Nov" 11
   "Dec" 12})

(def quarter->begin-month
  {:q1 7
   :q2 10
   :q3 1
   :q4 4})

(def quarter->end-month
  {:q1 9
   :q2 12
   :q3 3
   :q4 6})

(defn long-date-str->date [x]
  (let [[_ day m year] (re-matches #"(\d+) (\w+) (\d+)" x)
        month (str->month m)]
    (assert (-> month nil? not))
    ;(println day month year)
    (t/date-time (u/to-int year) month (u/to-int day))))

;;
;; Saying 'inclusive range' the user is assuming end of day for the second date,
;; so we affect this here. Note that end-period-moment also adds a day
;;
(defn inclusive-range [this-long-date-str that-long-date-str]
  [(long-date-str->date this-long-date-str)
   (t/plus (long-date-str->date that-long-date-str) (t/days 1))])

(defn short-date-str->date [x]
  (let [[_ d m y] (re-matches #"(\d+)/(\d+)/(\d+)" x)]
    (t/date-time (u/to-int y) (u/to-int m) (u/to-int d))))

(def -date-formatter (f/formatter "dd/MM/yyyy"))
(def format-date #(f/unparse -date-formatter %))

(def -time-formatter (f/formatter "dd/MM/yyyy HH:mm:ss"))
(def format-time #(f/unparse -time-formatter %))

(def change-for-quarter
  {:q1 dec
   :q2 dec
   :q3 identity
   :q4 identity})

(defn start-period-moment [{:keys [period/tax-year period/quarter]}]
  (let [year ((change-for-quarter quarter) tax-year)]
    (->> (quarter->begin-month quarter)
         (t/first-day-of-the-month year))))

(defn end-period-moment [{:keys [period/tax-year period/quarter]}]
  (let [year ((change-for-quarter quarter) tax-year)]
    (-> (->> (quarter->end-month quarter)
             (t/last-day-of-the-month year))
        (t/plus (t/days 1)))))

(defn equal? [this that]
  (t/equal? this that))

;;
;; Events are assumed to happen right at the beginning of days.
;; Consider event on first day of a quarter.
;; We want it to be in the correct quarter. So always:
;; begin-quarter <= event < end-quarter
;; Events occuring on last day will be 24 hours before end of quarter so < is fine!
;;
(defn within-range? [start-moment end-moment date]
  #_(when (= "08/02/2017" (format-date date))
    (println (format-time date) (format-time end-moment)))
  (and (or (t/after? date start-moment)
           (t/equal? date start-moment))
       (t/before? date end-moment)))

;; I'm assuming these Java dates are immutable - don't know why equals? exists in the clj-time library
(defn in-set? [dates date]
  (dates date))

(defn within-period? [period date]
  (let [start-moment (start-period-moment period)
        end-moment (end-period-moment period)]
    (within-range? start-moment end-moment date)))
