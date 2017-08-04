(ns cljs.user
  (:require
    [app.frontend :refer [app Root]]
    [ui.core :refer [RootView AnotherRoot] :as experi]
    [fulcro.client.core :as uc]
    [fulcro.ui.forms :as f]
    [om.next :as om]
    [goog.dom :as gdom]
    [cljs.spec.alpha :as s]
    [cljs.spec.test.alpha :as ts]
    [app.forms-helpers :as fh]
    [app.cljs-operations :as cljs-ops]))

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

(defn my-inc [x]
  (inc x))

(s/fdef my-inc
        :args (s/cat :x number?)
        :ret number?)

(ts/instrument)

(defn x-1 []
  (my-inc "Hi"))

(defn x-2 []
  (app.forms-helpers/fns-over-state {:a :b} [(fn [m] m)]))

(defn x-3 []
  (let [st {}
        nothing-fns (cljs-ops/detail-fns {}
                                         (fh/do-nothing)
                                         st)]))
