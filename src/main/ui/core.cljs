(ns ui.core
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]))

(def init-data
  {:list/one [{:name "John" :points 0}
              {:name "Mary" :points 0}
              {:name "Bob" :points 0}]
   :root-view [{:list/one 2000}]})

;; -----------------------------------------------------------------------------
;; Parsing

(defmulti read om/dispatch)

(defn get-people [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read :list/one
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})

(defmethod read :root-view
  [{:keys [state query] :as env} k params]
  (let [st @state]
    {:value (om/db->tree query (get st k) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'points/increment
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
            [:person/by-name name :points]
            inc))})

;; -----------------------------------------------------------------------------
;; Components

(defui Person
  static om/Ident
  (ident [this {:keys [name]}] [:person/by-name name])
  static om/IQuery
  (query [this] '[:name :points :age])
  Object
  (render [this]
    (let [{:keys [points name] :as props} (om/props this)]
      (dom/li nil
              (dom/label nil (str name ", points: " points))
              (dom/button
                #js {:onClick
                     (fn [e]
                       (om/transact! this
                                     `[(points/increment ~props)]))}
                "+")))))

(def person (om/factory Person {:keyfn :name}))

(defui ListView
  ;static om/Ident
  ;(ident [this {:keys [_]}] [:list-view/by-id 2000])
  Object
  (render [this]
    (let [list (om/props this)]
      (apply dom/ul nil
             (map person list)))))
(def list-view (om/factory ListView {:key-fn ffirst}))

;;
;; As there is only one RootView it needs to be put into app state
;;
(defui RootView
  static om/Ident
  (ident [this {:keys [_]}] [:root-view/by-id 1000])
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Person)]
      `[{:list/one ~subquery}]))
  Object
  (render [this]
    (let [{:keys [list/one]} (om/props this)]
      (apply dom/div nil
             [
              (dom/h2 nil "List A")
              (list-view one)
              ]))))

(def rootview (om/factory RootView))

;; wrapping the Root in another root (this fails)

(defui AnotherRoot
  static om/IQuery
  (query [this] [{:root-view (om/get-query RootView)}
                 {:list/one (om/get-query Person)}])
  Object
  (render
    [this]
    (let [{:keys [root-view]} (om/props this)]
      (dom/div nil
               (rootview root-view)))))

(def reconciler
  (om/reconciler
    {:state  init-data
     :parser (om/parser {:read read :mutate mutate})}))

#_(om/add-root! reconciler RootView (gdom/getElement "app"))
