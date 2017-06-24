(ns app.basic-ui
  (:require [untangled.client.core :as uc]
            [om.dom :as dom]
            [app.operations :as ops]
            [app.cljs-operations :as cljs-ops]
            [app.panels :as p]
            [app.ui-helpers :as ui-help]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [untangled.client.network :as net]
            [untangled.ui.forms :as f]
            [app.util :as u]))

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
  (query [this] [:db/id :ledger-item-list/label {:ledger-item-list/items (om/get-query LedgerItem)}])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id label]}]
    {:db/id                   id
     :ledger-item-list/label  label
     :ledger-item-list/items []})
  Object
  (render [this]
    (let [{:keys [db/id ledger-item-list/label ledger-item-list/items]} (om/props this)
          delete-ledger-item (fn [ledger-item-id]
                               (js/console.log label "asked to delete" name)
                               (om/transact! this `[(ops/delete-ledger-item {:list-id ~id :ledger-item-id ~ledger-item-id})]))]
      (dom/div nil
               (dom/h4 nil label)
               (dom/ul nil
                       (map (fn [ledger-item] (ui-ledger-item (om/computed ledger-item {:onDelete delete-ledger-item}))) items))))))

(def ui-ledger-item-list (om/factory LedgerItemList))

(defui ^:once PotentialData
  static om/Ident
  (ident [this props] [:potential-data/by-id p/POTENTIAL_DATA])
  static om/IQuery
  (query [this] [:potential-data/period-type :potential-data/commencing-period :potential-data/latest-period])
  Object
  (render [this]
    (let [{:keys [potential-data/period-type potential-data/commencing-period potential-data/latest-period]} (om/props this)
          period-label (condp = period-type
                         :period-type/quarterly "Quarter"
                         :period-type/monthly "Month"
                         :period-type/unknown ""
                         nil "")]
      (dom/div nil
               (dom/h4 nil "Year")
               (dom/h4 nil period-label)))))

(def ui-potential-data (om/factory PotentialData))

(defn period->year [{:keys [period/tax-year period/year]}]
  (or tax-year year))

(defn period->period [{:keys [period/quarter period/month]}]
  (or quarter month))

;;
;; The default year and period s/be worked out as the last ones in potential data
;;
(defui ^:once UserRequestForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id          id
                        :potential-data {:potential-data/period-type :period-type/unknown}}))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/dropdown-input :request/year [(f/option ::f/none "Not yet loaded")])
                     (f/dropdown-input :request/period [(f/option ::f/none "Not yet loaded")])])
  static om/Ident
  (ident [_ props] [:user-request/by-id (:db/id props)])
  static om/IQuery
  (query [_] [:db/id :request/year :request/period {:potential-data (om/get-query PotentialData)} f/form-root-key])
  Object
  #_(render [this]
    (let [{:keys [potential-data request/year request/period] :as form} (om/props this)
          {:keys [potential-data/period-type]} potential-data
          _ (u/warn (not= :period-type/unknown period-type) (str "No period type from: " potential-data))
          period-label (condp = period-type
                         :period-type/quarterly "Quarter"
                         :period-type/monthly "Month"
                         :period-type/unknown "Unknown"
                         nil "Unknown")
          year (or year (-> potential-data :potential-data/latest-period period->year u/probe-on))
          period (or period (-> potential-data :potential-data/latest-period period->period))]
      (dom/div #js {:className "form-horizontal"}
               (ui-help/field-with-label this form :request/year "Year")
               (ui-help/field-with-label this form :request/period period-label))))
  (render [this]
            (let [{:keys [potential-data]} (om/props this)]
              (dom/div nil (ui-potential-data potential-data)))))

(def ui-user-request-form (om/factory UserRequestForm))

(defui ^:once Root
  static om/IQuery
  (query [this] [:ui/react-key
                 {:root/potential-data (om/get-query PotentialData)}
                 {:root/user-request (om/get-query UserRequestForm)}
                 {:root/selected-items (om/get-query LedgerItemList)}])
  static
  uc/InitialAppState
  (initial-state [c params]
    {:root/selected-items
     (uc/get-initial-state LedgerItemList
                           {:id p/LEDGER_ITEMS_LIST :label "Account Balances"})
     :root/user-request
     (uc/get-initial-state UserRequestForm
                           {:id p/USER_REQUEST_FORM :potential-data {}})})
  Object
  (render [this]
    (let [{:keys [ui/react-key root/potential-data root/user-request root/selected-items]} (om/props this)
          ]
      (dom/div #js {:key react-key}
               (ui-user-request-form user-request)
               ;(dom/button #js {:onClick (fn [] (df/load this [:ledger-item/by-id 3] LedgerItem))}
               ;            "Refresh Selected Item with ID 3")
               (ui-ledger-item-list selected-items)))))

(defonce app-1 (atom (uc/new-untangled-client
                       :networking {:remote (net/make-untangled-network
                                              "/api"
                                              :global-error-callback (constantly nil))}
                       :started-callback (fn [app]
                                           (df/load app :my-potential-data PotentialData
                                                    {:refresh [:potential-data/period-type]
                                                     :post-mutation `cljs-ops/rm-my-potential-data
                                                     })
                                           (df/load app :my-selected-items LedgerItem
                                                    {:target [:ledger-item-list/by-id
                                                              p/LEDGER_ITEMS_LIST
                                                              :ledger-item-list/items]
                                                     :post-mutation `cljs-ops/sort-items-by-name
                                                     })))))
