(ns accounting.datomic-api
  (:require [datomic.api :as d]
            [accounting.util :as u]
            [accounting.core :as c]
            [cljc.domain-helpers :as dhs]
            [cljc.utils :as us]
            [accounting.entities :as e]
            [accounting.queries :as q]
            [accounting.play :as p]
            [accounting.time :as t]
            [accounting.match :as m]))

#_(defn make-district [connection list-name]
    (let [id (d/tempid :db.part/user)
          tx [{:db/id id :district/name list-name}]
          idmap (:tempids @(d/transact connection tx))
          real-id (d/resolve-tempid (d/db connection) idmap id)]
      real-id))

(defn organisation-data [conn query org-key]
  (assert conn)
  (assert (= org-key :seaweed))
  (println "QUERY: " query)
  (let [org-eid (q/read-organisation conn org-key)
        db (d/db conn)]
    (d/pull db query org-eid)))

;;
;; When this is called these are definitely statements that have
;; not been imported before.
;;
(defn update-bank-account-with-statements [conn account-key statements]
  (let [db (d/db conn)
        org-entity (->> account-key
                        (q/read-organisation conn)
                        (d/entity db)
                        (d/entity-db))]
    (assoc org-entity :bank-account/statements statements)))

#_(defn trial-balance-report [conn organisation year period]
    (let [
          ;{:keys [splits rules-fn bank-statements-fn starting-balances-fn] :as for-org} (organisation by-org)
          year (u/kw->number year)
          bank-statements (bank-statements-fn year period)
          current-rules (rules-fn year period)
          starting-balances (starting-balances-fn year period)]
      (-> (c/trial-balance bank-statements current-rules splits starting-balances)
          map->ledger-items)))

(defn objectify-account [acct]
  acct)

