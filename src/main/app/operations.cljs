(ns app.operations
  (:require
    [untangled.ui.forms :as forms]
    [untangled.client.mutations :as m :refer [defmutation]]
    [om.next :as om]
    [untangled.client.core :as uc]
    [om.dom :as dom]
    [om.next :as om :refer [defui]]
    [untangled.ui.forms :as f]))

(defmutation delete-ledger-item
             "Mutation: Delete the ledger-item with ledger-item-id from the list with list-id"
             [{:keys [list-id ledger-item-id]}]
             (action [{:keys [state]}]
                     (let [ident-to-remove [:ledger-item/by-id ledger-item-id]
                           strip-fk (fn [old-fks]
                                      (vec (filter #(not= ident-to-remove %) old-fks)))]
                       (swap! state update-in [:ledger-item-list/by-id list-id :ledger-item-list/people] strip-fk)))
             (remote [env] true))

(defmutation add-phone
  [{:keys [id person phone-form]}]
  (action [{:keys [state]}]
          (let [new-phone (f/build-form phone-form #_ValidatedPhoneForm {:db/id id :phone/type :home :phone/number ""})
                person-ident [:people/by-id person]
                phone-ident (om/ident phone-form #_ValidatedPhoneForm new-phone)]
            (swap! state assoc-in phone-ident new-phone)
            (uc/integrate-ident! state phone-ident :append (conj person-ident :person/phone-numbers))))
  #_(remote [env] true)
  )

#_(defmutation create-person [{:keys [id]}]
    (action [{:keys [state]}]
            (let [new-phone    (f/build-form uubms/ValidatedPhoneForm {:db/id id :phone/type :home :phone/number ""})
                  person-ident [:people/by-id person]
                  phone-ident  (om/ident uubms/ValidatedPhoneForm new-phone)]
              (swap! state assoc-in phone-ident new-phone)
              (uc/integrate-ident! state phone-ident :append (conj person-ident :person/phone-numbers)))))

