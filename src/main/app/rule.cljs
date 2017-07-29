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
            [cljc.utils :as us]))

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

(def ui-vcondition-form (om/factory ValidatedConditionForm))

(defn button-group [rule-unselected this props]
  (assert (or (nil? rule-unselected) (and (-> rule-unselected boolean? not) (fn? rule-unselected))))
  (dom/div #js {:className "button-group"}
           (when rule-unselected
             (dom/button #js {:className "btn btn-default"
                              :onClick rule-unselected}
                         "Back"))
           (dom/button #js {:className "btn btn-default"
                            :onClick   #(om/transact! this
                                                      `[(cljs-ops/add-condition
                                                          ~{:id             (oh/make-temp-id "add-condition in rule")
                                                            :rule           (:db/id props)
                                                            :condition-form ValidatedConditionForm})])}
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

(defui ^:once RuleForm
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
    (let [{:keys [rule/conditions] :as props} (om/props this)]
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this props :rule/logic-operator "Logic" {:checkbox-style? true})
               (dom/div nil
                        (mapv ui-vcondition-form conditions))
               (when (f/valid? props)
                 (dom/div nil "All fields have had been validated, and are valid"))
               (button-group nil this props)))))

(def ui-rule-form (om/factory RuleForm))

