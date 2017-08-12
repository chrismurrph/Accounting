(ns app.rule
  (:require [fulcro.client.core :as uc]
            [fulcro.client.mutations :as fcm]
            [om.next :as om :refer [defui]]
            [fulcro.ui.forms :as f]
            [fulcro.ui.bootstrap3 :as b]
            [om.dom :as dom]
            [app.forms-helpers :as fh]
            [app.cljs-operations :as cljs-ops]
            [app.operations :as ops]
            [app.domain-ui-helpers :as help]
            [app.om-helpers :as oh]
            [app.condition :as con]
            [cljc.utils :as us]
            [app.time :as t]
            [cljs-time.coerce :as c]
            [app.panels :as p]))

(defn make-fns-map [rule-unselected-f add-condition-f remove-condition-f]
  {:rule-unselected-f  rule-unselected-f
   :add-condition-f    add-condition-f
   :remove-condition-f remove-condition-f})

(defui ^:once ButtonGroup
  ;static uc/InitialAppState
  ;(initial-state [this params] {:debug-from "unknown"})
  static om/IQuery
  (query [this] [:debug-from])
  Object
  (render [this]
    (let [{:keys [debug-from] :as props} (om/props this)
          {:keys [rule-unselected-f add-condition-f remove-condition-f]} (om/get-computed this)]
      (assert debug-from (us/assert-str "debug-from" props))
      (assert rule-unselected-f (us/assert-str "rule-unselected-f" (om/get-computed this)))
      (b/button-group {}
                      (b/button {:onClick rule-unselected-f}
                                "Back")
                      (b/button {:onClick add-condition-f}
                                "Add")
                      (comment "remove s/be on row itself" (when remove-condition-f
                                                             (b/button {:onClick remove-condition-f}
                                                                       "Remove")))
                      (b/button {:disabled (not (f/dirty? props))
                                 :onClick  #(om/transact! this `[(f/validate-form {:form-id ~(f/form-ident props)})
                                                                 (ops/commit-to-within-entity
                                                                   {:form   ~props
                                                                    :remote true
                                                                    :within {:content-holder-key    :organisation/rules
                                                                             :attribute             :organisation/key
                                                                             :attribute-value-value :seaweed
                                                                             :content-holder-class  :organisation/by-id
                                                                             :master-class          :rule/by-id}})])}
                                "Submit")))))
(def buttongroup-ui (om/factory ButtonGroup))

