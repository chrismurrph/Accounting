(ns accounting.data.meta.croquet
  (:require [accounting.data.croquet :as data]
            [accounting.time :as t]))

(def bank-accounts [:bank/bendigo])

(def file-names
  {:bank/bendigo "BBL.CSV"})

(def splits data/splits)

(def ledgers {:cash-deposits {:recalc-date (t/short-date-str->date "30/01/2017") :records data/receive-cash}
              :expenses-owed data/expenses-owed})

(def years #{2017})
(def data-root "/home/chris/Downloads/")
