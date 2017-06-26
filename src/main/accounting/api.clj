(ns accounting.api
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.seasoft-context :as seasoft-con]
            [accounting.data.seaweed :as seaweed-data]
            [accounting.data.croquet :as croquet-data]
            [accounting.croquet-context :as croquet-con]
            [accounting.util :as u]
            [accounting.data.meta.periods :as periods]))

(defn make-ledger-item [idx [kw amount]]
  (assert (keyword? kw) (str "Expect a keyword but got: " kw ", has type: " (type kw)))
  {:db/id idx :ledger-item/name (name kw) :ledger-item/type ((comp keyword namespace) kw) :ledger-item/amount amount})

(defn ->ledger-items [m]
  (->> m
       (partition 2)
       (mapcat identity)
       (map-indexed make-ledger-item)
       vec))

(def seasoft-splits (-> meta/human-meta :seaweed :splits))
(def croquet-splits (-> meta/human-meta :croquet :splits))
(def by-org {:seaweed {:splits               seasoft-splits
                       :rules-fn             seasoft-con/rules-of-period
                       :bank-statements-fn   seasoft-con/bank-statements-of-period
                       :starting-balances-fn (partial seaweed-data/starting-gl periods/make-quarter)}
             :croquet {:splits               croquet-splits
                       :rules-fn             croquet-con/rules-of-period
                       :bank-statements-fn   croquet-con/bank-statements-of-period
                       :starting-balances-fn (partial croquet-data/starting-gl periods/make-month)}})

(defn trial-balance-report [organisation year period]
  (let [{:keys [splits rules-fn bank-statements-fn starting-balances-fn] :as for-org} (organisation by-org)
        year (u/kw->number year)
        bank-statements (bank-statements-fn year period)
        current-rules (rules-fn year period)
        starting-balances (starting-balances-fn year period)]
    (-> (c/trial-balance bank-statements current-rules splits starting-balances)
        ->ledger-items)))

(def rep->fn
  {:report/profit-and-loss (fn [_ _ _] [(make-ledger-item 0 [:dummy-entry 1000])])
   :report/balance-sheet   (fn [_ _ _] [(make-ledger-item 1 [:dummy-entry 1001])])
   :report/big-items-first (fn [_ _ _] [(make-ledger-item 2 [:dummy-entry 1002])])
   :report/trial-balance   trial-balance-report})

(defn fetch-report [query organisation year period report]
  (assert (= 4 (count query)))
  ((report rep->fn) organisation year period))
