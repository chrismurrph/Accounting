(ns accounting.gl
  (:require [accounting.time :as t]
            [accounting.util :as u]
            [accounting.seaweed-rules-data :as rd]))

;; Bank amounts are as at 30/06/2016, so s/be able to run quarters 1, 2 and 3 and get
;; balances as at 31/03/2017, which are:
;; :bank/anz-coy    2138.16
;; :bank/anz-visa    250.74
;; :bank/amp         431.76
;; They were indeed these amounts!
(def general-ledger
  {:bank/anz-coy              96.15M
   :bank/anz-visa             -1024.48M
   :bank/amp                  3010.59M
   :personal/amp              0M
   :personal/anz-visa         0M
   :income/mining-sales       0M
   :income/poker-parse-sales  0M
   :income/bank-interest      0M
   :capital/drawings          0M
   :exp/office-expense        0M
   :exp/motor-vehicle         0M
   :exp/cloud-expense         0M
   :exp/niim-trip             0M
   :exp/accounting-software   0M
   :exp/mobile-expense        0M
   :exp/bank-fee              0M
   :exp/interest-expense      0M
   :exp/petrol                0M
   :exp/computer-expense      0M
   :exp/office-rent           0M
   :exp/travel                0M
   :exp/donations             0M
   :exp/isp                   0M
   :exp/storage               0M
   :exp/light-power-heating   0M
   :exp/accomodation          0M
   :exp/food                  0M
   :exp/advertising           0M
   :exp/meeting-entertainmant 0M
   :exp/asic-payment          0M
   :exp/freight-courier       0M
   :exp/accounting-expense    0M
   :non-exp/ato-payment       0M
   :non-exp/private-health    0M
   })

;;
;; If the ns is income we increase :src-bank and decrease :dest-account
;; +ive for asset is Debit
;; So +ive is Debit and -ive is Credit
;; Decreasing income account is Credit of :income/poker-parse-sales
;;
(defn modify-gl [gl {:keys [out/src-bank out/dest-account out/amount]}]
  (assert src-bank)
  (assert dest-account)
  (number? amount)
  (assert (src-bank gl) (str "Not in general ledger: " src-bank))
  (assert (dest-account gl) (str "Not in general ledger: " dest-account))
  (-> gl
      (update src-bank #(+' % amount))
      (update dest-account #(-' % amount))))

(defn split-modify-gl [gl {:keys [out/src-bank out/dest-account out/amount]}]
  (let [splits (-> dest-account name keyword rd/splits vec)]
    (reduce
      (fn [gl [dest-account proportion]]
        (let [prop-amt' (*' proportion amount)
              ;; with-precision doesn't do decimal places
              prop-amt (bigdec (format "%.2f" prop-amt'))]
          ;(println "got" prop-amt " from " amount " and " proportion)
          (modify-gl gl {:out/src-bank     src-bank
                         :out/dest-account dest-account
                         :out/amount       prop-amt})))
      gl
      splits
      )))

(def dont-modify-gl (fn [x _] x))

(def how-apply-namespace
  {"income"   modify-gl
   "exp"      modify-gl
   "non-exp"  modify-gl
   "capital"  modify-gl
   "personal" modify-gl
   "split"    split-modify-gl})

;;
;; Modifies gl, used by reduce
;;
(defn apply-trans [gl {:keys [out/src-bank out/dest-account out/amount] :as trans}]
  (assert src-bank)
  (assert dest-account)
  (assert amount)
  (let [ns (namespace dest-account)
        _ (u/assrt ns (str "No namespace for: <" dest-account ">:\n" (-> trans t/show-record u/pp-str)))
        f (how-apply-namespace ns)]
    (assert f (str "Not found a function for namespace: <" ns ">, with dest-account: <" dest-account ">:\n" (-> trans t/show-record u/pp-str)))
    (f gl trans)))
