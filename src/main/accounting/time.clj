(ns accounting.time
  (:require [accounting.util :as u]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [accounting.data.meta.periods :as periods]))

(defn joda-set->java [s]
  (assert (or (nil? s) (set? s)) (str "Not set but: " (type s)))
  (when s (into #{} (map c/to-date s))))

(defn java-set->joda [s]
  (assert (or (nil? s) (set? s)) (str "Not set but: " (type s)))
  (when s (into #{} (map c/from-date s))))

(defn joda-vector->java [v]
  (assert (or (nil? v) (vector? v)) (str "Not vector but: " (type v)))
  (when v (mapv c/to-date v)))

(defn java-vector->joda [v]
  (assert (or (nil? v) (vector? v)) (str "Not vector but: " (type v)))
  (when v (mapv c/from-date v)))

(defn civilize-joda [m]
  (assert (map? m))
  (-> m
      (update :time-slot joda-vector->java)
      (update :on-dates joda-set->java)))

(defn wildify-java [m]
  (assert (map? m))
  (-> m
      (update :time-slot java-vector->joda)
      (update :on-dates java-set->joda)))


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

(def month-kw->month
  {:period.month/jan 1
   :period.month/feb 2
   :period.month/mar 3
   :period.month/apr 4
   :period.month/may 5
   :period.month/jun 6
   :period.month/jul 7
   :period.month/aug 8
   :period.month/sep 9
   :period.month/oct 10
   :period.month/nov 11
   :period.month/dec 12})

(def quarter->begin-month
  {:period.quarter/q1 7
   :period.quarter/q2 10
   :period.quarter/q3 1
   :period.quarter/q4 4})

(def quarter->end-month
  {:period.quarter/q1 9
   :period.quarter/q2 12
   :period.quarter/q3 3
   :period.quarter/q4 6})

(defn prior-quarter [{:keys [period/tax-year period/quarter]}]
  (assert tax-year)
  (assert (number? tax-year))
  (assert quarter)
  (let [at-yr-start? (= quarter :period.quarter/q1)
        new-year (cond-> tax-year at-yr-start? dec)
        new-quarter (if at-yr-start? :period.quarter/q4 (nth periods/quarters (dec (u/index-of quarter periods/quarters))))]
    {:period/tax-year new-year :period/quarter new-quarter}))

(defn prior-month [{:keys [period/year period/month]}]
  (assert year)
  (assert (number? year))
  (assert month)
  (let [at-yr-start? (= month :period.month/jan)
        new-year (cond-> year at-yr-start? dec)
        new-month (if at-yr-start? :period.month/dec (nth periods/months (dec (u/index-of month periods/months))))]
    {:period/year new-year :period/month new-month}))

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

(defn day-of-month [date]
  (->> date
       (f/unparse (f/formatter "dd"))
       Long/parseLong))

(def -time-formatter (f/formatter "dd/MM/yyyy HH:mm:ss"))
(def format-time #(f/unparse -time-formatter %))

(defn show-trans-record [record]
  (let [f-ed-out-date (-> record :out/date format-date)]
    (-> record
        (assoc :out/date f-ed-out-date))))

(defn show-ledger-record [record]
  (let [f-ed-when-date (-> record :when format-date)]
    (-> record
        (assoc :when f-ed-when-date))))

(def change-year-for-quarter
  {:period.quarter/q1 dec
   :period.quarter/q2 dec
   :period.quarter/q3 identity
   :period.quarter/q4 identity})

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
  (assert month-kw)
  (->> month-kw
       month-kw->month
       (t/first-day-of-the-month year)))

(defn -end-month-moment [month-kw year]
  (->> month-kw
       month-kw->month
       (t/last-day-of-the-month year)))

(defn start-period-moment-orig [{:keys [period/tax-year period/quarter period/year period/month] :as in}]
  ;(println "==" tax-year (nil? tax-year) year (nil? year))
  (assert (or tax-year year) in)
  (assert (or quarter month))
  (if (nil? tax-year)
    (-start-month-moment month year)
    (-start-quarter-moment tax-year quarter)))

(defn start-period-moment-datomic [{:keys [actual-period/year actual-period/period] :as in}]
  (let [{:keys [period/type period/quarter]} period]
    (if (= :period.type/quarterly type)
      (-start-quarter-moment year quarter)
      (assert false "Not yet doing monthly here")
      #_(-start-month-moment month year))))

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
(defn within-range-hof? [start-moment end-moment]
  (let [begin? (after-begin-bound? start-moment)
        end? (before-end-bound? end-moment)]
    (fn [date]
      (and (begin? date)
           (end? date)))))

;; I'm assuming these Java dates are immutable - don't know why equals? exists in the clj-time library
(defn in-set? [dates date]
  ;(println (str "See if " (format-date date) " is in " (mapv format-date dates)))
  (dates date))

(defn within-period? [actual-period date]
  (assert actual-period)
  (assert date)
  (let [start-moment (start-period-moment-datomic actual-period)
        end-moment (end-period-moment actual-period)
        res ((within-range-hof? start-moment end-moment) date)]
    res))

(defn get-within [date-kw]
  (fn [begin end xs]
    (let [within? (within-range-hof? begin end)]
      (->> xs
           (sort-by date-kw)
           ;; later use a combination of drop-while and take-while and don't use within but less and greater than fns
           (filter #(-> % date-kw within?))))))

(defn add-day [date]
  (t/plus date (t/days 1)))

