(ns cljs.user
  (:require
    [app.frontend :refer [app Root]]
    [ui.core :refer [RootView AnotherRoot] :as experi]
    [fulcro.client.core :as uc]
    [fulcro.ui.forms :as f]
    [om.next :as om]
    [goog.dom :as gdom]))

(defn refresh []
  (swap! app uc/mount Root "app"))

;; Alternative if you wanted straight Om.Next
#_(defn refresh []
  (om/add-root! experi/reconciler AnotherRoot (gdom/getElement "app")))

(refresh)

(defn dump
  [& keys]
  (let [state-map @(om.next/app-state (-> app deref :reconciler))
        data-of-interest (if (seq keys)
                           (get-in state-map keys)
                           state-map)]
    data-of-interest))

;; Alternative if you wanted straight Om.Next
#_(defn dump
  [& keys]
  (let [state-map @(om.next/app-state experi/reconciler)]
    state-map))

;;
;; Used to show before and after of a simple assoc-in change
;; If it works use `assoc-in` in a real mutation
;;
(defn dump-after [get-there replace-with]
  (let [st @(om.next/app-state (-> app deref :reconciler))
        prior (get-in st get-there)
        ]
    (when prior
      [prior (-> st
                 (assoc-in get-there replace-with)
                 (get-in get-there))])))
