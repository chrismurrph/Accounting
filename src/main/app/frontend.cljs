(ns app.frontend
  (:require [untangled.client.core :as uc]
            [om.dom :as dom]
            [app.operations :as ops]
            [app.cljs-operations :as cljs-ops]
            [app.panels :as p]
            [app.banking :as banking]
            [app.config :as config]
            [app.year-end :as year-end]
            [app.bookkeeping :as bookkeeping]
            [app.domain-ui-helpers :as domain]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [untangled.client.network :as net]
            [untangled.ui.forms :as f]
            [app.util :as u]
            [app.forms-helpers :as fh]
            [app.domain-ui-helpers :as help]
            [untangled.client.routing :as r :refer-macros [defrouter]]))

(defrouter TopRouter :top-router
           (ident [this props] [(:page props) :top])
           :bookkeeping bookkeeping/Bookkeeping
           :banking banking/Banking
           :config config/Config
           :year-end year-end/YearEnd)
(def ui-top (om/factory TopRouter))

(def routing-tree
  {:banking     [(r/router-instruction :top-router [:banking :top])]
   :bookkeeping [(r/router-instruction :top-router [:bookkeeping :top])]
   :year-end    [(r/router-instruction :top-router [:year-end :top])]
   :config      [(r/router-instruction :top-router [:config :top])]})

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
               (dom/br nil) (dom/label nil "")
               #_(dom/label nil (str "Selected: " (-> top-router :current-route first second)))
               (dom/br nil)
               (ui-top top-router)))))

(defui ^:once Neighborhood
  static om/Ident
  (ident [_ props] [:neighborhood/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :neighborhood/name])
  Object
  (render [this]
    (let [{:keys [name]} (om/props this)])
    (dom/tr nil
            (dom/td #js {:className "col-md-2"} name))))

(def ui-neighborhood (om/factory Neighborhood {:keyfn :db/id}))

;;
;; Only used for fetching data. The ident not really needed for that as we loading directly into
;; :district/by-id.
;; Notice the reverse query needed by Datomic
;;
(defui ^:once District
  static om/Ident
  (ident [_ props] [:district/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :district/name {:neighborhood/_district (om/get-query Neighborhood)}]))

(defn load-neighborhoods [comp name]
  (assert name (str "Need a district name to load"))
  (df/load comp :district/by-id District
           {
            :params        {:district-name name}
            :post-mutation `cljs-ops/target-neighborhoods
            :refresh       [[:results-list/by-id 'RESULTS_LIST_PANEL]]
            }))

(defui ^:once TestingResultsList
  static om/Ident
  (ident [_ props] [:results-list/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :district/name {:neighborhoods (om/get-query Neighborhood)}])
  static
  uc/InitialAppState
  (initial-state [c {:keys [id]}] {:db/id id :neighborhoods []})
  Object
  (render [this]
    (let [{:keys [district/name neighborhoods]} (om/props this)]
      (dom/div nil
               ;(dom/label nil (str "District: " name ", " (count _district)))
               (dom/table #js {:className "table table-bordered table-sm table-hover"}
                          (dom/tbody nil (map ui-neighborhood neighborhoods)))))))

(def ui-results-list (om/factory TestingResultsList))

(defui ^:once TestingRoot
  static om/IQuery
  (query [this] [:ui/react-key
                 {:root/results-list (om/get-query TestingResultsList)}])
  static
  uc/InitialAppState
  (initial-state [c params]
    {:root/results-list (uc/get-initial-state TestingResultsList {:id 'RESULTS_LIST_PANEL})})
  Object
  (render [this]
    (let [{:keys [ui/react-key root/results-list]} (om/props this)]
      (dom/div #js {:key react-key}
               (dom/button #js {:onClick #(load-neighborhoods this "Northwest")} (str "Query"))
               #_(dom/label nil "At Root")
               (ui-results-list results-list)))))

(defonce app-2 (atom (uc/new-untangled-client
                       :networking {:remote (net/make-untangled-network
                                              "/api"
                                              :global-error-callback (constantly nil))}
                       :started-callback (fn [app]
                                           ))))

(defonce app-1 (atom (uc/new-untangled-client
                       :networking {:remote (net/make-untangled-network
                                              "/api"
                                              :global-error-callback (constantly nil))}
                       :started-callback (fn [app]
                                           (bookkeeping/load-potential-data app :seaweed)
                                           (banking/load-unruly-bank-statement-line app)
                                           (config/load-config-data app :seaweed)))))
