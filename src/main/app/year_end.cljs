(ns app.year-end
  (:require [om.dom :as dom]
            [om.next :as om]
            [fulcro.client.core :as uc]
            [om.next :as om :refer [defui]]
            [fulcro.client.data-fetch :as df]))

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
