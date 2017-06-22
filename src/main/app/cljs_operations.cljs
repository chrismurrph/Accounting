(ns app.cljs-operations
  (:require
    [untangled.client.mutations :as m :refer [defmutation]]
    [om.next :as om]))

(defn sort-selected-items-by*
  "Sort the idents in the selected-items ledger-item list. Returns the new app-state."
  [state-map field]
  (let [friend-idents (get-in state-map [:ledger-item-list/by-id :selected-items :ledger-item-list/people] [])
        selected-items (map (fn [friend-ident] (get-in state-map friend-ident)) friend-idents)
        sorted-selected-items (sort-by field selected-items)
        new-idents (mapv (fn [friend] [:ledger-item/by-id (:db/id friend)]) sorted-selected-items)]
    (assoc-in state-map [:ledger-item-list/by-id :selected-items :ledger-item-list/people] new-idents)))

(defmutation sort-selected-items [no-params]
             (action [{:keys [state]}]
                     (swap! state sort-selected-items-by* :ledger-item/name)))

(defmutation uncover-first [no-params]
             (action [{:keys [state]}]
                     (swap! state update :server/potential-data first)))
