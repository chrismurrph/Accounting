(ns accounting.api
  (:require [accounting.core :as c]
            [accounting.data.meta.common :as meta]
            [accounting.seasoft-context :as seasoft-con]
            [accounting.data.seaweed :as seaweed-data]
            [accounting.data.croquet :as croquet-data]
            [accounting.croquet-context :as croquet-con]
            [accounting.util :as u]
            [accounting.data.meta.periods :as periods]
            [accounting.data.meta.common :as common-meta]
            [accounting.match :as m]
            [accounting.data.common :as dc]
            [accounting.data.meta.seaweed :as seasoft-meta]
            [accounting.data.meta.croquet :as croquet-meta]
            [accounting.time :as t]
            [cljc.utils :as us]))

(defn make-ledger-item [idx [kw amount]]
  (assert (keyword? kw) (str "Expect a keyword but got: " kw ", has type: " (type kw)))
  {:db/id idx
   :ledger-item/name (name kw)
   :ledger-item/type ((comp keyword namespace) kw)
   :ledger-item/amount amount})

(defn map->ledger-items [m]
  (assert (map? m) (str "Expected a map but got: " (type m)))
  (->> m
       (partition 2)
       (mapcat identity)
       (map-indexed make-ledger-item)
       vec))

(defn coll->ledger-items [xs]
  (assert (coll? xs) (str "Expected a coll but got: " (type xs)))
  (->> xs
       (map-indexed make-ledger-item)
       vec))

#_(defn bank-statements-of-period [all-periods bank-accounts make-period organisation]
  (assert (set? all-periods))
  (assert (set? bank-accounts))
  (assert (keyword? organisation))
  (fn [year period]
    (assert (number? year))
    (assert (keyword? period))
    (assert (period all-periods) all-periods)
    (let [period (make-period year period)
          bank-records (c/import-bank-records! organisation [period] bank-accounts)]
      {:bank-records bank-records :bank-accounts bank-accounts})))

(defn rules-of-period [make-period permanent-rules period->rules]
  (fn [year quarter]
    (assert (number? year))
    (assert (keyword? quarter))
    (let [period (make-period year quarter)
          initial-rules (merge-with (comp vec concat) permanent-rules (apply concat (map period->rules [period])))]
      (->> initial-rules
           dc/canonicalise-rules
           u/probe-off))))

(def seaweed-rules-of-period (rules-of-period periods/make-quarter seaweed-data/permanent-rules seasoft-con/quarter->rules))
(def croquet-rules-of-period (rules-of-period periods/make-month croquet-data/permanent-rules croquet-con/month->rules))

#_(def seaweed-bank-statements (bank-statements-of-period
                               periods/quarters-set
                               (set seasoft-con/seasoft-bank-accounts)
                               periods/make-quarter
                               :seaweed))

#_(def croquet-bank-statements (bank-statements-of-period
                               periods/months-set
                               (set croquet-con/croquet-bank-accounts)
                               periods/make-month
                               :croquet))

(def seasoft-splits (-> meta/human-meta :seaweed :splits))
(def croquet-splits (-> meta/human-meta :croquet :splits))
(def by-org {:seaweed {:splits               seasoft-splits
                       :rules-fn             seaweed-rules-of-period
                       ;:bank-statements-fn   seaweed-bank-statements
                       :starting-balances-fn (partial seaweed-data/starting-gl periods/make-quarter)}
             :croquet {:splits               croquet-splits
                       :rules-fn             croquet-rules-of-period
                       ;:bank-statements-fn   croquet-bank-statements
                       :starting-balances-fn (partial croquet-data/starting-gl periods/make-month)}})

(defn trial-balance-report [organisation year period]
  (let [{:keys [splits rules-fn bank-statements-fn starting-balances-fn] :as for-org} (organisation by-org)
        year (us/kw->number year)
        bank-statements (bank-statements-fn year period)
        current-rules (rules-fn year period)
        starting-balances (starting-balances-fn year period)]
    (-> (c/trial-balance bank-statements current-rules splits starting-balances)
        map->ledger-items)))

