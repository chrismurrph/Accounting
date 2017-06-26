(ns app.frontend
  (:require [untangled.client.core :as uc]
            [om.dom :as dom]
            [app.operations :as ops]
            [app.cljs-operations :as cljs-ops]
            [app.panels :as p]
            [app.domain-ui-helpers :as domain]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [untangled.client.network :as net]
            [untangled.ui.forms :as f]
            [app.util :as u]
            [app.forms-helpers :as fh]
            [app.domain-ui-helpers :as help]))

(defui ^:once LedgerItem
  static om/Ident
  (ident [this props] [:ledger-item/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :ledger-item/type :ledger-item/name :ledger-item/amount])
  Object
  (render [this]
    (let [{:keys [db/id ledger-item/type ledger-item/name ledger-item/amount]} (om/props this)
          onDelete (om/get-computed this :onDelete)]
      (dom/li nil
              (dom/h5 nil (str type ", " name ", " amount)
                      (dom/button #js {:onClick #(df/refresh! this)} "Refresh")
                      (dom/button #js {:onClick #(onDelete id)} "x"))))))

(def ui-ledger-item (om/factory LedgerItem {:keyfn :db/id}))

(defui ^:once LedgerItemList
  static om/Ident
  (ident [this props] [:ledger-item-list/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :ledger-item-list/label {:ledger-item-list/items (om/get-query LedgerItem)}])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id label]}]
    {:db/id                  id
     :ledger-item-list/label label
     :ledger-item-list/items []})
  Object
  (render [this]
    (let [{:keys [db/id ledger-item-list/label ledger-item-list/items]} (om/props this)
          delete-ledger-item (fn [ledger-item-id]
                               (u/log (str label "asked to delete" name))
                               (om/transact! this `[(ops/delete-ledger-item {:list-id ~id :ledger-item-id ~ledger-item-id})]))]
      (dom/div nil
               (dom/h4 nil label)
               (dom/ul nil
                       (map #(ui-ledger-item (om/computed % {:onDelete delete-ledger-item})) items))))))

(def ui-ledger-item-list (om/factory LedgerItemList))

;;
;; Important for data fetching the meta-data for a user/orgs from the server.
;; Note that on login the full list of organisations for that individual will come
;; through. When getting potential data the app already has an organisation.
;;
(defui ^:once PotentialData
  static om/Ident
  (ident [this props] [:potential-data/by-id p/POTENTIAL_DATA])
  static om/IQuery
  (query [this] [:potential-data/period-type :potential-data/commencing-period
                 :potential-data/latest-period :potential-data/possible-reports]))

(defui ^:once UserRequestForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id          id
                        :potential-data {:potential-data/period-type       :period-type/unknown
                                         :potential-data/latest-period     {:period/quarter  :q1
                                                                            :period/tax-year 2000}
                                         :potential-data/commencing-period {:period/quarter  :q1
                                                                            :period/tax-year 2000}}}))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     ;; Here hard-coding what will come in at login time
                     (f/dropdown-input :request/organisation
                                       [(f/option :seaweed "Seaweed Software Pty Ltd")
                                        (f/option :croquet "Croquet Club")]
                                       :default-value :seaweed)
                     ;; These options are put to something else on reload. I'd rather have them empty,
                     ;; but it seems Untangled doesn't allow that
                     (f/dropdown-input :request/year [(f/option :not-yet-1 "Not yet loaded 1")])
                     (f/dropdown-input :request/period [(f/option :not-yet-2 "Not yet loaded 2")])
                     (f/dropdown-input :request/report [(f/option :not-yet-3 "Not yet loaded 3")])])
  static om/Ident
  (ident [_ props] [:user-request/by-id (:db/id props)])
  static om/IQuery
  (query [_] [:db/id :request/organisation :request/year :request/period :request/report
              {:potential-data (om/get-query PotentialData)} f/form-root-key f/form-key])
  Object
  (render [this]
    (let [{:keys [potential-data request/organisation request/year request/period request/report] :as form} (om/props this)
          {:keys [potential-data/period-type]} potential-data
          period-label (condp = period-type
                         :period-type/quarterly "Quarter"
                         :period-type/monthly "Month"
                         :period-type/unknown "Unknown"
                         nil "Unknown")]
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this form :request/organisation "Organisation"
                                    {:onChange (fn [evt]
                                                 (om/transact! this `[(cljs-ops/touch-report)])
                                                 (let [new-value (js->clj (.. evt -target -value))]
                                                   (df/load this :my-potential-data PotentialData
                                                            {:refresh       [[:user-request/by-id p/USER_REQUEST_FORM]]
                                                             :post-mutation `cljs-ops/potential-data
                                                             :params        {:request/organisation new-value}
                                                             })))})
               (fh/field-with-label this form :request/year "Year"
                                    {:onChange (fn [evt] (om/transact! this `[(cljs-ops/touch-report) (cljs-ops/year-changed)]))})
               (fh/field-with-label this form :request/period period-label
                                    {:onChange (fn [evt] (om/transact! this `[(cljs-ops/touch-report)]))})
               (fh/field-with-label this form :request/report "Report"
                                    {:onChange (fn [evt] (om/transact! this `[(cljs-ops/touch-report)]))})
               (dom/button #js {:onClick (fn [_]
                                           (om/transact! this `[(cljs-ops/touch-report)])
                                           (df/load this
                                                    :my-selected-items LedgerItem
                                                    {:target        help/report-items-whereabouts
                                                     :params        {:request/organisation organisation
                                                                     :request/year         year
                                                                     :request/period       period
                                                                     :request/report       report}
                                                     :post-mutation `cljs-ops/post-report
                                                     }))}
                           "Execute Report")))))

(def ui-user-request-form (om/factory UserRequestForm))

(defui ^:once Root
  static om/IQuery
  (query [this] [:ui/react-key
                 {:root/user-request (om/get-query UserRequestForm)}
                 {:root/selected-items (om/get-query LedgerItemList)}])
  static
  uc/InitialAppState
  (initial-state [c params]
    {:root/selected-items
     (uc/get-initial-state LedgerItemList
                           {:id p/LEDGER_ITEMS_LIST :label help/report-placeholder})
     :root/user-request
     (uc/get-initial-state UserRequestForm
                           {:id p/USER_REQUEST_FORM :potential-data {}})})
  Object
  (render [this]
    (let [{:keys [ui/react-key root/potential-data root/user-request root/selected-items]} (om/props this)]
      (dom/div #js {:key react-key}
               (ui-user-request-form user-request)
               (ui-ledger-item-list selected-items)))))

(defonce app-1 (atom (uc/new-untangled-client
                       :networking {:remote (net/make-untangled-network
                                              "/api"
                                              :global-error-callback (constantly nil))}
                       :started-callback (fn [app]
                                           (df/load app :my-potential-data PotentialData
                                                    {:refresh       [[:user-request/by-id p/USER_REQUEST_FORM]]
                                                     :post-mutation `cljs-ops/potential-data
                                                     :params        {:request/organisation :seaweed}
                                                     })))))
