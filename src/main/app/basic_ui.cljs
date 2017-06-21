(ns app.basic-ui
  (:require [untangled.client.core :as uc]
            [om.dom :as dom]
            [app.operations :as ops]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [untangled.client.mutations :as m]
            [untangled.client.network :as net]))

(defui ^:once LedgerItem
  static om/Ident
  (ident [this props] [:ledger-item/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :ledger-item/name :ledger-item/amount])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id name amount] :as params}] {:db/id id :ledger-item/name name :ledger-item/amount amount})
  Object
  (render [this]
    (let [{:keys [db/id ledger-item/name ledger-item/amount]} (om/props this)
          onDelete (om/get-computed this :onDelete)]
      (dom/li nil
              (dom/h5 nil name (str "(amount: " amount ")")
                      (dom/button #js {:onClick #(df/refresh! this)} "Refresh")
                      (dom/button #js {:onClick #(onDelete id)} "X"))))))

(def ui-ledger-item (om/factory LedgerItem {:keyfn :ledger-item/name}))

(defui ^:once LedgerItemList
  static om/Ident
  (ident [this props] [:ledger-item-list/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :ledger-item-list/label {:ledger-item-list/people (om/get-query LedgerItem)}])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id label]}]
    {:db/id                   id
     :ledger-item-list/label  label
     :ledger-item-list/people []})
  Object
  (render [this]
    (let [{:keys [db/id ledger-item-list/label ledger-item-list/people]} (om/props this)
          delete-ledger-item (fn [ledger-item-id]
                               (js/console.log label "asked to delete" name)
                               (om/transact! this `[(ops/delete-ledger-item {:list-id ~id :ledger-item-id ~ledger-item-id})]))]
      (dom/div nil
               (dom/h4 nil label)
               (dom/ul nil
                       (map (fn [ledger-item] (ui-ledger-item (om/computed ledger-item {:onDelete delete-ledger-item}))) people))))))

(def ui-ledger-item-list (om/factory LedgerItemList))

(defui ^:once PotentialData
  static om/Ident
  (ident [this props] [:potential-data/by-id :the-one])
  static om/IQuery
  (query [this] [:potential-data/period-type :potential-data/commencing-period :potential-data/latest-period])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [period-type commencing-period latest-period] :as params}]
    {:potential-data/period-type       period-type
     :potential-data/commencing-period commencing-period
     :potential-data/latest-period     latest-period})
  Object
  (render [this]
    (let [{:keys [potential-data/period-type potential-data/commencing-period potential-data/latest-period]} (om/props this)
          onDelete (om/get-computed this :onDelete)]
      (dom/li nil
              (dom/h5 nil name (str "(period-type: " period-type ")")
                      (dom/button #js {:onClick #(df/refresh! this)} "Refresh")
                      (dom/button #js {:onClick #(onDelete "Yo!")} "X"))))))

(def ui-potential-data (om/factory PotentialData))

(defui ^:once Root
  static om/IQuery
  (query [this] [:ui/react-key
                 ;Don't see point of this
                 ;:ui/ledger-item-id
                 ;{:current-ledger-item (om/get-query LedgerItem)}
                 ;[:potential-data/by-id :the-one]
                 {:potential-data (om/get-query PotentialData)}
                 {:selected-items (om/get-query LedgerItemList)}])
  static
  uc/InitialAppState
  (initial-state [c params]
    #_{:potential-data
     (uc/get-initial-state PotentialData
                           {})}
    {:selected-items
     (uc/get-initial-state LedgerItemList
                           {:id :selected-items :label "Account Balances"})})
  Object
  (render [this]
    (let [{:keys [ui/react-key potential-data selected-items]} (om/props this)]
      (dom/div #js {:key react-key}
               (dom/h4 nil (str "Period Type: " (keys potential-data)))
               ;(dom/button #js {:onClick (fn [] (df/load this [:ledger-item/by-id 3] LedgerItem))}
               ;            "Refresh Selected Item with ID 3")
               (ui-ledger-item-list selected-items)))))

(defonce app-1 (atom (uc/new-untangled-client
                       :networking {:remote (net/make-untangled-network
                                              "/api"
                                              :global-error-callback (constantly nil))}
                       :started-callback (fn [app]
                                           (df/load app :server/potential-data PotentialData
                                                    {:post-mutation `ops/uncover-first}
                                                    ;{:target [:potential-data/by-id
                                                    ;          :the-one
                                                    ;          :potential-data]}
                                                     )
                                           (df/load app :my-selected-items LedgerItem
                                                    {:target [:ledger-item-list/by-id
                                                              :selected-items
                                                              :ledger-item-list/people]
                                                     ;:post-mutation `ops/sort-selected-items
                                                     })))))
