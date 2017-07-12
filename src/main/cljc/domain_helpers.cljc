(ns cljc.domain-helpers
  (:require [cljc.utils :as u]))

(def quarters [:q1 :q2 :q3 :q4])
(def months [:jan :feb :mar :apr :may :jun :jul :aug :sep :oct :nov :dec])

(defn all-periods [{:keys [potential-data/period-type]}]
  (case period-type
    :quarterly quarters
    :monthly months))

(defn actual-period->year [{:keys [actual-period/year]}]
  (assert year)
  year)

;;
;; Looks confusing because there are two concepts of period.
;; Here going from a period within a year to a period without
;; the year context (eg. :q1).
;;
(defn actual-period->period [actual-period]
  (let [{:keys [actual-period/type actual-period/quarter actual-period/month]} actual-period
        _ (assert type (str "No type in actual-period: " actual-period))
        ]
    (if (= :quarterly type)
      quarter
      month)))

;;
;; The default year and period s/be worked out as the last ones in potential data
;;
(defn latest-year [{:keys [organisation/timespan]}]
  (assert (map? timespan))
  (-> timespan :timespan/latest-period actual-period->year))

(defn latest-period [{:keys [organisation/timespan]}]
  (assert (map? timespan))
  (-> timespan :timespan/latest-period actual-period->period))

(defn commencing-year [{:keys [organisation/timespan]}]
  (assert (map? timespan) (u/assert-str "timespan" timespan))
  (-> timespan :timespan/commencing-period actual-period->year))

(defn commencing-period [{:keys [organisation/timespan]}]
  (println timespan)
  (assert (map? timespan) (u/assert-str "timespan" timespan))
  (-> timespan :timespan/commencing-period actual-period->period))

(defn following-period [periods m]
  (->> periods
       (drop-while #(not= m %))
       next
       first))

(defn *periods-range [periods begin end]
  (u/log-on (str "begin, end: <" begin "," end ">"))
  (assert begin)
  (assert end)
  (->> periods
       (drop-while #(not= begin %))
       (take-while #(not= (following-period periods end) %))
       vec))
;;
;; If the year we are wanting isn't the first or last year of our org's existence,
;; then all the periods will be available.
;;
(defn range-of-periods [yr potential-data]
  (assert (map? potential-data) (u/assert-str "potential-data" potential-data))
  (let [period-type (-> potential-data :organisation/period-type)
        _ (assert period-type)
        _ (println "period-type:" period-type)
        periods (condp = period-type
                  :quarterly quarters
                  :monthly months
                  (assert false (u/assert-str "period-type" period-type)))
        starting (commencing-year potential-data)
        finishing (latest-year potential-data)
        year (u/kw->number yr)]
    (u/log-on (str ">>" periods))
    (u/log-on (str ">" starting ", " finishing ", " year))
    (cond
      (= starting finishing year)
      (*periods-range periods
                      (commencing-period potential-data)
                      (latest-period potential-data))

      (= finishing year)
      (*periods-range periods (first periods) (latest-period potential-data))

      (= starting year)
      (*periods-range periods (commencing-period potential-data) (last periods))

      :else
      (all-periods potential-data))))

;;
;; Create the full range given the ends, then returning the most recent years first
;;
(defn range-of-years [_ potential-data]
  (u/log-off (str "POT: " potential-data))
  (assert potential-data (str "No potential data"))
  (let [starting (commencing-year potential-data)
        finishing (latest-year potential-data)]
    ;(println starting finishing)
    (->> (u/numerical-range starting finishing)
         reverse
         vec)))

