(ns accounting.datomic-api
  (:require [datomic.api :as d]
            [accounting.util :as u]
            [accounting.core :as c]
            [cljc.domain-helpers :as dhs]
            [cljc.utils :as us]))

#_(defn make-district [connection list-name]
    (let [id (d/tempid :db.part/user)
          tx [{:db/id id :district/name list-name}]
          idmap (:tempids @(d/transact connection tx))
          real-id (d/resolve-tempid (d/db connection) idmap id)]
      real-id))

(defn find-organisation
  "Find an organisation with the given name. Always returns a valid organisation ID."
  [conn org-key]
  (when-let [eid (d/q '[:find ?e . :in $ ?o :where [?e :organisation/key ?o]] (d/db conn) org-key)]
    eid))

(defn read-bank-accounts
  "Find a bank account with the org."
  [conn org-key]
  (let [db (d/db conn)
        eids (d/q '[:find [?a ...] :in $ ?o :where
                    [?e :organisation/key ?o]
                    [?e :organisation/bank-accounts ?a]] db org-key)
        rvs (mapv #(d/pull db '[:db/id] %) eids)]
    rvs))

;;
;; Only used to make it human readable
;;
(defn db-actual-period-> [{:keys [actual-period/year actual-period/period]}]
  (let [{:keys [period/type period/month period/quarter]} period
        {:keys [db/ident]} quarter
        period-type (:db/ident type)]
    (assert (= period-type :period.type/quarterly) type)
    {:period/type period-type :period/year year :period/quarter ident}))

(def -actual-period-pull [:actual-period/year
                          {:actual-period/period [:period/type
                                                  :period/month
                                                  :period/quarter]}])
(def timespan-pull [{:timespan/commencing-period -actual-period-pull}
                    {:timespan/latest-period -actual-period-pull}])

(def heading-key-pull [:heading/ordinal :heading/key])

(defn -read-statement-importing-meta [conn org-key]
  (assert org-key)
  (let [db (d/db conn)]
    (d/q '[:find ?an ?ts
           :in $ ?o
           :where
           [?e :organisation/key ?o]
           [?e :organisation/import-templates ?t]
           [?t :import-template/account ?a]
           [?t :import-template/template-str ?ts]
           [?a :account/name ?an]
           ] db org-key)))

(defn -read-headings [conn account-name]
  (assert account-name)
  (let [db (d/db conn)]
    (d/q '[:find ?ahs
           :in $ ?an
           :where
           [?a :account/name ?an]
           [?a :account/headings ?ahs]
           ] db account-name)))

(defn -read-organisation-meta [conn org-key]
  (assert org-key)
  (let [db (d/db conn)]
    (d/q '[:find ?dr ?pt ?tis
           :in $ ?o
           :where
           [?e :organisation/key ?o]
           [?e :organisation/import-data-root ?dr]
           [?e :organisation/period-type ?pt]
           [?e :organisation/timespan ?tis]
           ] db org-key)))

(defn find-importing-meta [conn org-key]
  (let [db (d/db conn)]
    (for [[bank-acct-name template-str]
          (us/count-probe-off (-read-statement-importing-meta conn org-key))]
      {:bank-acct-name bank-acct-name
       :template-str   template-str
       })))

(defn find-headings [conn account-name]
  (let [db (d/db conn)]
    (->> (for [[heading-eid]
               (us/count-probe-off (-read-headings conn account-name))]
           (d/pull db heading-key-pull heading-eid))
         (sort-by :heading/ordinal)
         (map :heading/key))))

(defn find-org-meta [conn org-key]
  (let [db (d/db conn)]
    (let [[[root-dir period-type timespan]] (seq (-read-organisation-meta conn org-key))]
      {:root-dir                 root-dir
       :organisation/period-type period-type
       :organisation/timespan    (d/pull db timespan-pull timespan)
       })))

(defn read-organisation [connection query org-key]
  (let [org-id (find-organisation connection org-key)
        db (d/db connection)
        rv (d/pull db query org-id)]
    rv))

(defn organisation-data [conn kws org-key]
  (assert conn)
  (assert (= org-key :seaweed))
  ;(println kws)
  (let [res (read-organisation conn kws org-key)]
    ;(println (-> res :organisation/possible-reports))
    res))


#_(defn trial-balance-report [conn organisation year period]
    (let [
          ;{:keys [splits rules-fn bank-statements-fn starting-balances-fn] :as for-org} (organisation by-org)
          year (u/kw->number year)
          bank-statements (bank-statements-fn year period)
          current-rules (rules-fn year period)
          starting-balances (starting-balances-fn year period)]
      (-> (c/trial-balance bank-statements current-rules splits starting-balances)
          map->ledger-items)))

(defn bank-statements [conn organisation year period]
  )

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
  (let [bank-accounts (read-bank-accounts conn organisation)]
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

(def db-uri "datomic:dev://localhost:4334/b00ks")

;;
;; The function used (f) didn't think to give us back the year, so this wrapper does
;; the job for us.
;; Notice not yet returning Datomic format. We might need to do that later.
;;
(defn year-taker [f]
  (fn [year]
    (let [periods (f year)]
      ;(println "periods:" periods)
      (map (fn [period] {:period/year year :period/quarter period}) periods))))

(defn import-statements []
  (let [c (d/connect db-uri)
        customer-kw :seaweed
        {:keys [organisation/period-type] :as org-meta} (find-org-meta c customer-kw)
        importing-meta (find-importing-meta c customer-kw)
        years (dhs/range-of-years nil org-meta)
        f (us/flip dhs/range-of-periods)
        range-of-periods-f (year-taker (partial f org-meta))
        periods (mapcat range-of-periods-f years)
        ]
    (for [{:keys [bank-acct-name template-str]} importing-meta
          period periods]
      (let [headings (find-headings c bank-acct-name)]
        (->> (c/slurp-raw-data->csv-datomic! org-meta period-type template-str period)
             ;(take 1)
             u/probe-off
             (c/parse-csv headings bank-acct-name)
             )))))

(defn general-query-1 []
  (let [c (d/connect db-uri)
        customer-kw :seaweed]
    (map (juxt :bank-acct-name
               :template-str
               ) (find-importing-meta c customer-kw))))

(defn general-query-1 []
  (let [c (d/connect db-uri)
        customer-kw :seaweed]
    (map (juxt :root-dir
               :organisation/period-type
               :organisation/timespan
               ) (find-org-meta c customer-kw))))

(defn general-query-2 []
  (let [c (d/connect db-uri)
        customer-kw :seaweed]
    ((juxt :root-dir
           :organisation/period-type
           :organisation/timespan
           ) (find-org-meta c customer-kw))))

(defn general-query-3 []
  (let [c (d/connect db-uri)
        account-name "anz-visa"]
    (find-headings c account-name)))