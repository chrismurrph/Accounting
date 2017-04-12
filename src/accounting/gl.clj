(ns accounting.gl
  (:require [clj-time.core :as t]
            [accounting.util :as u]))

(def general-ledger
  {:bank/anz-coy             0
   :bank/anz-visa            0
   :bank/amp                 0
   :income/poker-parse-sales 0
   :exp/office-expense       0
   :capital/drawings         0
   :exp/motor-vehicle        0
   :exp/cloud-expense        0
   :exp/niim-trip            0
   :exp/accounting-software  0
   :income/mining-sales      0
   :exp/mobile-expense       0
   :exp/bank-fee             0
   :exp/interest-expense     0
   :non-exp/ato-payment      0
   :non-exp/private-health   0
   :exp/petrol               0
   :exp/computer-expense     0
   :income/bank-interest     0})

(def example-transaction-income
  #:out{:date         (u/long-date-str->date "31 Mar 2017"),
        :amount       206.90M,
        :desc         "TRANSFER FROM R T WILSON       FROM 03645081",
        :src-bank     :bank/anz-coy,
        :dest-account :income/poker-parse-sales})

(def example-transaction-exp
  #:out{:date         (u/long-date-str->date "30 Mar 2017"),
        :amount       -15.91M,
        :desc         "OFFICEWORKS 0502          TRINITY GDNS",
        :src-bank     :bank/anz-visa,
        :dest-account :exp/office-expense})

(def example-transaction-capital
  #:out{:date         (u/long-date-str->date "21 Mar 2017"),
        :amount       -400.00M,
        :desc         "ANZ INTERNET BANKING FUNDS TFER TRANSFER 546251 TO      CHRISTOPHER MURP",
        :src-bank     :bank/anz-coy,
        :dest-account :capital/drawings})

;;
;; If the ns is income we increase :src-bank and decrease :dest-account
;; +ive for asset is Debit
;; So +ive is Debit and -ive is Credit
;; Decreasing income account is Credit of :income/poker-parse-sales
;;
(defn modify-gl [gl src-bank dest-account amount]
  (assert src-bank)
  (assert dest-account)
  (number? amount)
  (assert (src-bank gl) (str "Not in general ledger: " src-bank))
  (assert (dest-account gl) (str "Not in general ledger: " dest-account))
  (-> gl
      (update src-bank #(+ % amount))
      (update dest-account #(- % amount))))

(def how-apply
  {"income"  modify-gl
   "exp"     modify-gl
   "non-exp" modify-gl
   "capital" modify-gl})

;;
;; Modifies gl, used by reduce
;;
(defn apply-trans [gl trans]
  (let [{:keys [out/amount out/src-bank out/dest-account]} trans
        _ (assert amount)
        _ (assert src-bank)
        _ (assert dest-account)
        _ (u/assrt (not= dest-account :investigate-further))
        ns (namespace dest-account)
        _ (u/assrt ns (str "No namespace for: <" dest-account ">:\n" (u/pp-str trans)))
        f (how-apply ns)]
    (assert f (str "Not found a function for " ns))
    (f gl src-bank dest-account amount)))

(defn x-1 []
  (apply-trans general-ledger example-transaction-capital))
