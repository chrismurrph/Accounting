(ns accounting.test-gl
  (:require [accounting.gl :as g]
            [accounting.time :as t]
            [accounting.data.seaweed :as d]))

(def example-transaction-income
  #:out{:date         (t/long-date-str->date "31 Mar 2017"),
        :amount       206.90M,
        :desc         "TRANSFER FROM R T WILSON       FROM 03645081",
        :src-bank     :bank/anz-coy,
        :dest-account :income/poker-parse-sales})

(def example-transaction-exp
  #:out{:date         (t/long-date-str->date "30 Mar 2017"),
        :amount       -15.91M,
        :desc         "OFFICEWORKS 0502          TRINITY GDNS",
        :src-bank     :bank/anz-visa,
        :dest-account :exp/office-expense})

(def example-transaction-capital
  #:out{:date         (t/long-date-str->date "21 Mar 2017"),
        :amount       -400.00M,
        :desc         "ANZ INTERNET BANKING FUNDS TFER TRANSFER 546251 TO      CHRISTOPHER MURP",
        :src-bank     :bank/anz-coy,
        :dest-account :capital/drawings})


(defn x-1 []
  (g/apply-trans {} d/general-ledger example-transaction-capital))
