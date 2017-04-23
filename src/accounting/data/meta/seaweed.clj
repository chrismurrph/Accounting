(ns accounting.data.meta.seaweed
  (:require [accounting.data.seaweed :as data]
            [clojure.string :as s]
            [clojure.set :as set]
            [accounting.util :as u]))

(def bank-accounts [:bank/amp :bank/anz-coy :bank/anz-visa])
(def other-asset-accounts #{})
(def income-accounts #{:income/bank-interest :income/mining-sales})
(def tax-expense-accounts #{})
(def non-tax-expense-accounts #{:non-expense/ato-payment})
(def negative-equity-account #{:capital/drawings})

(def splits data/splits)

(def all-accounts (set/union (set bank-accounts)
                             other-asset-accounts
                             income-accounts
                             tax-expense-accounts
                             non-tax-expense-accounts
                             negative-equity-account))
;(println all-accounts)

;; Always start with quarter even thou in that directory
;; "Q3_AMP_TransactionHistory.csv"
(def file-names
  {:bank/amp      "_AMP_TransactionHistory.csv"
   :bank/anz-coy  "_ANZ_coy.csv"
   :bank/anz-visa "_ANZ_credit_card.csv"})

;; tax-year is year ending i.e. 30th June of the second calendar year in the financial year
(def tax-years #{2017})
(def data-root "/home/chris/state/Google Drive/data/ATO/")
