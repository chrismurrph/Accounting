(ns accounting.data.meta.croquet
  (:require [accounting.data.croquet :as data]
            [accounting.time :as t]))

(def bank-accounts [:bank/bendigo])

(def file-names
  {:bank/bendigo "BBL.CSV"})

(def all-accounts (set bank-accounts))

(def splits data/splits)

(def years #{2017})
(def data-root "/home/chris/Downloads/")
