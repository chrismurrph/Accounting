(ns accounting.meta
  (:require [clojure.string :as s]
            [clojure.set :as set]))

(def quarters #{:q1 :q2 :q3 :q4})

(def bank-accounts [:bank-amp :bank-anz-coy :bank-anz-visa])
(def other-asset-accounts #{})
(def income-accounts #{:bank-interest :mining-sales})
(def tax-expense-accounts #{})
(def non-tax-expense-accounts #{:ato-payment})
(def negative-equity-account #{:drawings})

(def all-accounts (set/union (set bank-accounts)
                             other-asset-accounts
                             income-accounts
                             tax-expense-accounts
                             non-tax-expense-accounts
                             negative-equity-account))
;(println all-accounts)

(defn quarter->str [quarter]
  (-> quarter name s/capitalize))

;; Always start with quarter even thou in that directory
;; "Q3_AMP_TransactionHistory.csv"
(def file-names
  {:bank-amp "_AMP_TransactionHistory.csv"
   :bank-anz-coy "_ANZ_coy.csv"
   :bank-anz-visa "_ANZ_credit_card.csv"})

;; year is year ending i.e. 30th June of the second calendar year in the financial year
(def years #{2017})
(def data-root "/home/chris/state/Google Drive/data/ATO/")

(defn -dir-for [{:keys [year quarter] :as period}]
  (assert (quarters quarter))
  (assert (years year))
  (let [tax-year (str "Tax Year " year)
        qtr (quarter->str quarter)]
    (str data-root tax-year "/" qtr)))

(defn bank-period->file-name [bank {:keys [year quarter] :as period}]
  (let [qtr (quarter->str quarter)
        file-name (str qtr (file-names bank))
        file-path (str (-dir-for period) "/" file-name)]
    file-path))

(defn x-1 []
  (bank-period->file-name :bank-amp {:year 2017 :quarter :q3}))
