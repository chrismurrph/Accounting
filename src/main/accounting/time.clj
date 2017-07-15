(ns accounting.time
  (:require [accounting.util :as u]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [accounting.data.meta.periods :as periods]
            [cljc.utils :as us]))

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

(defn wildify-java-1 [m]
  (assert (map? m))
  (assert (every? #{:time-slot :on-dates} (keys m)))
  (-> m
      (update :time-slot java-vector->joda)
      (update :on-dates java-set->joda)))

(defn wildify-java-2 [m]
  (assert (map? m))
  (assert (every? #{:time-slot/start-at :time-slot/end-at} (keys m)))
  (-> m
      (update :time-slot/start-at (fn [d] (c/from-date d)))
      (update :time-slot/end-at (fn [d] (c/from-date d)))))

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
    ;Validate does
    ;(assert d (str "No day from: <" x ">"))
    ;(assert m (str "No month from: <" x ">"))
    ;(assert y (str "No year from: <" y ">"))
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
  {:q1 dec
   :q2 dec
   :q3 identity
   :q4 identity})

(defn -start-quarter-moment [year quarter]
  (assert year)
  (assert quarter)
  (let [calendar-year ((change-year-for-quarter quarter) year)]
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

(defn start-actual-period-moment [{:keys [actual-period/year actual-period/month
                                          actual-period/quarter actual-period/type] :as in}]
  (if (= :quarterly type)
    (-start-quarter-moment year quarter)
    (assert false (str "Not yet doing monthly here: " type))
    #_(-start-month-moment month year)))

(defn end-actual-period-moment [{:keys [actual-period/year actual-period/quarter
                                        actual-period/type actual-period/month]}]
  (if (= :quarterly type)
    (-end-quarter-moment year quarter)
    (-end-month-moment month year)))

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
    (println (type date) (type begin-moment))
    (let [res (or (t/after? date begin-moment)
                  (t/equal? date begin-moment))]
      (println "after-begin-bound? it:" (show date) ", beg-bound:" (show begin-moment) res)
      res)))

(defn before-end-bound? [end-moment]
  (fn [date]
    (let [res (or (t/before? date end-moment)
                  (t/equal? date end-moment))]
      (println "before-end-bound? it:" (show date) ", end-bound:" (show end-moment) res)
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

(defn within-actual-period? [date actual-period]
  (assert actual-period)
  (assert (:actual-period/type actual-period) (us/assert-str "actual-period" actual-period))
  (when date
    (let [start-moment (start-actual-period-moment actual-period)
          end-moment (end-actual-period-moment actual-period)
          res ((within-range-hof? start-moment end-moment) date)]
      res)))

;;
;; To become db function. We are always looking at a current time period.
;; Some accounts are no longer being used. Or our period might be before
;; an account existed. In either case we don't want the user to see these
;; accounts.
;;
(defn intersects? [{:keys [time-slot/start-at time-slot/end-at]} actual-period]
  (let [start-actual-period (start-actual-period-moment actual-period)
        end-actual-period (end-actual-period-moment actual-period)
        within-bank-account-duration-f? (within-range-hof? start-at end-at)]
    (or (within-bank-account-duration-f? start-actual-period)
        (within-bank-account-duration-f? end-actual-period))))

(defn get-within [date-kw]
  (fn [begin end xs]
    (let [within? (within-range-hof? begin end)]
      (->> xs
           (sort-by date-kw)
           ;; later use a combination of drop-while and take-while and don't use within but less and greater than fns
           (filter #(-> % date-kw within?))))))

(defn add-day [date]
  (t/plus date (t/days 1)))

