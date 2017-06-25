(ns app.domain-ui-helpers
  (:require [om.dom :as dom]
            [untangled.ui.forms :as f]
            [app.util :as u]
            [app.panels :as p]
            [app.forms-helpers :as fh]))

(def quarters [:q1 :q2 :q3 :q4])
(def months [:jan :feb :mar :apr :may :jun :jul :aug :sep :oct :nov :dec])

(def period-kw->period-name
  {:q1  "Q1"
   :q2  "Q2"
   :q3  "Q3"
   :q4  "Q4"
   :jan "Jan" :feb "Feb" :mar "Mar" :apr "Apr" :may "May" :jun "Jun" :jul "Jul"
   :aug "Aug" :sep "Sep" :oct "Oct" :nov "Nov" :dec "Dec"})

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

(defn all-periods [{:keys [potential-data/period-type]}]
  (case period-type
    :period-type/quarterly quarters
    :period-type/monthly months))

(defn period->year [{:keys [period/tax-year period/year]}]
  (or tax-year year))

;;
;; Looks confusing because there are two concepts of period.
;; Here going from a period within a year to a period without
;; the year context (eg. :q1).
;;
(defn period->period [{:keys [period/quarter period/month]}]
  (or quarter month))

;;
;; The default year and period s/be worked out as the last ones in potential data
;;
(defn latest-year [{:keys [potential-data/latest-period]}]
  (assert (map? latest-period))
  (-> latest-period period->year))

(defn latest-period [{:keys [potential-data/latest-period]}]
  (assert (map? latest-period))
  (-> latest-period period->period))

(defn commencing-year [{:keys [potential-data/commencing-period]}]
  (assert (map? commencing-period))
  (-> commencing-period period->year))

(defn commencing-period [{:keys [potential-data/commencing-period]}]
  (assert (map? commencing-period))
  (-> commencing-period period->period))

;;
;; If the year we are wanting isn't the first or last year of our org's existence,
;; then all the periods will be available.
;;
(defn range-of-periods [yr potential-data]
  (assert (map? potential-data))
  (let [period-type (:potential-data/period-type potential-data)
        periods (condp = period-type
                  :period-type/quarterly quarters
                  :period-type/monthly months)
        starting (commencing-year potential-data)
        finishing (latest-year potential-data)
        year (u/kw->number yr)
        ]
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
  (u/log-off (str "POT: " potential-data))
  (assert potential-data (str "No potential data"))
  (let [starting (commencing-year potential-data)
        finishing (latest-year potential-data)]
    (->> (u/numerical-range starting finishing)
         reverse
         vec)))

(def report-kw->report-name
  {:report/profit-and-loss "Profit & Loss"
   :report/balance-sheet   "Balance Sheet"
   :report/trial-balance   "Trial Balance"
   :report/big-items-first "Biggest first"})

(def years-options-generator (fh/options-generator
                               range-of-years
                               #(f/option (keyword (str %)) (str %))
                               #(-> % first str keyword)))

(def periods-options-generator (fh/options-generator
                                 range-of-periods
                                 #(f/option % (period-kw->period-name %))
                                 last))

(def reports-options-generator (fh/options-generator
                                 #(:potential-data/possible-reports %2)
                                 #(f/option % (report-kw->report-name %))
                                 first))

;;
;; Useful for things like changing options in fields in panels
;;
(def request-form-ident [:user-request/by-id p/USER_REQUEST_FORM])
(def year-field-whereabouts (conj request-form-ident :request/year))
(def period-field-whereabouts (conj request-form-ident :request/period))
(def report-field-whereabouts (conj request-form-ident :request/report))

(def request-form-input-options (partial fh/input-options request-form-ident))
(def year-options-whereabouts (request-form-input-options :request/year))
(def period-options-whereabouts (request-form-input-options :request/period))
(def report-options-whereabouts (request-form-input-options :request/report))

(def request-form-input-default-value (partial fh/input-default-value request-form-ident))
(def year-default-value-whereabouts (request-form-input-default-value :request/year))
(def period-default-value-whereabouts (request-form-input-default-value :request/period))
(def report-default-value-whereabouts (request-form-input-default-value :request/report))

