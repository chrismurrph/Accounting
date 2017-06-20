(ns app.operations
  (:require
    [untangled.server :as server :refer [defquery-root defquery-entity defmutation]]
    [taoensso.timbre :as timbre]))

(def people-db (atom {1  {:db/id 1 :ledger-item/name "Bert" :ledger-item/age 55 :ledger-item/relation :friend}
                      2  {:db/id 2 :ledger-item/name "Sally" :ledger-item/age 22 :ledger-item/relation :friend}
                      3  {:db/id 3 :ledger-item/name "Allie" :ledger-item/age 76 :ledger-item/relation :enemy}
                      4  {:db/id 4 :ledger-item/name "Zoe" :ledger-item/age 32 :ledger-item/relation :friend}
                      99 {:db/id 99 :ledger-item/name "Me" :ledger-item/role "admin"}}))

(def trial-balance (atom {}))

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

(defn get-people [kind keys]
  (->> @people-db
    vals
    (filter #(= kind (:ledger-item/relation %)))
    vec))

(defquery-entity :ledger-item/by-id
  "Server query for allowing the client to pull an individual ledger-item from the database"
  (value [env id params]
    (timbre/info "Query for ledger-item" id)
    (update (get @people-db id) :ledger-item/name str " (refreshed)")))

(defquery-root :my-selected-items
  "Queries for selected-items and returns them to the client"
  (value [{:keys [query]} params]
    (get-people :friend query)))
