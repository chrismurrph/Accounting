(ns app.cljs-operations
  (:require
    [untangled.client.mutations :as m :refer [defmutation]]
    [om.next :as om]
    [app.panels :as p]
    [app.domain-ui-helpers :as help]
    [app.util :as u]
    [untangled.ui.forms :as f]
    [app.forms-helpers :as fh]))

;;
;; Only when the report is done do we show its title properly. Consider going from grayed out to black.
;;
(defmutation post-report [no-params]
             (action [{:keys [state]}]
                     (swap! state #(-> %
                                       help/set-report-title))))

;;
;; Touch any of the inputs and the report is no longer valid, so remove it from the screen
;;
(defmutation touch-report [no-params]
             (action [{:keys [state]}]
                     (swap! state help/blank-out-report)))

;;
;; Assumes that state has already been changed, so that year-field-whereabouts has what the user has just chosen
;; Changes the period options to those that are possible, to those that reflect the data available
;;
(defmutation year-changed [no-params]
             (action [{:keys [state]}]
                     (let [st @state
                           ident [:potential-data/by-id p/POTENTIAL_DATA]
                           potential-data (get-in st ident)
                           selected-year (get-in st help/year-field-whereabouts)
                           [selected-period period-options] (help/periods-options-generator potential-data selected-year)
                           ]
                       (swap! state #(-> %
                                         (help/period-dropdown-rebuilder selected-period period-options))))))

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
                                         help/blank-out-report
                                         (help/year-dropdown-rebuilder selected-year year-options)
                                         (help/period-dropdown-rebuilder selected-period period-options)
                                         (help/report-dropdown-rebuilder selected-report report-options)
                                         (dissoc :my-potential-data))))))
