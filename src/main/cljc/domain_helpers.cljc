(ns cljc.domain-helpers
  (:require [cljc.utils :as u]))

(def quarters [:period.quarter/q1 :period.quarter/q2 :period.quarter/q3 :period.quarter/q4])
(def months [:period.month/jan :period.month/feb :period.month/mar :period.month/apr :period.month/may :period.month/jun
             :period.month/jul :period.month/aug :period.month/sep :period.month/oct :period.month/nov :period.month/dec])

(defn all-periods [{:keys [potential-data/period-type]}]
  (case period-type
    :period-type/quarterly quarters
    :period-type/monthly months))

(defn period->year [{:keys [actual-period/year]}]
  year)

;;
;; Looks confusing because there are two concepts of period.
;; Here going from a period within a year to a period without
;; the year context (eg. :q1).
;;
(defn period->period [{:keys [actual-period/period]}]
  (let [{:keys [period/type period/quarter period/month]} period]
    (if (= :period.type/quarterly (:db/ident type))
      (:db/ident quarter)
      (:db/ident month))))

;;
;; The default year and period s/be worked out as the last ones in potential data
;;
(defn latest-year [{:keys [organisation/latest-period]}]
  (assert (map? latest-period))
  (-> latest-period period->year))

(defn latest-period [{:keys [organisation/latest-period]}]
  (assert (map? latest-period))
  (-> latest-period period->period))

(defn commencing-year [{:keys [organisation/commencing-period]}]
  (assert (map? commencing-period) (u/assert-str "commencing-period" commencing-period))
  (-> commencing-period period->year))

(defn commencing-period [{:keys [organisation/commencing-period]}]
  (assert (map? commencing-period) (u/assert-str "commencing-period" commencing-period))
  (-> commencing-period period->period))

(defn following-period [periods m]
  (->> periods
       (drop-while #(not= m %))
       next
       first))

(defn *periods-range [periods begin end]
  (u/log-off (str "begin, end: " begin end))
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
  (let [period-type (-> potential-data :organisation/period-type :db/ident)
        _ (println "period-type: " potential-data)
        periods (condp = period-type
                  :organisation.period-type/quarterly quarters
                  :organisation.period-type/monthly months
                  (assert false (u/assert-str "period-type" period-type)))
        starting (commencing-year potential-data)
        finishing (latest-year potential-data)
        year (u/kw->number yr)]
    (u/log-off potential-data)
    (u/log-off (str starting ", " finishing ", " year))
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
  (u/log-on (str "POT: " potential-data))
  (assert potential-data (str "No potential data"))
  (let [starting (commencing-year potential-data)
        finishing (latest-year potential-data)]
    (->> (u/numerical-range starting finishing)
         reverse
         vec)))

