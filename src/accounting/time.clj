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

(def kw->month
  {:jan 1
   :feb 2
   :mar 3
   :apr 4
   :may 5
   :jun 6
   :jul 7
   :aug 8
   :sep 9
   :oct 10
   :nov 11
   :dec 12})

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
(def show format-date)

(def -time-formatter (f/formatter "dd/MM/yyyy HH:mm:ss"))
(def format-time #(f/unparse -time-formatter %))

(defn show-record [record]
  (let [f-ed-out-date (-> record :out/date format-date)
        f-ed-when-date (-> record :when format-date)]
    (-> record
        (assoc :out/date f-ed-out-date)
        (assoc :when f-ed-when-date))))

(def change-year-for-quarter
  {:q1 dec
   :q2 dec
   :q3 identity
   :q4 identity})

(defn -start-quarter-moment [tax-year quarter]
  (assert tax-year)
  (let [calendar-year ((change-year-for-quarter quarter) tax-year)]
    (->> (quarter->begin-month quarter)
         (t/first-day-of-the-month calendar-year))))

(defn -end-quarter-moment [tax-year quarter]
  (assert tax-year)
  (assert quarter)
  (let [year ((change-year-for-quarter quarter) tax-year)]
    (->> (quarter->end-month quarter)
         (t/last-day-of-the-month year))))

(defn -start-month-moment [month-kw year]
  (->> month-kw
       kw->month
       (t/first-day-of-the-month year)))

(defn -end-month-moment [month-kw year]
  (->> month-kw
       kw->month
       (t/last-day-of-the-month year)))

(defn start-period-moment [{:keys [period/tax-year period/quarter period/year period/month]}]
  ;(println "==" tax-year (nil? tax-year) year (nil? year))
  (if (nil? tax-year)
    (-start-month-moment month year)
    (-start-quarter-moment tax-year quarter)))

(defn end-period-moment [{:keys [period/tax-year period/quarter period/year period/month]}]
  (if (nil? tax-year)
    (-end-month-moment month year)
    (-end-quarter-moment tax-year quarter)))

(defn equal? [this that]
  (t/equal? this that))

(defn gt? [this that]
  (t/after? this that))

(defn gte? [this that]
  (or (t/after? this that)
      (t/equal? this that)))

(defn lt? [this that]
  (t/before? this that))

(defn after-begin-bound? [begin-moment]
  (fn [date]
    (let [res (or (t/after? date begin-moment)
                  (t/equal? date begin-moment))]
      ;(println "after-begin-bound? " (show begin-moment) (show date) res)
      res)))

(defn before-end-bound? [end-moment]
  (fn [date]
    (let [res (or (t/before? date end-moment)
                  (t/equal? date end-moment))]
      ;(println "before-end-bound? " (show end-moment) (show date) res)
      res)))

;;
;; Simplest way is if the dates we use always abut each other. We are not going to
;; actually have times so s/not need to think about which part of the day.
;; 31st and 1st abut each other and we always do inclusive selections: <= on both sides.
;; The recalc date can be thought of as a previous period. So we add one day to it for the start
;; moment.
;;
(defn within-range? [start-moment end-moment]
  (let [begin? (after-begin-bound? start-moment)
        end? (before-end-bound? end-moment)]
    (fn [date]
      (and (begin? date)
           (end? date)))))

;; I'm assuming these Java dates are immutable - don't know why equals? exists in the clj-time library
(defn in-set? [dates date]
  ;(println (str "See if " (format-date date) " is in " (mapv format-date dates)))
  (dates date))

(defn within-period? [period date]
  (assert period)
  (assert date)
  (let [start-moment (start-period-moment period)
        end-moment (end-period-moment period)
        res ((within-range? start-moment end-moment) date)]
    res))

(defn get-within [date-kw]
  (fn [begin end xs]
    (let [within? (within-range? begin end)]
      (->> xs
           (sort-by date-kw)
           ;; later use a combination of drop-while and take-while and don't use within but less and greater than fns
           (filter #(-> % date-kw within?))))))

(defn add-day [date]
  (t/plus date (t/days 1)))

