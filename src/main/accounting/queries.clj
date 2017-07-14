(ns accounting.queries
  (:require [datomic.api :as d]
            [cljc.utils :as us]
            [accounting.util :as u]))

(defn read-account [conn account-name account-category]
  (assert account-name)
  (let [db (d/db conn)]
    (d/q '[:find ?a .
           :in $ ?an ?ac
           :where
           [?a :account/name ?an]
           [?a :account/category ?ac]
           ] db account-name account-category)))

(defn read-organisation
  "Read an organisation with the given name. Always returns a valid organisation ID."
  [conn org-key]
  (when-let [eid (d/q '[:find ?e .
                        :in $ ?o
                        :where [?e :organisation/key ?o]]
                      (d/db conn) org-key)]
    eid))

(defn read-bank-accounts
  "Find all bank accounts of the org."
  [conn org-key]
  (let [db (d/db conn)
        eids (d/q '[:find [?a ...] :in $ ?o :where
                    [?e :organisation/key ?o]
                    [?e :organisation/bank-accounts ?a]] db org-key)
        rvs (mapv #(d/pull db '[:db/id :account/name] %) eids)]
    rvs))

(defn read-statements
  "Find all statements of the org."
  [conn org-key]
  (let [db (d/db conn)
        eids (d/q '[:find [?s ...] :in $ ?o :where
                    [?e :organisation/key ?o]
                    [?e :organisation/bank-accounts ?a]
                    [?a :bank-account/statements ?s]] db org-key)
        rvs (mapv #(d/pull db '[:db/id :statement/ordinal] %) eids)]
    rvs))

(defn read-period-statements
  "Find all statements of the org for a particular period."
  [conn org-key year quarter]
  (assert (number? year))
  (assert (keyword? quarter))
  (let [db (d/db conn)
        eids (d/q '[:find [?s ...]
                    :in $ ?o ?y ?q :where
                    [?e :organisation/key ?o]
                    [?e :organisation/bank-accounts ?a]
                    [?a :bank-account/statements ?s]
                    [?s :statement/actual-period ?p]
                    [?p :actual-period/year ?y]
                    [?p :actual-period/quarter ?q]] db org-key year quarter)
        rvs (mapv #(d/pull db '[:db/id :statement/ordinal] %) eids)]
    rvs))

(defn current-period-line-items
  "Current line items from all banks"
  [conn org-key]
  (let [db (d/db conn)
        eids (d/q '[:find [?i ...]
                    :in $ ?o :where
                    [?e :organisation/key ?o]
                    [?e :organisation/bank-accounts ?a]
                    [?e :organisation/current-ordinal ?ord]
                    [?a :bank-account/statements ?s]
                    [?s :statement/ordinal ?ord]
                    [?s :statement/line-items ?i]] db org-key)
        rvs (mapv #(d/pull db '[*] %) eids)]
    rvs))

(def -rule-conditions-pull [:condition/field :condition/predicate :condition/subject])

(def -time-slot-pull [:time-slot/start-at :time-slot/end-at])

(def -account-pull [:account/category :account/name :account/desc
                    {:account/time-slot -time-slot-pull}])

(def -actual-period-pull [:actual-period/year
                          :actual-period/month
                          :actual-period/quarter
                          :actual-period/type])

;; Some not yet being used been left out: :rule/dominates :rule/actual-period :rule/on-dates
(def -rule-pull [:db/id
                 :rule/rule-num
                 :rule/logic-operator
                 {:rule/time-slot -time-slot-pull}
                 {:rule/actual-period -actual-period-pull}
                 :on-dates
                 {:rule/conditions -rule-conditions-pull}
                 {:rule/source-bank -account-pull}
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
      {:root-dir                 (u/err-nil root-dir)
       :organisation/period-type (u/err-nil period-type)
       :organisation/timespan    (u/err-nil (d/pull db timespan-pull timespan))
       })))

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn query-rules []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (find-rules conn customer-kw)))

(defn query-line-items []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (current-period-line-items conn customer-kw)))

(defn query-statements []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (read-statements conn customer-kw)))

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

;;
;; Tells me the console's browser needs to be refreshed!
;;
(defn ->entity []
  (let [id 17592186045473
        conn (d/connect db-uri)
        db (d/db conn)
        entity (d/entity db id)
        db-entity (d/entity-db entity)
        fetched (d/pull db '[*] id)]
    fetched))

;;
;; Only works when :rule/rule-num is :unique-identity
;;
(defn ->eid []
  (let [conn (d/connect db-uri)
        db (d/db conn)]
    (d/pull db '[*] [:rule/rule-num 1])))

;;
;; All rules from AMP are for personal spending. Makes sense.
;;
(defn amp-is-personal []
  (->> (query-rules)
       (filter #(= "amp" (-> % :rule/target-account :account/name)))
       (map (juxt :rule/source-bank :rule/target-account))
       (take 3)))

(defn general-query []
  (->> (query-rules)
       (map #(dissoc % :rule/conditions :rule/logic-operator :rule/source-bank :rule/target-account))
       (filter #(or
                  (-> % :rule/actual-period some?)
                  (-> % :rule/time-slot some?)
                  (-> % :rule/on-dates count pos?)
                  #_(= "anz-visa" (-> % :rule/target-account :account/name))))
       ;(keep keys)
       ;(map (juxt :rule/source-bank :rule/target-account))
       ;(take 20)
       (map (juxt :db/id
                  :rule/rule-num
                  #(-> % :on-dates count)
                  #(-> % :rule/actual-period some?)
                  #(-> % :rule/time-slot some?)))
       ))