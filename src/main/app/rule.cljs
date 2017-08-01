(ns app.rule
  (:require [fulcro.client.core :as uc]
            [om.next :as om :refer [defui]]
            [fulcro.ui.forms :as f]
            [om.dom :as dom]
            [app.forms-helpers :as fh]
            [app.cljs-operations :as cljs-ops]
            [app.operations :as ops]
            [app.domain-ui-helpers :as help]
            [app.om-helpers :as oh]
            [cljc.utils :as us]
            [app.time :as t]
            [cljs-time.coerce :as c]))

(defui ^:once TimeSlot
  static om/IQuery
  (query [this] [:time-slot/start-at :time-slot/end-at])
  Object
  (render [this]
    (let [{:keys [time-slot/start-at time-slot/end-at]} (om/props this)]
      (dom/label nil (str (t/show start-at) ", " (t/show end-at))))))
(def ui-timeslot (om/factory TimeSlot {:keyfn :time-slot/start-at}))

(defui ^:once ValidatedConditionForm
  static uc/InitialAppState
  (initial-state [this params] (f/build-form this (or params {})))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/dropdown-input :condition/field [(f/option :out/desc "Description")
                                                         (f/option :out/amount "Amount")])
                     (f/dropdown-input :condition/predicate [(f/option :starts-with "Starts with")
                                                             (f/option :ends-with "Ends with")
                                                             (f/option :equals "Equals")])
                     (f/text-input :condition/subject)])
  static om/IQuery
  (query [this] [:db/id :condition/field :condition/predicate :condition/subject f/form-key])
  static om/Ident
  (ident [this props] [:condition/by-id (:db/id props)])
  Object
  (render [this]
    (let [form (om/props this)]
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this form :condition/field "Field:")
               (fh/field-with-label this form :condition/predicate "Predicate:")
               (fh/field-with-label this form :condition/subject "Value:")))))
(def ui-vcondition-form (om/factory ValidatedConditionForm {:keyfn :db/id}))

(defn button-group [rule-unselected-f add-condition-f this props]
  (assert (or (nil? rule-unselected-f) (and (-> rule-unselected-f boolean? not) (fn? rule-unselected-f))))
  (dom/div #js {:className "button-group"}
           (when rule-unselected-f
             (dom/button #js {:className "btn btn-default"
                              :onClick rule-unselected-f}
                         "Back"))
           (dom/button #js {:className "btn btn-default"
                            :onClick   add-condition-f}
                       "Add Condition")
           (dom/button #js {:className "btn btn-default", :disabled (not (f/dirty? props))
                            :onClick   #(om/transact! this `[(f/validate-form {:form-id ~(f/form-ident props)})
                                                             (ops/commit-to-within-entity
                                                               {:form   ~props
                                                                :remote true
                                                                :within {:content-holder-key    :organisation/rules
                                                                         :attribute             :organisation/key
                                                                         :attribute-value-value :seaweed
                                                                         :master-class          :rule/by-id
                                                                         :detail-class          :condition/by-id}})])}
                       "Submit")))

(defui ^:once RuleFConditionF
  static uc/InitialAppState
  (initial-state [this params] (f/build-form this (or params {})))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/subform-element :rule/conditions ValidatedConditionForm :many)
                     (f/dropdown-input :rule/logic-operator help/logic-options
                                       :default-value :single)])
  static om/IQuery
  ; NOTE: f/form-root-key so that sub-forms will trigger render here
  (query [this] [f/form-root-key f/form-key
                 :db/id
                 :rule/logic-operator
                 {:rule/conditions (om/get-query ValidatedConditionForm)}])
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  Object
  (render [this]
    (let [{:keys [rule/conditions] :as props} (om/props this)
          {:keys [rule-unselected]} (om/get-computed this)]
      (assert (fh/form? props) (str "props is not a form: " (keys props)))
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this props :rule/logic-operator "Logic")
               (dom/div nil
                        (mapv ui-vcondition-form conditions))
               (when (f/valid? props)
                 (dom/div nil "All fields have had been validated, and are valid"))
               (button-group rule-unselected nil this props)))))
(def ui-rule-f-condition-f (om/factory RuleFConditionF))

