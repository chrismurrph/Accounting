(ns app.cljs-operations
  (:require
    [untangled.client.mutations :as m :refer [defmutation]]
    [om.next :as om]
    [app.panels :as p]
    [app.domain-ui-helpers :as help]
    [app.util :as u]
    [untangled.ui.forms :as f]
    [app.forms-helpers :as fh]))

(defn sort-selected-items-by*
  "Sort the idents in the selected-items ledger-item list. Returns the new app-state."
  [state-map field]
  (let [items (get-in state-map [:ledger-item-list/by-id p/LEDGER_ITEMS_LIST :ledger-item-list/items] [])
        selected-items (map (fn [item-ident] (get-in state-map item-ident)) items)
        sorted-selected-items (sort-by field selected-items)
        new-idents (mapv (fn [item] [:ledger-item/by-id (:db/id item)]) sorted-selected-items)]
    ;(println (str "SORTED by " field " -> " new-idents))
    (assoc-in state-map [:ledger-item-list/by-id p/LEDGER_ITEMS_LIST :ledger-item-list/items] new-idents)))

(defmutation sort-items-by-amount [no-params]
             (action [{:keys [state]}]
                     (swap! state sort-selected-items-by* :ledger-item/amount)))

(def year-dropdown-changer
  (fh/dropdown-changer
    help/year-field-whereabouts help/year-options-whereabouts))
(def period-dropdown-changer
  (fh/dropdown-changer
    help/period-field-whereabouts help/period-options-whereabouts))
(def report-dropdown-changer
  (fh/dropdown-changer
    help/report-field-whereabouts help/report-options-whereabouts))

(defmutation sort-items-by-name [no-params]
             (action [{:keys [state]}]
                     (swap! state sort-selected-items-by* :ledger-item/name)))

;;
;; Assumes that state has already been changed, so that year-field-whereabouts has what the user has just chosen
;; Changes the period options to those that are possible, to those that reflect the data available
;;
(defmutation year-changed [no-params]
             (action [{:keys [state]}]
                     (let [st @state
                           ident [:potential-data/by-id p/POTENTIAL_DATA]
                           potential-data (get-in st ident)
                           changed-to-year (get-in st help/year-field-whereabouts)
                           periods (help/range-of-periods changed-to-year potential-data)
                           default-period (last periods)
                           period-options (mapv #(f/option % (help/period-kw->period-name %)) periods)
                           ]
                       (swap! state #(-> %
                                         (period-dropdown-changer default-period period-options))))))

(defmutation potential-data [no-params]
             (action [{:keys [state]}]
                     (let [st @state
                           ident (:my-potential-data st)
                           potential-data (get-in st ident)
                           [selected-year year-options] (help/years-options-generator potential-data nil)
                           [selected-period period-options] (help/periods-options-generator potential-data selected-year)
                           [selected-report report-options] (help/reports-options-generator potential-data nil)
                           ]
                       (u/log-off (str "year: " selected-year))
                       (u/log-off (str "year options: " year-options))
                       (u/log-off (str "period options: " period-options))
                       (swap! state #(-> %
                                         (year-dropdown-changer selected-year year-options)
                                         (period-dropdown-changer selected-period period-options)
                                         (report-dropdown-changer selected-report report-options)
                                         (dissoc :my-potential-data))))))
