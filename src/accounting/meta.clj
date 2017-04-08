(ns accounting.meta
  (:require [clojure.string :as s]))

(def quarters #{:q1 :q2 :q3 :q4})
(def bank-accounts [:amp :anz-coy :anz-visa])

(defn quarter->str [quarter]
  (-> quarter name s/capitalize))

;; Always start with quarter even thou in that directory
;; "Q3_AMP_TransactionHistory.csv"
(def file-names
  {:amp "_AMP_TransactionHistory.csv"
   :anz-coy "_ANZ_coy.csv"
   :anz-visa "_ANZ_credit_card.csv"})

;; year is year ending
(def years #{2017})
(def data-root "/home/chris/state/Google Drive/data/ATO/Tax Year 2017/Q3")
(def data-root "/home/chris/state/Google Drive/data/ATO/")

(defn -dir-for [year quarter]
  (assert (quarters quarter))
  (assert (years year))
  (let [tax-year (str "Tax Year " year)
        qtr (quarter->str quarter)]
    (str data-root tax-year "/" qtr)))

(defn bank-period->file-name [bank year quarter]
  (let [qtr (quarter->str quarter)
        file-name (str qtr (file-names bank))
        file-path (str (-dir-for year quarter) "/" file-name)]
    file-path))

;; :mock is fields I've manually added on purpose when file format changed and I did not want to upset Xero
;; :ignore is a field that holds no purpose for accounting
(def amp-structure [:date :mock :desc :amount :ignore :ignore :mock])

(def heading->parse-fn
  {:date identity
   :mock identity
   :desc identity
   :amount identity})

(defn x-1 []
  (bank-period->file-name :amp 2017 :q3))
