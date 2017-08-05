(ns app.operations
  (:require
    [app.seasoft-context :as context]
    [fulcro.server :as server :refer [defquery-root defquery-entity defmutation]]
    [taoensso.timbre :as timbre]
    [accounting.util :as u]
    [accounting.api :as old-api]
    [accounting.experiments-api :as experiments-api]
    [accounting.datomic-api :as new-api]
    [fulcro.ui.forms :as f]
    [datomic.api :as d]
    [accounting.datomic-helpers :as dh]
    [untangled.datomic.protocols :as db]))

(def people-db (atom {1  {:db/id 1 :ledger-item/name "Bert" :ledger-item/amount 55}
                      2  {:db/id 2 :ledger-item/name "Sally" :ledger-item/amount 22}
                      3  {:db/id 3 :ledger-item/name "Allie" :ledger-item/amount 76}
                      4  {:db/id 4 :ledger-item/name "Zoe" :ledger-item/amount 32}
                      99 {:db/id 99 :ledger-item/name "Chris"}}))

(defquery-root :current-user
               "Queries for the current user returns it to the client"
               (value [env params]
                      (get @people-db 99)))

;;
;; :attribute             :organisation/key
;; :attribute-value-value :seaweed
;; :master-class          :people/by-id
;; :detail-class          :phone/by-id
;;
(defmutation commit-to-within-entity
             [{:keys [form-diff within]}]
             (action [{:keys [b00ks-database]} ;; :b00ks-database :request :parser :target :query-root :path :ast
                      ]
                     (u/fulcro-assert (not (dh/unimplemented-keys? form-diff))
                                      (str "Not yet coded for these keys: "
                                           (dh/unimplemented-keys? form-diff) form-diff))
                     (let [{:keys [attribute-value-value attribute]} within
                           conn (:connection b00ks-database)
                           ;_ (println "conn: <" conn ">")
                           eid (and attribute (d/q '[:find ?e .
                                                     :in $ ?o ?a
                                                     :where [?e ?a ?o]]
                                                   (d/db conn) attribute-value-value attribute))
                           {:keys [omid->tempid tx]} (dh/datomic-driver-1 (assoc within :eid eid) form-diff)
                           result @(d/transact conn tx)
                           tempid->realid (:tempids result)
                           omids->realids (dh/resolve-ids (d/db conn) omid->tempid tempid->realid)]
                       {:tempids omids->realids})))

(defmutation delete-ledger-item
             "Server Mutation: Handles deleting a ledger-item on the server"
             [{:keys [list-id ledger-item-id]}]
             (action [{:keys [state]}]
                     (timbre/info "Server deleting ledger-item" ledger-item-id)
                     (swap! people-db dissoc ledger-item-id)))

(comment "Not yet using defquery-entity"
         (defquery-entity :ledger-item/by-id
                          "Server query for allowing the client to pull an individual ledger-item from the database"
                          (value [env id params]
                                 (update (get @people-db id) :ledger-item/name str " (refreshed)"))))

(defquery-root :my-selected-items
               "Queries for selected-items and returns them to the client"
               (value [{:keys [query]} {:keys [request/organisation request/year request/period request/report]}]
                      (old-api/fetch-report query organisation year period report)))

(defquery-root :my-potential-data-old
               "Queries for potential-data and returns it to the client"
               (value [{:keys [query]} {:keys [request/organisation]}]
                      (old-api/potential-data query organisation)))

(defquery-root :my-config-data
               "Queries for config-data and returns it to the client"
               (value [{:keys [query]} {:keys [request/organisation]}]
                      (old-api/config-data query organisation)))

(defquery-root :my-unruly-bank-statement-line
               "Queries for the next unruly line and returns it to the client"
               (value [{:keys [query b00ks-database]} {:keys [request/organisation]}]
                      (new-api/next-unruly-line (:connection b00ks-database) organisation)))

(defquery-root :my-existing-rules
               "Queries for existing rules"
               (value [{:keys [query b00ks-database]} {:keys [request/organisation source-bank target-ledger]}]
                      (new-api/rules-from-bank-ledger (:connection b00ks-database) organisation source-bank target-ledger)))

(defquery-root :district-query
               "Datomic query"
               (value [{:keys [query districts-database] :as env} {:keys [district-name]}]
                      (println "DATOMIC query: " query " using: <" district-name ">")
                      (u/probe-on (experiments-api/read-district (:connection districts-database) query district-name))))

(defquery-root :my-potential-data-new
               "Queries for potential-data and returns it to the client"
               (value [{:keys [query b00ks-database]} {:keys [request/organisation]}]
                      (new-api/organisation-data (:connection b00ks-database) query organisation)))

(defquery-root :my-selected-items-new
               "Queries for selected-items and returns them to the client"
               (value [{:keys [query b00ks-database]} {:keys [request/organisation request/year request/period request/report]}]
                      (new-api/fetch-report (:connection b00ks-database) query organisation year period report)))
