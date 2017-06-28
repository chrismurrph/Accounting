(ns app.bookkeeping
  (:require [om.dom :as dom]
            [om.next :as om]
            [untangled.client.core :as uc]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [app.panels :as p]
            [app.domain-ui-helpers :as help]
            [app.forms-helpers :as fh]
            [app.cljs-operations :as cljs-ops]
            [app.util :as u]
            [untangled.ui.forms :as f]
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
               :post-mutation `cljs-ops/post-report
               :params        {:request/organisation organisation
                               :request/year         year
                               :request/period       period
                               :request/report       report}
               :refresh       [:request/manually-executable?]})))

(defn load-potential-data [comp new-org-value]
  (assert (keyword? new-org-value))
  (df/load comp :my-potential-data PotentialData
           {:refresh       [[:user-request/by-id p/USER_REQUEST_FORM]]
            :post-mutation `cljs-ops/potential-data
            :params        {:request/organisation new-org-value}}))

;; Hopefully there will be a decent error message rather than user seeing this
(def initial-potential-data {:potential-data/period-type       :period-type/unknown
                             :potential-data/latest-period     {:period/quarter  :q1
                                                                :period/tax-year 2000}
                             :potential-data/commencing-period {:period/quarter  :q1
                                                                :period/tax-year 2000}})
(defui ^:once UserRequestForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id                        id
                        :request/manually-executable? true
                        :potential-data               initial-potential-data}))
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
          at-className (if manually-executable? "btn btn-primary" "btn disabled")
          at-disabled (if manually-executable? "" "true")]
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this form :request/organisation "Organisation"
                                    {:onChange (fn [evt]
                                                 (om/transact! this `[(cljs-ops/touch-report)])
                                                 (let [new-org-value (u/keywordize (.. evt -target -value))]
                                                   (load-potential-data this new-org-value))
                                                 (om/transact! this `[(cljs-ops/enable-report-execution)]))})
               (fh/field-with-label this form :request/year "Year"
                                    {:onChange (fn [evt]
                                                 (om/transact! this `[(cljs-ops/touch-report) (cljs-ops/year-changed) (cljs-ops/enable-report-execution)]))})
               (fh/field-with-label this form :request/period period-label
                                    {:onChange (fn [evt]
                                                 (om/transact! this `[(cljs-ops/touch-report)])
                                                 ((execute-report this organisation year (u/keywordize (.. evt -target -value)) report)))})
               (fh/field-with-label this form :request/report "Report"
                                    {:onChange (fn [evt]
                                                 (om/transact! this `[(cljs-ops/touch-report)])
                                                 ((execute-report this organisation year period (u/keywordize (.. evt -target -value)))))})
               (dom/button #js {:className at-className
                                :disabled  at-disabled
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