(defn biggest-items-report [organisation year period]
  (let [{:keys [rules-fn bank-statements-fn] :as for-org} (organisation by-org)
        year (us/kw->number year)
        bank-statements (bank-statements-fn year period)
        current-rules (rules-fn year period)]
    (->> (c/account-grouped-transactions bank-statements current-rules)
         c/accounts-summary
         (sort-by (comp - u/abs second))
         coll->ledger-items)))

(def rep->fn
  {:report/profit-and-loss (fn [_ _ _] [(make-ledger-item 0 [:dummy-entry 1000])])
   :report/balance-sheet   (fn [_ _ _] [(make-ledger-item 1 [:dummy-entry 1001])])
   :report/big-items-first biggest-items-report
   :report/trial-balance   trial-balance-report})

(defn fetch-report [query organisation year period report]
  (assert (= 4 (count query)))
  ((report rep->fn) organisation year period))

(defn get-by-limit-kw [org]
  (assert (keyword? org))
  (let [organisation org
        total-range ({:seaweed seasoft-con/total-range
                      :croquet croquet-con/total-range}
                      organisation)]
    (fn [kw]
      (case kw
        :db/id 'POTENTIAL-DATA
        :potential-data/period-type (-> common-meta/human-meta organisation :period-type)
        :potential-data/commencing-period (first total-range)
        :potential-data/latest-period (last total-range)
        :potential-data/possible-reports [:report/trial-balance :report/big-items-first :report/profit-and-loss :report/balance-sheet]))))

(defn potential-data [kws organisation]
  (let [f (get-by-limit-kw organisation)]
    (->> kws
         (mapv f)
         (zipmap kws))))

(defn config-data [kws organisation]
  (assert organisation)
  {:db/id                       'CONFIG-DATA
   :config-data/ledger-accounts ({:seaweed seasoft-meta/all-accounts
                                  :croquet croquet-meta/all-accounts}
                                  organisation)
   :config-data/bank-accounts   ({:seaweed seasoft-con/seasoft-bank-accounts
                                  :croquet croquet-con/croquet-bank-accounts}
                                  organisation)})

;;
;; The id doesn't seem to matter, nor for potential data above
;;
(defn next-unruly-line [kws]
  (merge {:db/id 'BANK-STATEMENT-LINE
          ;:bank-line/src-bank :bank/anz-visa
          ;:bank-line/date     "24/08/2016"
          ;:bank-line/desc     "OFFICEWORKS SUPERSTO      KESWICK"
          ;:bank-line/amount   71.01M
          }
         (->> (c/records-without-single-rule-match nil #_seasoft-con/seasoft-bank-statements @seasoft-con/current-rules)
              ffirst
              (map (fn [[k v]]
                     [({:out/date     :bank-line/date
                        :out/amount   :bank-line/amount
                        :out/desc     :bank-line/desc
                        :out/src-bank :bank-line/src-bank}
                        k) v]))
              (into {})
              u/probe-off)))

(defn rule->outgoing [idx {:keys [logic-operator conditions rule/source-bank
                                  rule/permanent? rule/period
                                  rule/target-account time-slot
                                  on-dates]}]
  {:db/id                        idx
   :rule/actual-period           period
   :rule/permanent?              permanent?
   :rule/source-bank             source-bank
   :rule/target-account          target-account
   :rule/time-slot               time-slot
   :rule/on-dates                on-dates
   :rule/conditions              conditions
   :rule/logic-operator          logic-operator})

(defn rules-from-bank-ledger [_ kws source-bank target-ledger]
  (assert (keyword? source-bank) (us/assert-str "source-bank" source-bank))
  (assert (keyword? target-ledger) (us/assert-str "target-ledger" target-ledger))
  ;(println "Choosing on " source-bank " and " target-ledger)
  (let [to-send (m/filter-rules-old #{source-bank} #{target-ledger} (mapv t/civilize-joda @seasoft-con/current-rules))]
    (->> to-send
         (map-indexed rule->outgoing)
         vec
         u/probe-off)))

