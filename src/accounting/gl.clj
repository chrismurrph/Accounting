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
  (assert accumulated-amount)
  (let [[{:keys [amount when] :as h} & tail] unprocessed]
    (if h
      (let [_ (assert amount (str "No amount in <" h ">"))
            next-result (conj result h)]
        {:unprocessed          tail
         :creeping-recalc-date when
         :accumulated-amount   (+ accumulated-amount amount)
         :result               next-result})
      (do
        (assert false (str "Processed all and only got: " accumulated-amount "\n" (u/pp-str (mapv t/show-ledger-record result))))
        #_{:unprocessed          []
         :creeping-recalc-date when
         :accumulated-amount   accumulated-amount
         :result               result}
        ))))

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
    (assert (seq start-from-records) (str "No records from " (count records) " " (-> records first t/show-trans-record)))
    (u/pp (map t/show-ledger-record start-from-records))
    ;(println "amount" amount)
    (->> (iterate iteree {:unprocessed          start-from-records
                          :accumulated-amount   0M
                          :creeping-recalc-date nil
                          :result               []})
         (take-while (fn [{:keys [accumulated-amount creeping-recalc-date unprocessed]}]
                       (and (seq unprocessed)
                            (<= accumulated-amount amount)
                            (or (nil? creeping-recalc-date) (before-end? creeping-recalc-date)))
                       ))
         last
         )))

(defn show-ledger-records [records]
  (mapv #(-> % t/show-ledger-record) records))

(defn ledger-modify-data [context {:keys [gl ledgers] :as data} {:keys [out/date out/src-bank out/dest-account out/amount]}]
  (assert (map? gl))
  (assert (map? ledgers))
  (let [ledger-kw (-> dest-account name keyword)
        {:keys [records recalc-date]} (ledger-kw ledgers)
        _ (assert recalc-date (str "No recalc-date for " dest-account))
        [begin end] [(t/add-day recalc-date) date]]
    (if (t/gte? end begin)
      (let [{:keys [accumulated-amount creeping-recalc-date result]} (get-within :when begin end amount records)
            ;; The ledger just doesn't have the entries
            _ (assert (= accumulated-amount amount) (str "Total gathered is " accumulated-amount
                                                         ", whereas we were expecting " amount " for " ledger-kw " from "
                                                         (t/show begin) " to " (t/show end) ", in count:\n" (show-ledger-records records)
                                                         "\ngathered:\n" (u/pp-str (map t/show-ledger-record result))))
            totals-by-account (account->amount result)
            _ (println totals-by-account)
            new-gl (reduce (fn [acc [account amount]]
                             (u/assrt (account acc) (str "Not in general ledger: " account))
                             (-> acc
                                 (update src-bank #(+' % amount))
                                 (update account #(-' % amount)))) gl totals-by-account)
            new-ledgers (assoc-in ledgers [ledger-kw :recalc-date] creeping-recalc-date)]
        (println "old,new:" (-> ledgers ledger-kw :recalc-date t/show) (-> new-ledgers ledger-kw :recalc-date t/show) "for" ledger-kw)
        {:gl new-gl :ledgers new-ledgers})
      (do
        (u/warning (str "There is a " dest-account " on " (t/show end)
                        ", which we won't have ledger for, as this ledger starts at " (t/show begin)
                        ", ledger record/s:\n" (show-ledger-records records)))
        {:gl gl :ledgers ledgers}))
    ))

;;
;; If the ns is income we increase :src-bank and decrease :dest-account
;; +ive for asset is Debit
;; So +ive is Debit and -ive is Credit
;; Decreasing income account is Credit of :income/poker-parse-sales
;;
(defn modify-data [context {:keys [gl ledgers] :as data} {:keys [out/src-bank out/dest-account out/amount]}]
  (assert gl (str "No gl in: " data))
  (assert ledgers)
  (assert src-bank)
  (assert dest-account)
  (number? amount)
  (u/assrt (src-bank gl) (str "Not in general ledger: " src-bank))
  (u/assrt (dest-account gl) (str "Not in general ledger: " dest-account))
  {:gl      (-> gl
                (update src-bank #(+' % amount))
                (update dest-account #(-' % amount)))
   :ledgers ledgers})

(defn split-modify-data [{:keys [splits] :as context} {:keys [gl ledgers] :as data} {:keys [out/src-bank out/dest-account out/amount]}]
  (assert splits)
  (let [split-ups (-> dest-account name keyword splits vec)]
    {:gl      (reduce
                (fn [gl [dest-account proportion]]
                  (let [prop-amt' (*' proportion amount)
                        ;; with-precision doesn't do decimal places
                        prop-amt (bigdec (format "%.2f" prop-amt'))]
                    ;(println "got" prop-amt " from " amount " and " proportion)
                    (modify-data context data {:out/src-bank     src-bank
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
        _ (u/assrt ns (str "No namespace for: <" dest-account ">:\n" (-> trans t/show-trans-record u/pp-str)))
        f (how-apply-namespace ns)]
    (assert f (str "Not found a function for namespace: <" ns ">, with dest-account: <" dest-account ">:\n" (-> trans t/show-trans-record u/pp-str)))
    (f context data trans)))