(defui ^:once ConditionRow
  static om/IQuery
  (query [this] [:db/id :condition/field :condition/predicate :condition/subject])
  static om/Ident
  (ident [this props] [:condition/by-id (:db/id props)])
  Object
  (render [this]
    (let [{:keys [db/id condition/field condition/predicate condition/subject]} (om/props this)
          field-display (us/kw->string field)
          predicate-display (us/kw->string predicate)]
      (dom/tr nil
              (dom/td #js {:className "col-md-2"} field-display)
              (dom/td #js {:className "col-md-2"} predicate-display)
              (dom/td #js {:className "col-md-2"} subject)))))
(def ui-condition-row (om/factory ConditionRow {:keyfn :db/id}))

;;
;; Idea here that conditions not be made forms until needed.
;; Can you have a form that has a many that are not themselves forms?
;; Research and ask...
;;

(defui ^:once RuleF
  static uc/InitialAppState
  (initial-state [this params] (f/build-form this (or params {})))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/subform-element :rule/upserting-condition ValidatedConditionForm :one)
                     (f/dropdown-input :rule/logic-operator help/logic-options
                                       :default-value :single)])
  static om/IQuery
  ; NOTE: f/form-root-key so that sub-forms will trigger render here
  (query [this] [f/form-root-key f/form-key
                 :db/id
                 :rule/logic-operator
                 {:rule/upserting-condition (om/get-query ValidatedConditionForm)}
                 {:rule/conditions (om/get-query ConditionRow)}])
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  Object
  (add-condition [this props]
    (om/transact! this
                  `[(cljs-ops/add-condition-2
                      ~{:id             (oh/make-temp-id "add-condition in rule")
                        :rule           (:db/id props)
                        :condition-form ValidatedConditionForm})]))
  (render [this]
    (let [{:keys [rule/conditions rule/upserting-condition] :as props} (om/props this)
          {:keys [rule-unselected]} (om/get-computed this)]
      (assert (fh/form? props) (str "props is not a form: " (keys props)))
      (dom/div #js {:className "form-horizontal"}
               (button-group rule-unselected #(.add-condition this props) this props)
               (fh/field-with-label this props :rule/logic-operator "Logic")
               #_(when (f/valid? props)
                 (dom/div nil "All fields have had been validated, and are valid"))
               (when upserting-condition
                 (ui-vcondition-form upserting-condition))
               (dom/table #js {:className "table table-bordered table-sm table-hover"}
                          (dom/tbody nil (map ui-condition-row conditions)))))))
(def ui-rule-f (om/factory RuleF))

(defui ^:once Rule
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/rule-num :rule/source-bank :rule/target-account
                 :rule/logic-operator {:rule/conditions (om/get-query ConditionRow)}
                 :rule/on-dates {:rule/time-slot (om/get-query TimeSlot)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [db/id rule/permanent? rule/rule-num rule/source-bank rule/target-account
                  rule/logic-operator rule/conditions]} props
          {:keys [rule-unselected]} (om/get-computed this)]
      (dom/div nil
               (button-group rule-unselected nil this props)
               (dom/table #js {:className "table table-bordered table-sm table-hover"}
                          (dom/tbody nil (map ui-condition-row conditions)))))))
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
                 {:rule/conditions (om/get-query ConditionRow)}
                 :rule/on-dates {:rule/time-slot (om/get-query TimeSlot)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [db/id rule/permanent? rule/source-bank rule/target-account rule/logic-operator rule/conditions]} props
          {:keys [rule-selected]} (om/get-computed this)
          ;; permanent? is either true or nil
          permanent-display (if permanent? "Permanent" (display-on-dates props))
          ;source-bank-display (us/kw->string source-bank)
          ;target-account-display (us/kw->string target-account)
          logic-operator-display (if (= :single logic-operator) "" (us/kw->string logic-operator))
          ;num-conds (str (count conditions))
          ]
      (dom/tr #js {:onClick #(rule-selected id)}
              (dom/td #js {:className "col-md-2"} permanent-display)
              ;(dom/td #js {:className "col-md-2"} source-bank-display)
              ;(dom/td #js {:className "col-md-2"} target-account-display)
              (dom/td #js {:className "col-md-1"} logic-operator-display)
              ;(dom/td #js {:className "col-md-2"} num-conds)
              (dom/td #js {:className "col-md-12"} (dom/table #js {:className "table table-bordered table-sm table-hover"}
                                                              (dom/tbody nil (map ui-condition-row conditions))))))))
(def ui-rule-row (om/factory RuleRow {:keyfn :db/id}))