(defn rules-from-bank-ledger [conn org-key source-bank target-ledger]
  (assert (keyword? source-bank) (us/assert-str "source-bank" source-bank))
  (assert (keyword? target-ledger) (us/assert-str "target-ledger" target-ledger))
  ;(println source-bank target-ledger)
  (let [{:keys [time-slot actual-period]} (q/find-current-period conn org-key)
        {:keys [actual-period/year actual-period/quarter]} actual-period
        bank (e/make-account-key source-bank)
        ledger (e/make-account-key target-ledger)]
    (->> (q/read-period-specific-rules false conn org-key year quarter)
         (m/filter-rules-new #{bank} #{ledger})
         (map #(assoc % :rule/button-group {:debug-from "server"}))
         ;(drop 3)
         ;(take 10)
         vec
         u/probe-off)))

(defn ->upload-key [k]
  (case k
    :out/date :bank-line/date
    :out/amount :bank-line/amount
    :out/desc :bank-line/desc
    :out/src-bank :bank-line/src-bank
    (assert false (str "Unknown key: " k))))

;;
;; The id is removed as the statement line is never updated
;;
(defn next-unruly-line [conn org-key]
  (let [{:keys [time-slot actual-period]} (q/find-current-period conn org-key)
        {:keys [time-slot/start-at time-slot/end-at]} time-slot
        {:keys [actual-period/year actual-period/quarter]} actual-period
        ]
    (merge {:db/id 'BANK-STATEMENT-LINE}
           (->> (c/records-without-single-rule-match
                  {:bank-accounts (q/read-bank-accounts conn org-key start-at end-at)
                   :bank-records  (->> org-key
                                       (q/current-period-line-items conn)
                                       (q/line-items-transform conn))}
                  (q/read-period-specific-rules false conn org-key year quarter))
                ffirst
                (keep (fn [[k v]]
                        (when (not= k :db/id)
                          [(->upload-key k) v])))
                (into {})
                u/probe-off))))

#_(defn biggest-items-report [conn organisation year period]
    (let [
          ;{:keys [rules-fn bank-statements-fn] :as for-org} (organisation by-org)
          year (u/kw->number year)
          bank-statements (bank-statements conn organisation year period)
          current-rules (rules-fn year period)]
      (->> (c/account-grouped-transactions bank-statements current-rules)
           c/accounts-summary
           (sort-by (comp - u/abs second))
           coll->ledger-items)))

(defn make-dummy-line-item [idx [kw amount]]
  (assert idx)
  (assert (keyword? kw) (str "Expect a keyword but got: " kw ", has type: " (type kw)))
  (assert (number? amount) (us/assert-str "amount" amount))
  {:db/id            idx
   :line-item/name   (name kw)
   :line-item/type   ((comp keyword namespace) kw)
   :line-item/amount amount})

(defn make-line-item [idx [{:keys [ledger-account/category ledger-account/name]} amount]]
  (assert (number? amount) (us/assert-str "amount" amount))
  {:db/id            idx
   :line-item/name   name
   :line-item/type   category
   :line-item/amount amount})

(defn coll->ledger-items [xs]
  (assert (coll? xs) (str "Expected a coll but got: " (type xs)))
  (->> xs
       (map-indexed make-line-item)
       vec))

(defn biggest-items-report [conn org-key year period]
  (assert year)
  (assert period)
  ;(println period)
  (let [rep-actual-period #:actual-period{:type :quarterly :year (us/kw->number year) :quarter period}
        bank-accounts (q/read-bank-accounts conn org-key
                                            (t/start-actual-period-moment rep-actual-period)
                                            (t/end-actual-period-moment rep-actual-period))
        ;_ (println bank-accounts)
        rep-bank-accounts (filter #(t/intersects? (:account/time-slot %) rep-actual-period)
                                  bank-accounts)
        _ (assert (seq rep-bank-accounts) (str "No current bank accounts from " bank-accounts " within " rep-actual-period))
        rep-bank-records (q/find-line-items conn org-key (us/kw->number year) period)
        rep-rules (q/read-period-specific-rules true conn org-key (us/kw->number year) period)]
    (->> (c/account-grouped-transactions {:bank-accounts rep-bank-accounts
                                          :bank-records  rep-bank-records} rep-rules)
         c/accounts-summary
         (sort-by (comp - u/abs second))
         coll->ledger-items))
  #_[(make-dummy-line-item 2 [:dummy-entry 1002])])

(def rep->fn
  {:report/profit-and-loss (fn [_ _ _ _] [(make-dummy-line-item 0 [:dummy-entry 1000])])
   :report/balance-sheet   (fn [_ _ _ _] [(make-dummy-line-item 1 [:dummy-entry 1001])])
   :report/big-items-first biggest-items-report
   :report/trial-balance   (fn [_ _ _ _] [(make-dummy-line-item 3 [:dummy-entry 1003])])})

(defn fetch-report [conn query organisation year period report]
  (assert (= 4 (count query)))
  (let [rep-f (report rep->fn)]
    (assert rep-f (str "No report function listed for " report))
    (rep-f conn organisation year period)))

;;
;; The function used (f) didn't think to give us back the year, so this wrapper does
;; the job for us.
;; Notice not yet returning Datomic format. We might need to do that later.
;;
(defn year-taker [f]
  (fn [year]
    (let [periods (f year)]
      (map (fn [period] {:actual-period/year year :actual-period/quarter period :actual-period/type :quarterly}) periods))))

(defn -do-statements-import [conn org-meta importing-meta actual-periods]
  (let [period-type (:organisation/period-type org-meta)]
    (for [{:keys [bank-acct-name template-str]} importing-meta
          :let [headings (q/find-headings conn bank-acct-name)]
          actual-period actual-periods]
      (let [records (->> (c/slurp-raw-data->csv-datomic! org-meta period-type template-str actual-period)
                         ;(take 1)
                         u/probe-off
                         (c/parse-csv headings bank-acct-name)
                         )]
        {:bank-acct-name bank-acct-name
         :actual-period  actual-period
         :records        records}))))

(defn -import-statements [conn customer-kw]
  (let [org-meta (q/find-org-meta conn customer-kw)
        importing-meta (q/find-importing-meta conn customer-kw)
        f (us/flip dhs/range-of-periods)
        range-of-periods-f (year-taker (partial f org-meta))]
    (->> org-meta
         (dhs/range-of-years nil)
         (mapcat range-of-periods-f)
         u/probe-off
         (-do-statements-import conn org-meta importing-meta))))

(def records-count #(-> % :records count))

(defn period-desc [{:keys [actual-period]}]
  ((juxt :period/year :period/quarter) actual-period))

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn set-default-current-time-ordinal []
  (let [conn (d/connect db-uri)
        org-key :seaweed
        max-ord (->> (q/read-statements conn org-key)
                     (map :statement/time-ordinal)
                     (apply max))]
    (e/update-organisations-time-ordinal conn org-key max-ord)
    (println (str "Max ordinal for " org-key " is now " max-ord))))

(defn import-bank-statements []
  (let [conn (d/connect db-uri)
        statement-make (partial e/make-statement conn)
        org-key :seaweed
        statements (->> org-key
                        (-import-statements conn)
                        (keep statement-make))]
    (println (str "Number of bank statements imported: " (count statements)))))