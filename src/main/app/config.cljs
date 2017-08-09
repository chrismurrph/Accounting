(ns app.config
  (:require [om.dom :as dom]
            [om.next :as om]
            [fulcro.client.core :as uc]
            [om.next :as om :refer [defui]]
            [fulcro.client.data-fetch :as df]
            [app.cljs-operations :as cljs-ops]
            [app.panels :as p]
            [app.domain-ui-helpers :as help]))

(defui ^:once ConfigData
  static om/Ident
  (ident [this props] [:config-data/by-id p/CONFIG_DATA])
  static om/IQuery
  (query [this] [:config-data/ledger-accounts :config-data/bank-accounts]))

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

(defn load-config-data [comp new-org-value]
  (assert (keyword? new-org-value))
  (df/load comp :my-config-data ConfigData
           {:target help/banking-form-config-data-whereabouts
            :params {:request/organisation new-org-value}
            ;:post-mutation `cljs-ops/config-data
            }))
