(ns app.operations
  (:require
    [fulcro.ui.forms :as forms]
    [fulcro.client.mutations :as m :refer [defmutation]]
    [om.next :as om]
    [fulcro.client.core :as uc]
    [om.dom :as dom]
    [om.next :as om :refer [defui]]
    [fulcro.ui.forms :as f]))

(defmutation delete-ledger-item
             "Mutation: Delete the ledger-item with ledger-item-id from the list with list-id"
             [{:keys [list-id ledger-item-id]}]
             (action [{:keys [state]}]
                     (let [ident-to-remove [:ledger-item/by-id ledger-item-id]
                           strip-fk (fn [old-fks]
                                      (vec (filter #(not= ident-to-remove %) old-fks)))]
                       (swap! state update-in [:ledger-item-list/by-id list-id :ledger-item-list/people] strip-fk)))
             (remote [env] true))
