(ns app.ui-helpers
  (:require [om.dom :as dom]
            [untangled.ui.forms :as f]
            [app.util :as u]))

(def quarters [:q1 :q2 :q3 :q4])
(def months [:jan :feb :mar :apr :may :jun :jul :aug :sep :oct :nov :dec])

(def period-kw->period-name
  {:q1 "Q1"
   :q2 "Q2"
   :q3 "Q3"
   :q4 "Q4"
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

;;
;; e.g. begin :q2 end :q3
;; begin and end are both from within the same year
;;
#_(defn periods-range [period-type begin end]
  (assert (and begin end))
  (let [periods (case period-type
                  :period-type/quarterly quarters
                  :period-type/monthly months)]
    (*periods-range periods begin end)))



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
(defn range-of-years [potential-data]
  (u/log-off (str "POT: " potential-data))
  (assert potential-data (str "No potential data"))
  (let [starting (commencing-year potential-data)
        finishing (latest-year potential-data)]
    (->> (u/numerical-range starting finishing)
         reverse
         vec)))

(defn field-with-label
  "A non-library helper function, written by you to help lay out your form."
  ([comp form name label] (field-with-label comp form name label nil))
  ([comp form name label validation-message]
   (assert label)
   (dom/div #js {:className (str "form-group" (if (f/invalid? form name) " has-error" ""))}
            (dom/label #js {:className "col-sm-2" :htmlFor name} label)
            (dom/div #js {:className "col-sm-10"} (f/form-field comp form name))
            (when (and validation-message (f/invalid? form name))
              (dom/span #js {:className (str "col-sm-offset-2 col-sm-10" name)} validation-message)))))

;;
;; Can be used in a mutation to assoc-in new options
;;
(defn input-options [[table-name id] field]
  [table-name id ::f/form :elements/by-name field :input/options])

;;
;; Useful for things like changing options in fields in panels
;;
(def year-options-whereabouts (input-options [:user-request/by-id 'USER-REQUEST-FORM] :request/year))
(def period-options-whereabouts (input-options [:user-request/by-id 'USER-REQUEST-FORM] :request/period))

