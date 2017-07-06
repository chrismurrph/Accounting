(ns accounting.data.meta.seaweed
  (:require [accounting.data.seaweed :as data]
            [clojure.string :as s]
            [clojure.set :as set]
            [accounting.util :as u]))

;;
;; Intention is to make all-accounts accurate when needed. Not yet used...
;;
(def bank-accounts [:bank/amp :bank/anz-coy :bank/anz-visa])
(def other-asset-accounts #{})
(def income-accounts #{:income/bank-interest :income/mining-sales :income/poker-parse-sales})
(def tax-expense-accounts #{:exp/office-expense
                            :exp/motor-vehicle
                            :exp/cloud-expense
                            :exp/niim-trip
                            :exp/accounting-software
                            :exp/mobile-expense
                            :exp/bank-fee
                            :exp/bank-interest
                            :exp/petrol
                            :exp/computer-expense
                            :exp/national-travel
                            :exp/donations
                            :exp/isp
                            :exp/storage
                            :exp/light-power-heating
                            :exp/rent
                            :exp/food
                            :exp/advertising
                            :exp/meeting-entertainment
                            :exp/asic-payment
                            :exp/freight-courier
                            :exp/accounting-expense})
(def non-tax-expense-accounts #{:non-exp/ato-payment :non-exp/private-health})
(def liab-accounts #{:liab/drawings})
(def personal-accounts #{:personal/amp
                         :personal/anz-visa})

(def ledger-accounts (set/union
                             other-asset-accounts
                             income-accounts
                             tax-expense-accounts
                             non-tax-expense-accounts
                             liab-accounts
                             personal-accounts))

(def all-accounts (set/union (set bank-accounts) ledger-accounts))

(def splits data/splits)

;; Always start with quarter even thou in that directory
;; "Q3_AMP_TransactionHistory.csv"
(def import-templates
  {:bank/amp      "_AMP_TransactionHistory.csv"
   :bank/anz-coy  "_ANZ_coy.csv"
   :bank/anz-visa "_ANZ_credit_card.csv"})

;; tax-year is year ending i.e. 30th June of the second calendar year in the financial year
(def tax-years #{2017})
(def import-data-root "/home/chris/state/Google Drive/data/ATO/")
