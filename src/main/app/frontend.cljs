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
            [app.domain-ui-helpers :as help]
            [untangled.client.routing :as r :refer-macros [defrouter]]
            [goog.string :as gstring]
            [goog.string.format]))

(defui ^:once LedgerItem
  static om/Ident
  (ident [this props] [:ledger-item/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :ledger-item/type :ledger-item/name :ledger-item/amount])
  Object
  (render [this]
    (let [{:keys [db/id ledger-item/type ledger-item/name ledger-item/amount]} (om/props this)
          type-display (and type (subs (str type) 1))
          negative? (= \- (first (str amount)))
          amount-display (str "$" (gstring/format "%.2f" (u/abs amount)))
          name-display (clojure.string/replace name #"-" " ")
          ]
      (dom/tr nil
              (dom/td #js {:className "col-md-2"} name-display)
              (dom/td #js {:style #js {:color (if negative? "red" "")} :className "text-right col-md-1"} amount-display)
              (dom/td #js {:className "col-md-2"} type-display)))))
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
               ;; table-inverse did not work
               ;; table-striped doesn't work well with hover as same colour
               (dom/table #js {:className "table table-bordered table-sm table-hover"}
                          (dom/tbody nil (map #(ui-ledger-item (om/computed % {:onDelete delete-ledger-item})) items)))))))
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

;;
;; Manually executable problems.
;; First is I don't know how to disable a button (do bootstrap class selectors)
;; No other problems - just call this fn whenever quarter or report are changed
;;
(defn execute-report [comp organisation year period report]
  #((om/transact! comp `[(cljs-ops/touch-report)])
    (df/load comp
             :my-selected-items LedgerItem
             {:target        help/report-items-whereabouts
              :params        {:request/organisation organisation
                              :request/year         year
                              :request/period       period
                              :request/report       report}
              :post-mutation `cljs-ops/post-report})))

(defn load-potential-data [comp new-value]
  (assert (keyword? new-value))
  (df/load comp :my-potential-data PotentialData
           {:refresh       [[:user-request/by-id p/USER_REQUEST_FORM]]
            :post-mutation `cljs-ops/potential-data
            :params        {:request/organisation new-value}
            }))

(defui ^:once UserRequestForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id          id
                        :request/manually-executable? true
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
              :request/manually-executable?
              {:potential-data (om/get-query PotentialData)} f/form-root-key f/form-key])
  Object
  (render [this]
    (let [{:keys [potential-data request/organisation request/year request/period
                  request/report request/manually-executable?] :as form} (om/props this)
          {:keys [potential-data/period-type]} potential-data
          period-label (condp = period-type
                         :period-type/quarterly "Quarter"
                         :period-type/monthly "Month"
                         :period-type/unknown "Unknown"
                         nil "Unknown")
          on-className (if manually-executable? "btn btn-primary" "btn disabled")
          on-disabled (if manually-executable? "" "true")]
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this form :request/organisation "Organisation"
                                    {:onChange (fn [evt]
                                                 (om/transact! this `[(cljs-ops/touch-report)])
                                                 (let [new-value (u/keywordize (.. evt -target -value))]
                                                   (load-potential-data this new-value)))})
               (fh/field-with-label this form :request/year "Year"
                                    {:onChange (fn [evt] (om/transact! this `[(cljs-ops/touch-report) (cljs-ops/year-changed)]))})
               (fh/field-with-label this form :request/period period-label
                                    {:onChange (fn [evt] (om/transact! this `[(cljs-ops/touch-report)]))})
               (fh/field-with-label this form :request/report "Report"
                                    {:onChange (fn [evt] (om/transact! this `[(cljs-ops/touch-report)]))})
               (dom/button #js {:className on-className
                                :disabled  on-disabled
                                :onClick   (execute-report this organisation year period report)}
                           (if manually-executable? "Execute Report" "Auto Execute ON"))))))
(def ui-user-request-form (om/factory UserRequestForm))

(defui ^:once Bookkeeping
  static om/IQuery
  (query [this] [:page
                 {:bookkeeping/user-request (om/get-query UserRequestForm)}
                 {:bookkeeping/selected-items (om/get-query LedgerItemList)}])
  static
  uc/InitialAppState
  (initial-state [c params]
    {:page :bookkeeping
     :bookkeeping/selected-items
            (uc/get-initial-state LedgerItemList
                                  {:id p/LEDGER_ITEMS_LIST :label help/report-placeholder})
     :bookkeeping/user-request
            (uc/get-initial-state UserRequestForm
                                  {:id p/USER_REQUEST_FORM :potential-data {}})})
  Object
  (render [this]
    (let [{:keys [bookkeeping/user-request bookkeeping/selected-items]} (om/props this)]
      (dom/div nil
               (ui-user-request-form user-request)
               (ui-ledger-item-list selected-items)))))

(defui ^:once YearEnd
  static om/IQuery
  (query [this] [:page])
  static
  uc/InitialAppState
  (initial-state [c params] {:page :year-end})
  Object
  (render [this]
    (let [{:keys [page]} (om/props this)]
      (dom/div nil (str "I'm Year End: " page)))))

(defui ^:once Banking
  static om/IQuery
  (query [this] [:page])
  static
  uc/InitialAppState
  (initial-state [c params] {:page :banking})
  Object
  (render [this]
    (let [{:keys [page]} (om/props this)]
      (dom/div nil (str "I'm Banking: " page)))))

(defui ^:once Config
  static om/IQuery
  (query [this] [:page])
  static
  uc/InitialAppState
  (initial-state [c params] {:page :config})
  Object
  (render [this]
    (let [{:keys [page]} (om/props this)]
      (dom/div nil (str "I'm Config: " page)))))

(defrouter TopRouter :top-router
           (ident [this props] [(:page props) :top])
           :bookkeeping Bookkeeping
           :banking Banking
           :config Config
           :year-end YearEnd)
(def ui-top (om/factory TopRouter))

(def routing-tree
  {:banking [(r/router-instruction :top-router [:banking :top])]
   :bookkeeping [(r/router-instruction :top-router [:bookkeeping :top])]
   :year-end [(r/router-instruction :top-router [:year-end :top])]
   :config [(r/router-instruction :top-router [:config :top])]})

(defn nav-to [comp kw]
  #(om/transact! comp `[(r/route-to {:handler ~kw})]))

(defn show-selected-kw-hof [selected-kw]
  (fn [kw]
    (if (= kw selected-kw) "red" "")))

(defui ^:once Root
  static om/IQuery
  (query [this] [:ui/react-key
                 {:top-router (om/get-query TopRouter)}])
  static
  uc/InitialAppState
  (initial-state [c params]
    {r/routing-tree-key routing-tree
     :top-router        (uc/get-initial-state TopRouter {})
     })
  Object
  (render [this]
    (let [{:keys [ui/react-key top-router]} (om/props this)
          selected-kw (-> top-router :current-route first second)
          show-selected (show-selected-kw-hof selected-kw)]
      (dom/div #js {:key react-key}
               (dom/br nil)
               (dom/a #js {:style #js {:color (show-selected :bookkeeping)} :onClick (nav-to this :bookkeeping)} "Bookkeeping") " | "
               (dom/a #js {:style #js {:color (show-selected :banking)} :onClick (nav-to this :banking)} "Banking") " | "
               (dom/a #js {:style #js {:color (show-selected :year-end)} :onClick (nav-to this :year-end)} "Year End") " | "
               (dom/a #js {:style #js {:color (show-selected :config)} :onClick (nav-to this :config)} "Config")
               (dom/br nil)(dom/label nil "")
               #_(dom/label nil (str "Selected: " (-> top-router :current-route first second)))
               (dom/br nil)
               (ui-top top-router)))))

(defonce app-1 (atom (uc/new-untangled-client
                       :networking {:remote (net/make-untangled-network
                                              "/api"
                                              :global-error-callback (constantly nil))}
                       :started-callback (fn [app] (load-potential-data app :seaweed)))))
