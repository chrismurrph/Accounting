(ns accounting.data.meta.common
  (:require [clojure.string :as s]
            [accounting.data.meta.seaweed :as seasoft]
            [accounting.data.meta.croquet :as croquet]
            [accounting.data.meta.periods :as periods]))

(defn -financial-dir-for [{:keys [tax-years data-root]} {:keys [period/tax-year period/quarter] :as period}]
  (assert tax-years)
  (assert data-root)
  (assert (periods/quarters-set quarter) (str quarter " is not considered a quarter"))
  (assert (tax-years tax-year))
  (let [tax-year (str "Tax Year " tax-year)
        qtr (periods/quarter->str quarter)]
    (str data-root tax-year "/" qtr)))

(defn -charity-dir-for [{:keys [years data-root]} {:keys [period/year period/month] :as period}]
  (assert years)
  (assert data-root)
  (assert (periods/months-set month) (str month " is not considered a month"))
  (assert (years year))
  (let [year (str "Year " year)
        mnth (periods/month->str month)]
    (str data-root year "/" mnth)))

;;
;; Assuming charities work in months and real businesses in quarters
;;
(defn bank-period->file-name [{:keys [file-names tax-years years data-root] :as meta}]
  (fn [bank period]
    (cond
      years (let [{:keys [period/year period/month]} period
                  mnth (periods/month->str month)
                  file-name (file-names bank)
                  file-path (str (-charity-dir-for meta period) "/" file-name)]
              file-path)
      tax-years (let [{:keys [period/tax-year period/quarter]} period
                      qtr (periods/quarter->str quarter)
                      file-name (str qtr (file-names bank))
                      file-path (str (-financial-dir-for meta period) "/" file-name)]
                  file-path))))

(def human-meta
  {:seaweed {:file-names seasoft/file-names
             :splits seasoft/splits
             :tax-years seasoft/tax-years
             :data-root seasoft/data-root
             :bank-accounts seasoft/bank-accounts
             :period-type :period-type/quarterly}
   :croquet {:file-names croquet/file-names
             :splits croquet/splits
             :years croquet/years
             :data-root croquet/data-root
             :bank-accounts croquet/bank-accounts
             :period-type :period-type/monthly}})
