(ns app.operations
  (:require
    [app.seasoft-context :as context]
    [untangled.server :as server :refer [defquery-root defquery-entity defmutation]]
    [taoensso.timbre :as timbre]
    [accounting.util :as u]
    [accounting.api :as api]
    [untangled.ui.forms :as f]
    [untangled.server :as s]))

(def people-db (atom {1  {:db/id 1 :ledger-item/name "Bert" :ledger-item/amount 55}
                      2  {:db/id 2 :ledger-item/name "Sally" :ledger-item/amount 22}
                      3  {:db/id 3 :ledger-item/name "Allie" :ledger-item/amount 76}
                      4  {:db/id 4 :ledger-item/name "Zoe" :ledger-item/amount 32}
                      99 {:db/id 99 :ledger-item/name "Chris"}}))

(defquery-root :current-user
               "Queries for the current user returns it to the client"
               (value [env params]
                      (get @people-db 99)))

#_(defmutation add-phone
             [{:keys [id person #_phone-form]}]
             (action [{:keys [state]}]
                     (timbre/info "Server add-phone" id " " person)))

(defmethod s/server-mutate `f/commit-to-entity [env k params]
  {:action (fn []
             (timbre/info "Mutation for " (u/pp params)))})

(defmutation delete-ledger-item
             "Server Mutation: Handles deleting a ledger-item on the server"
             [{:keys [list-id ledger-item-id]}]
             (action [{:keys [state]}]
                     (timbre/info "Server deleting ledger-item" ledger-item-id)
                     (swap! people-db dissoc ledger-item-id)))

(defquery-entity :ledger-item/by-id
                 "Server query for allowing the client to pull an individual ledger-item from the database"
                 (value [env id params]
                        (update (get @people-db id) :ledger-item/name str " (refreshed)")))

(defquery-root :my-selected-items
               "Queries for selected-items and returns them to the client"
               (value [{:keys [query]} {:keys [request/organisation request/year request/period request/report]}]
                      (api/fetch-report query organisation year period report)))

(defquery-root :my-potential-data
               "Queries for potential-data and returns it to the client"
               (value [{:keys [query]} {:keys [request/organisation]}]
                      (api/potential-data query organisation)))

(defquery-root :my-config-data
               "Queries for config-data and returns it to the client"
               (value [{:keys [query]} {:keys [request/organisation]}]
                      (api/config-data query organisation)))

(defquery-root :my-unruly-bank-statement-line
               "Queries for the next unruly line and returns it to the client"
               (value [{:keys [query]} {:keys []}]
                      (api/next-unruly-line query)))

(defquery-root :my-existing-rules
               "Queries for existing rules"
               (value [{:keys [query]} {:keys [source-bank target-ledger]}]
                      (api/rules-from-bank-ledger query source-bank target-ledger)))
