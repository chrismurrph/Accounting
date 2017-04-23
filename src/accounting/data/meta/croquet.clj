(ns accounting.data.meta.croquet
  (:require [accounting.data.croquet :as data]))

(def bank-accounts [:bank/bendigo])

(def file-names
  {:bank/bendigo "BBL.CSV"})

(def splits data/splits)

(def ledgers {:cash-deposts data/receive-cash
              :expenses-owed data/expenses-owed})

(def years #{2017})
(def data-root "/home/chris/Downloads/")
