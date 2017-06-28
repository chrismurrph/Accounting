(ns app.banking
  (:require [om.dom :as dom]
            [om.next :as om]
            [untangled.client.core :as uc]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]))

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
