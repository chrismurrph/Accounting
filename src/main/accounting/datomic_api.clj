(ns accounting.datomic-api
  (:require [datomic.api :as d]
            [accounting.util :as u]
            [accounting.core :as c]
            [cljc.domain-helpers :as dhs]
            [cljc.utils :as us]
            [accounting.entities :as e]
            [accounting.queries :as q]))

#_(defn make-district [connection list-name]
    (let [id (d/tempid :db.part/user)
          tx [{:db/id id :district/name list-name}]
          idmap (:tempids @(d/transact connection tx))
          real-id (d/resolve-tempid (d/db connection) idmap id)]
      real-id))

(defn organisation-data [conn query org-key]
  (assert conn)
  (assert (= org-key :seaweed))
  ;(println kws)
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

;;
;; The id doesn't seem to matter, nor for potential data above
;;
(defn next-unruly-line [conn kws]
  (merge {:db/id 'BANK-STATEMENT-LINE
          ;:bank-line/src-bank :bank/anz-visa
          ;:bank-line/date     "24/08/2016"
          ;:bank-line/desc     "OFFICEWORKS SUPERSTO      KESWICK"
          ;:bank-line/amount   71.01M
          }
         (->> (c/records-without-single-rule-match nil (q/query-rules))
              ffirst
              (map (fn [[k v]]
                     [({:out/date     :bank-line/date
                        :out/amount   :bank-line/amount
                        :out/desc     :bank-line/desc
                        :out/src-bank :bank-line/src-bank}
                        k) v]))
              (into {})
              u/probe-off)))


(def trial-balance-report nil)

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

(declare make-ledger-item)

(defn biggest-items-report [conn organisation year period]
  (assert period)
  (println period)
  (let [bank-accounts (q/read-bank-accounts conn organisation)]
    (println bank-accounts))
  (make-ledger-item 1 [:dummy-entry 1002]))

(defn make-ledger-item [idx [kw amount]]
  (assert (keyword? kw) (str "Expect a keyword but got: " kw ", has type: " (type kw)))
  {:db/id              idx
   :ledger-item/name   (name kw)
   :ledger-item/type   ((comp keyword namespace) kw)
   :ledger-item/amount amount})

(def rep->fn
  {:report/profit-and-loss (fn [_ _ _ _] [(make-ledger-item 0 [:dummy-entry 1000])])
   :report/balance-sheet   (fn [_ _ _ _] [(make-ledger-item 1 [:dummy-entry 1001])])
   :report/big-items-first biggest-items-report
   :report/trial-balance   trial-balance-report})

(defn fetch-report [conn query organisation year period report]
  (assert (= 4 (count query)))
  ((report rep->fn) conn organisation year period))

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
         u/probe-on
         (-do-statements-import conn org-meta importing-meta))))

(def records-count #(-> % :records count))

(defn period-desc [{:keys [actual-period]}]
  ((juxt :period/year :period/quarter) actual-period))

(defn import-bank-statements []
  (let [conn (d/connect q/db-uri)
        statement-make (partial e/make-statement conn)
        org-key :seaweed
        statements (->> org-key
                        (-import-statements conn)
                        (keep statement-make))]
    (println (str "Number of statements imported: " (count statements)))
    ;statements
    ))