(defui ^:once RuleF
  ;static uc/InitialAppState
  ;(initial-state [this params] (f/build-form this {:rule/button-group (uc/get-initial-state ButtonGroup {})}))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/subform-element :rule/conditions con/MaybeFormConditionRow :many)
                     (f/dropdown-input :rule/logic-operator help/logic-options
                                       :default-value :single)])
  static om/IQuery
  (query [this] [f/form-root-key f/form-key
                 :db/id
                 :rule/logic-operator
                 {:rule/conditions (om/get-query con/MaybeFormConditionRow)}
                 :ui/editing?
                 {:rule/button-group (om/get-query ButtonGroup)}])
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  Object
  (condition-selected-f [this props child-id]
    (om/transact! this `[(cljs-ops/select-condition {:selected-ident [:condition/by-id ~child-id]
                                                     :master-join    ~(conj (om/ident this props) :rule/conditions)})]))
  (condition-unselect [this]
    (om/transact! this `[(cljs-ops/un-select {:details-at   ~(conj (om/get-ident this) :rule/conditions)
                                              :detail-class :condition/by-id}) [:banking-form/by-id ~p/BANKING_FORM]]))
  (add-condition [this props]
    (om/transact! this
                  `[(cljs-ops/add-condition
                      ~{:id             (oh/make-temp-id "add-condition in rule")
                        :rule           (:db/id props)
                        :condition-form con/MaybeFormConditionRow})]))
  (render [this]
    (let [{:keys [rule/conditions ui/editing? rule/button-group] :as props} (om/props this)]
      (println "Doing for button-group:" button-group)
      (assert (fh/form? props) (str "props is not a form: " (keys props)))
      (dom/div #js {:className "form-horizontal"}
               (buttongroup-ui (om/computed button-group
                                            (make-fns-map #(.condition-unselect this) #(.add-condition this props) nil)))
               (fh/field-with-label this props :rule/logic-operator "Logic")
               (b/table {:className "table table-bordered table-sm table-hover"}
                        (dom/tbody nil (map
                                         #(con/ui-maybe-form-condition-row
                                            (om/computed % {:condition-selected-f (fn [id] (.condition-selected-f this props id))}))
                                         conditions)))))))
(def ui-rule-f (om/factory RuleF))

(defui ^:once RuleFConditionF
  ;static uc/InitialAppState
  ;;; Only put in :debug-from to stop spec complaining
  ;(initial-state [this params] (f/build-form this (or (merge params {:button-group {:debug-from :a}}) {})))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/subform-element :rule/conditions con/ValidatedConditionForm :many)
                     (f/dropdown-input :rule/logic-operator help/logic-options
                                       :default-value :single)])
  static om/IQuery
  (query [this] [f/form-root-key f/form-key
                 :db/id
                 :rule/logic-operator
                 {:rule/conditions (om/get-query con/ValidatedConditionForm)}
                 {:rule/button-group (om/get-query ButtonGroup)}])
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  Object
  (render [this]
    (let [{:keys [rule/conditions rule/button-group] :as props} (om/props this)
          {:keys [rule-unselected]} (om/get-computed this)]
      (assert (fh/form? props) (str "props is not a form: " (keys props)))
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this props :rule/logic-operator "Logic")
               (dom/div nil
                        (mapv con/ui-vcondition-form conditions))
               (when (f/valid? props)
                 (dom/div nil "All fields have had been validated, and are valid"))
               (buttongroup-ui (om/computed button-group (make-fns-map rule-unselected nil nil)))))))
(def ui-rule-f-condition-f (om/factory RuleFConditionF))

(defui ^:once TimeSlot
  static om/IQuery
  (query [this] [:time-slot/start-at :time-slot/end-at])
  Object
  (render [this]
    (let [{:keys [time-slot/start-at time-slot/end-at]} (om/props this)]
      (dom/label nil (str (t/show start-at) ", " (t/show end-at))))))
#_(def ui-timeslot (om/factory TimeSlot {:keyfn :time-slot/start-at}))

(defui ^:once Rule
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/rule-num :rule/source-bank :rule/target-account
                 :rule/logic-operator {:rule/conditions (om/get-query con/ConditionRow)}
                 {:rule/button-group (om/get-query ButtonGroup)}
                 :rule/on-dates {:rule/time-slot (om/get-query TimeSlot)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [db/id rule/permanent? rule/rule-num rule/source-bank rule/target-account
                  rule/logic-operator rule/conditions rule/button-group]} props
          {:keys [rule-unselected]} (om/get-computed this)]
      (dom/div nil
               (buttongroup-ui (om/computed button-group (make-fns-map rule-unselected nil nil)))
               (b/table #js {:className "table table-bordered table-sm table-hover"}
                        (dom/tbody nil (map con/ui-condition-row conditions)))))))
(def ui-rule (om/factory Rule {:keyfn :db/id}))

;;
;; All the 'just for this period' goes through here as well, but ends up as just ""
;;
(defn display-on-dates [{:keys [rule/on-dates rule/time-slot]}]
  (assert (or (nil? time-slot) (zero? (count time-slot))))
  (let [on-dates-str (cond-> ""
                             (pos? (count on-dates))
                             (str "on: "
                                  (apply str (interpose ", " (map (comp t/show c/from-date) on-dates)))))]
    (str on-dates-str)))

(defui ^:once RuleRow
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/source-bank :rule/target-account :rule/logic-operator
                 {:rule/conditions (om/get-query con/ConditionRow)}
                 :rule/on-dates {:rule/time-slot (om/get-query TimeSlot)}
                 {:rule/button-group (om/get-query ButtonGroup)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [db/id rule/permanent? rule/source-bank rule/target-account
                  rule/logic-operator rule/conditions rule/button-group]} props
          {:keys [rule-selected-f]} (om/get-computed this)
          ;; permanent? is either true or nil
          permanent-display (if permanent? "Permanent" (display-on-dates props))
          ;source-bank-display (us/kw->string source-bank)
          ;target-account-display (us/kw->string target-account)
          logic-operator-display (if (= :single logic-operator) "" (us/kw->string logic-operator))
          ;num-conds (str (count conditions))
          ]
      (dom/tr #js {:onClick #(rule-selected-f id)}
              (dom/td #js {:className "col-md-1"} id)
              (dom/td #js {:className "col-md-2"} permanent-display)
              ;(dom/td #js {:className "col-md-2"} source-bank-display)
              ;(dom/td #js {:className "col-md-2"} target-account-display)
              (dom/td #js {:className "col-md-1"} logic-operator-display)
              ;(dom/td #js {:className "col-md-2"} num-conds)
              (dom/td #js {:className "col-md-12"} (b/table {:className "table table-bordered table-sm"}
                                                            (dom/tbody nil (map con/ui-condition-row conditions))))))))
(def ui-rule-row (om/factory RuleRow {:keyfn :db/id}))


