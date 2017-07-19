(ns accounting.play
  (:require [accounting.queries :as q]
            [datomic.api :as d]
            [accounting.util :as u]
            [accounting.match :as m]
            [cljc.utils :as us]))

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn query-current-period-rules []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (q/find-current-period-rules conn customer-kw)))

(defn query-statements []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (q/read-statements conn customer-kw)))

(defn query-bank-template []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (map (juxt :bank-acct-name
               :template-str
               ) (q/find-importing-meta conn customer-kw))))

(defn query-org []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    ((juxt :root-dir
           :organisation/period-type
           :organisation/timespan
           ) (q/find-org-meta conn customer-kw))))

(defn query-headings []
  (let [conn (d/connect db-uri)
        account-name "anz-visa"]
    (q/find-headings conn account-name)))

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
    (d/pull db '[*] [:rule/rule-num 3])))

;;
;; All rules from AMP are for personal spending. Makes sense.
;;
(defn amp-is-personal []
  (->> (query-current-period-rules)
       (filter #(= "amp" (-> % :rule/target-account :account/name)))
       (map (juxt :rule/source-bank :rule/target-account))
       (take 3)))

(defn general-query-2 []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (->> (q/find-current-period conn customer-kw)
         u/probe-off
         )))

(defn general-query-1 []
  (->> (query-current-period-rules)
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
                  #(-> % :rule/time-slot some?)))))

(defn query-line-items []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed]
    (q/find-line-items conn customer-kw 2017 :q3)))

(defn query-filter-current-period-rules []
  (let [conn (d/connect db-uri)
        customer-kw :seaweed
        bank-account #:account{:category :bank :name "anz-visa"}
        ledger-account #:account{:category :exp :name "niim-trip"}]
    (->> (q/read-period-specific-rules conn customer-kw 2017 :q3)
         (m/filter-rules-new #{bank-account} #{ledger-account})
         us/probe-count-on)))
