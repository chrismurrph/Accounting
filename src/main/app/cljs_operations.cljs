(ns app.cljs-operations
  (:require
    [untangled.client.mutations :as m :refer [defmutation]]
    [om.next :as om]
    [app.panels :as p]))

(defn sort-selected-items-by*
  "Sort the idents in the selected-items ledger-item list. Returns the new app-state."
  [state-map field]
  (let [items (get-in state-map [:ledger-item-list/by-id p/LEDGER_ITEMS_LIST :ledger-item-list/items] [])
        selected-items (map (fn [item-ident] (get-in state-map item-ident)) items)
        sorted-selected-items (sort-by field selected-items)
        new-idents (mapv (fn [item] [:ledger-item/by-id (:db/id item)]) sorted-selected-items)]
    (println (str "SORTED by " field " -> " new-idents))
    (assoc-in state-map [:ledger-item-list/by-id p/LEDGER_ITEMS_LIST :ledger-item-list/items] new-idents)))

(defmutation sort-items-by-amount [no-params]
             (action [{:keys [state]}]
                     (swap! state sort-selected-items-by* :ledger-item/amount)))

(defmutation sort-items-by-name [no-params]
             (action [{:keys [state]}]
                     (swap! state sort-selected-items-by* :ledger-item/name)))

(defmutation rm-my-potential-data [no-params]
             (action [{:keys [state]}]
                     (swap! state dissoc :my-potential-data)))

#_(defmethod m/mutate 'rm-my-potential-data [{:keys [state]} k {:keys [id]}]
  {:action (fn []
             (swap! state dissoc :my-potential-data))})

(defmutation uncover-first [no-params]
             (action [{:keys [state]}]
                     (swap! state update :server/potential-data first)))
