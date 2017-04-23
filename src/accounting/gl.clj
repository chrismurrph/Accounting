(ns accounting.gl
  (:require [accounting.time :as t]
            [accounting.util :as u]))

;;
;; If the ns is income we increase :src-bank and decrease :dest-account
;; +ive for asset is Debit
;; So +ive is Debit and -ive is Credit
;; Decreasing income account is Credit of :income/poker-parse-sales
;;
(defn modify-gl [context gl {:keys [out/src-bank out/dest-account out/amount]}]
  (assert src-bank)
  (assert dest-account)
  (number? amount)
  (assert (src-bank gl) (str "Not in general ledger: " src-bank))
  (assert (dest-account gl) (str "Not in general ledger: " dest-account))
  (-> gl
      (update src-bank #(+' % amount))
      (update dest-account #(-' % amount))))

(defn split-modify-gl [{:keys [splits] :as context} gl {:keys [out/src-bank out/dest-account out/amount]}]
  (let [split-ups (-> dest-account name keyword splits vec)]
    (reduce
      (fn [gl [dest-account proportion]]
        (let [prop-amt' (*' proportion amount)
              ;; with-precision doesn't do decimal places
              prop-amt (bigdec (format "%.2f" prop-amt'))]
          ;(println "got" prop-amt " from " amount " and " proportion)
          (modify-gl context gl {:out/src-bank     src-bank
                                 :out/dest-account dest-account
                                 :out/amount       prop-amt})))
      gl
      split-ups
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
(defn apply-trans [context gl {:keys [out/src-bank out/dest-account out/amount] :as trans}]
  (assert src-bank)
  (assert dest-account)
  (assert amount)
  (let [ns (namespace dest-account)
        _ (u/assrt ns (str "No namespace for: <" dest-account ">:\n" (-> trans t/show-record u/pp-str)))
        f (how-apply-namespace ns)]
    (assert f (str "Not found a function for namespace: <" ns ">, with dest-account: <" dest-account ">:\n" (-> trans t/show-record u/pp-str)))
    (f context gl trans)))
