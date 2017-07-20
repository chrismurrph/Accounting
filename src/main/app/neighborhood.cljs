(ns app.neighborhood
  (:require [om.dom :as dom]
            [om.next :as om :refer [defui]]
            [fulcro.client.core :as uc]
            [fulcro.client.data-fetch :as df]
            [app.cljs-operations :as cljs-ops]))

;;
;; Just having this means that what comes back from Datomic includes communities
;;
(defui ^:once Community
       static om/Ident
       (ident [_ props] [:community/by-id (:db/id props)])
       static om/IQuery
       (query [this] [:db/id :community/name :community/url])
       Object
       (render [this]
               (let [{:keys [community/name community/url]} (om/props this)]
                 (dom/tr nil
                         (dom/td #js {:className "col-md-2"} name)
                         (dom/td #js {:className "col-md-2"} url)))))

(defui ^:once Neighborhood
       static om/Ident
       (ident [_ props] [:neighborhood/by-id (:db/id props)])
       static om/IQuery
       (query [this] [:db/id :neighborhood/name {:community/_neighborhood (om/get-query Community)}])
       Object
       (render [this]
               (let [{:keys [neighborhood/name]} (om/props this)]
                 (dom/tr nil
                         (dom/td #js {:className "col-md-2"} name)))))

(def ui-neighborhood (om/factory Neighborhood {:keyfn :db/id}))

;;
;; Only used for fetching data. The ident not really needed for that as we loading directly into
;; :district-query.
;; Notice the reverse query needed by Datomic
;; Note there is nothing tacky about this as District is not in the UI.
;;
(defui ^:once DistrictFetcher
       static om/Ident
       (ident [_ props] [:district-query (:db/id props)])
       static om/IQuery
       (query [this] [:db/id :district/name {:neighborhood/_district (om/get-query Neighborhood)}]))

(defn load-neighborhoods [comp name]
  (assert name (str "Need a district name to load"))
  (df/load comp :district-query DistrictFetcher
           {
            :params        {:district-name name}
            :post-mutation `cljs-ops/target-neighborhoods
            :refresh       [[:results-list/by-id 'RESULTS_LIST_PANEL]]
            }))

(def hard-coded-district "Northwest")

;;
;; table of all the Neighborhoods of a particular District
;;
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
                          (dom/button #js {:onClick #(load-neighborhoods this hard-coded-district)} (str "Query"))
                          #_(dom/label nil "At Root")
                          (ui-results-list results-list)))))

