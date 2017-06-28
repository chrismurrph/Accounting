(ns app.config
  (:require [om.dom :as dom]
            [om.next :as om]
            [untangled.client.core :as uc]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]))

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
  (df/load comp :my-config-data ConfigData))
