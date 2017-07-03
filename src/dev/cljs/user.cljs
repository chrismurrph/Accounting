(ns cljs.user
  (:require
    [app.frontend :refer [app-2 TestingRoot]]
    ;[app.ui-helpers :as help]
    [untangled.client.core :as uc]
    [untangled.ui.forms :as f]))

(defn refresh [] (swap! app-2 uc/mount TestingRoot "app-2"))

(refresh)

(defn dump
  [& keys]
  (let [state-map @(om.next/app-state (-> app-2 deref :reconciler))
        data-of-interest (if (seq keys)
                           (get-in state-map keys)
                           state-map)]
    data-of-interest))

;;
;; Used to show before and after of a simple assoc-in change
;; If it works use `assoc-in` in a real mutation
;;
(defn dump-after [get-there replace-with]
  (let [st @(om.next/app-state (-> app-2 deref :reconciler))
        prior (get-in st get-there)
        ]
    (when prior
      [prior (-> st
                 (assoc-in get-there replace-with)
                 (get-in get-there))])))
