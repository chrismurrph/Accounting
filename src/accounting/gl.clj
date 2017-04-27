(ns accounting.gl
  (:require [accounting.time :as t]
            [accounting.util :as u]))

(defn account->amount [records]
  (->> records
       (group-by :type)
       (map (fn [[k v]]
              [k (reduce + (map :amount v))]))
       ;(into {})
       ))

(defn iteree [{:keys [unprocessed accumulated-amount result]}]
  (let [[{:keys [amount when] :as h} & tail] unprocessed
        next-result (conj result h)
        ]
    {:unprocessed          tail
     :creeping-recalc-date when
     :accumulated-amount   (+ accumulated-amount amount)
     :result               next-result}))

;;
;; Returns the records between the period up to making the amount
;; Need a short-circuiting reduce i.e. iterate
;;
(defn get-within [date-kw begin end amount records]
  (let [after-begin? (t/after-begin-bound? begin)
        before-end? (t/before-end-bound? end)
        start-from-records (->> records
                                (sort-by date-kw)
                                (drop-while #(-> % date-kw after-begin? not)))]
    (u/pp (map t/show-record start-from-records))
    (println "amount" amount)
    (->> (iterate iteree {:unprocessed          start-from-records
                          :accumulated-amount   0M
                          :creeping-recalc-date nil
                          :result               []})
         (take-while (fn [{:keys [accumulated-amount creeping-recalc-date]}]
                       (and (<= accumulated-amount amount)
                            (or (nil? creeping-recalc-date) (before-end? creeping-recalc-date)))
                       ))
         (u/probe-on "taken")
         last
         (u/probe-on "last"))))

(defn ledger-modify-data [context {:keys [gl ledgers] :as data} {:keys [out/date out/src-bank out/dest-account out/amount]}]
  (assert (map? gl))
  (assert (map? ledgers))
  (let [{:keys [records recalc-date]} ((-> dest-account name keyword) ledgers)
        _ (assert recalc-date (str "No recalc-date for " dest-account))
        [begin end] [(t/add-day recalc-date) date]
        _ (assert (t/gte? end begin) (str "recalc-date needs to be set before " (t/show begin) ", " (t/show end) " for " dest-account))
        ;; Later we will not always get all amounts, but sometimes stop short when :out/amount has been reached
        ;; This will mean an earlier recalc-date for next time. For this code get-up-to that takes a
        {:keys [accumulated-amount creeping-recalc-date result]} (get-within :when begin end amount records)
        _ (assert (= accumulated-amount amount) (str "Total gathered is " accumulated-amount
                                                     ", whereas we were expecting " amount " from "
                                                     (t/show begin) ", " (t/show end) ", " (count records)))
        totals-by-account (account->amount result)
        _ (println totals-by-account)
        new-gl (reduce (fn [acc [account amount]]
                         (assert (account acc) (str "Not in general ledger: " account))
                         (-> acc
                             (update src-bank #(+' % amount))
                             (update account #(-' % amount)))) gl totals-by-account)
        new-ledgers ledgers
        ]
    {:gl new-gl :ledgers new-ledgers}
    ;(println amount)
    ;(println accumulated-amount)
    ;(u/pp totals-by-account)
    ))

;;
;; If the ns is income we increase :src-bank and decrease :dest-account
;; +ive for asset is Debit
;; So +ive is Debit and -ive is Credit
;; Decreasing income account is Credit of :income/poker-parse-sales
;;
(defn modify-data [context {:keys [gl ledgers] :as data} {:keys [out/src-bank out/dest-account out/amount]}]
  (assert src-bank)
  (assert dest-account)
  (number? amount)
  (assert (src-bank gl) (str "Not in general ledger: " src-bank))
  (assert (dest-account gl) (str "Not in general ledger: " dest-account))
  {:gl      (-> gl
                (update src-bank #(+' % amount))
                (update dest-account #(-' % amount)))
   :ledgers ledgers})

(defn split-modify-data [{:keys [splits] :as context} {:keys [gl ledgers] :as data} {:keys [out/src-bank out/dest-account out/amount]}]
  (let [split-ups (-> dest-account name keyword splits vec)]
    {:gl (reduce
           (fn [gl [dest-account proportion]]
             (let [prop-amt' (*' proportion amount)
                   ;; with-precision doesn't do decimal places
                   prop-amt (bigdec (format "%.2f" prop-amt'))]
               ;(println "got" prop-amt " from " amount " and " proportion)
               (modify-data context gl {:out/src-bank     src-bank
                                        :out/dest-account dest-account
                                        :out/amount       prop-amt})))
           gl
           split-ups
           )
     :ledgers ledgers}))

(def dont-modify-gl (fn [x _] x))

(def how-apply-namespace
  {"income"   modify-data
   "exp"      modify-data
   "non-exp"  modify-data
   "capital"  modify-data
   "personal" modify-data
   "split"    split-modify-data
   "ledger"   ledger-modify-data})

;;
;; Modifies data (which includes gl), used by reduce
;;
(defn apply-trans [context data {:keys [out/src-bank out/dest-account out/amount] :as trans}]
  (assert src-bank)
  (assert dest-account)
  (assert amount)
  (let [ns (namespace dest-account)
        _ (u/assrt ns (str "No namespace for: <" dest-account ">:\n" (-> trans t/show-record u/pp-str)))
        f (how-apply-namespace ns)]
    (assert f (str "Not found a function for namespace: <" ns ">, with dest-account: <" dest-account ">:\n" (-> trans t/show-record u/pp-str)))
    (f context data trans)))
