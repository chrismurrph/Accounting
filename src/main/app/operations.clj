(ns app.operations
  (:require
    [app.seasoft-context :as context]
    [untangled.server :as server :refer [defquery-root defquery-entity defmutation]]
    [taoensso.timbre :as timbre]
    [accounting.util :as u]
    [accounting.api :as api]))

(def people-db (atom {1  {:db/id 1 :ledger-item/name "Bert" :ledger-item/amount 55}
                      2  {:db/id 2 :ledger-item/name "Sally" :ledger-item/amount 22}
                      3  {:db/id 3 :ledger-item/name "Allie" :ledger-item/amount 76}
                      4  {:db/id 4 :ledger-item/name "Zoe" :ledger-item/amount 32}
                      99 {:db/id 99 :ledger-item/name "Chris"}}))

(defquery-root :current-user
               "Queries for the current user returns it to the client"
               (value [env params]
                      (get @people-db 99)))

(defmutation delete-ledger-item
             "Server Mutation: Handles deleting a ledger-item on the server"
             [{:keys [list-id ledger-item-id]}]
             (action [{:keys [state]}]
                     (timbre/info "Server deleting ledger-item" ledger-item-id)
                     (swap! people-db dissoc ledger-item-id)))

#_(defn get-people [keys]
  (->> @people-db
       vals
       #_(filter #(= kind (:ledger-item/relation %)))
       vec))

(defquery-entity :ledger-item/by-id
                 "Server query for allowing the client to pull an individual ledger-item from the database"
                 (value [env id params]
                        (timbre/info "Query for ledger-item" id)
                        (update (get @people-db id) :ledger-item/name str " (refreshed)")))

(defquery-root :my-selected-items
               "Queries for selected-items and returns them to the client"
               (value [{:keys [query]} {:keys [request/year request/period request/report]}]
                      (timbre/info "Query :my-selected-items:" query year period report)
                      (api/fetch-report query year period report)))

(defquery-root :my-potential-data
               "Queries for potential-data and returns it to the client"
               (value [{:keys [query]} params]
                      ;(timbre/info "Query :my-potential-data:" query)
                      (u/probe-off (context/potential-data query) "my-potential-data")))
