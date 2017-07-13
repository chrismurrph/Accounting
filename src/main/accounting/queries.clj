(ns accounting.queries
  (:require [datomic.api :as d]
            [cljc.utils :as us]))

(defn read-organisation
  "Read an organisation with the given name. Always returns a valid organisation ID."
  [conn org-key]
  (when-let [eid (d/q '[:find ?e . :in $ ?o :where [?e :organisation/key ?o]] (d/db conn) org-key)]
    eid))

(defn read-bank-accounts
  "Find all bank accounts of the org."
  [conn org-key]
  (let [db (d/db conn)
        eids (d/q '[:find [?a ...] :in $ ?o :where
                    [?e :organisation/key ?o]
                    [?e :organisation/bank-accounts ?a]] db org-key)
        rvs (mapv #(d/pull db '[:db/id] %) eids)]
    rvs))

(def -rule-conditions-pull [:condition/field :condition/predicate :condition/subject])

(def -time-slot-pull [:time-slot/start-at :time-slot/end-at])

(def -account-pull [:account/category :account/name :account/desc
                    {:account/time-slot -time-slot-pull}])

;; Some not yet being used been left out: :rule/dominates :rule/period :rule/on-dates
(def -rule-pull [:rule/logic-operator {:rule/time-slot -time-slot-pull}
                 {:rule/conditions -rule-conditions-pull} {:rule/source-bank -account-pull}
                 {:rule/target-account -account-pull}])

(defn find-rules
  "Find all rules of the org."
  [conn org-key]
  (let [db (d/db conn)
        eids (d/q '[:find [?a ...] :in $ ?o :where
                    [?e :organisation/key ?o]
                    [?e :organisation/rules ?a]] db org-key)
        rvs (mapv #(d/pull db -rule-pull %) eids)]
    rvs))

(def -actual-period-pull [:actual-period/year
                          :actual-period/month
                          :actual-period/quarter
                          :actual-period/type])
(def timespan-pull [{:timespan/commencing-period -actual-period-pull}
                    {:timespan/latest-period -actual-period-pull}])

(def heading-key-pull [:heading/ordinal :heading/key])

(defn -read-headings [conn account-name]
  (assert account-name)
  (let [db (d/db conn)]
    (d/q '[:find ?ahs
           :in $ ?an
           :where
           [?a :account/name ?an]
           [?a :bank-account/headings ?ahs]
           ] db account-name)))

;;
;; Even if one reference returned (here timespan) we call it a `read`
;;
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
  (assert org-key)
  (let [db (d/db conn)]
    (for [[bank-acct-name template-str]
          (us/count-probe-off (let [db (d/db conn)]
                                (d/q '[:find ?an ?ts
                                       :in $ ?o
                                       :where
                                       [?e :organisation/key ?o]
                                       [?e :organisation/import-templates ?t]
                                       [?t :import-template/account ?a]
                                       [?t :import-template/template-str ?ts]
                                       [?a :account/name ?an]
                                       ] db org-key)))]
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

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn query-rules []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (find-rules conn customer-kw)))

(defn query-bank-template []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (map (juxt :bank-acct-name
               :template-str
               ) (find-importing-meta conn customer-kw))))

(defn query-org []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    ((juxt :root-dir
           :organisation/period-type
           :organisation/timespan
           ) (find-org-meta conn customer-kw))))

(defn query-headings []
  (let [conn (d/connect db-uri)
        account-name "anz-visa"]
    (find-headings conn account-name)))
