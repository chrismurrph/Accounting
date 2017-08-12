(ns app.condition
  (:require [om.next :as om :refer [defui]]
            [om.dom :as dom]
            [cljc.utils :as us]
            [app.forms-helpers :as fh]
            [fulcro.ui.forms :as f]
            [fulcro.client.core :as uc]
            [fulcro.ui.bootstrap3 :as b]))

(defui ^:once MaybeFormConditionRow
  ;static uc/InitialAppState
  ;(initial-state [this params] (f/build-form this (or params {})))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/dropdown-input :condition/field [(f/option :out/desc "Description")
                                                         (f/option :out/amount "Amount")])
                     (f/dropdown-input :condition/predicate [(f/option :starts-with "Starts with")
                                                             (f/option :ends-with "Ends with")
                                                             (f/option :equals "Equals")])
                     (f/text-input :condition/subject)])
  static om/IQuery
  (query [this] [:db/id :condition/field :condition/predicate :condition/subject f/form-key :ui/editable? :ui/selected?])
  static om/Ident
  (ident [this props] [:condition/by-id (:db/id props)])
  Object
  (render [this]
    (let [{:keys [db/id condition/field condition/predicate condition/subject ui/editable? ui/selected?] :as form} (om/props this)
          {:keys [condition-selected-f]} (om/get-computed this)
          selected-style (when selected? #js {:backgroundColor "lightBlue"})
          attribs #js {:onClick #(condition-selected-f id)
                       :style   selected-style}]
      (assert (or (nil? editable?) (boolean? editable?)) (us/assert-str "editable?" editable?))
      (cond
        editable? (dom/tr nil
                          (fh/field-with-label-in-row this form :condition/field "Field:")
                          (fh/field-with-label-in-row this form :condition/predicate "Predicate:")
                          (fh/field-with-label-in-row this form :condition/subject "Value:"))
        :else (let [field-display (us/kw->string field)
                    predicate-display (us/kw->string predicate)]
                (dom/tr attribs
                        (dom/td #js {:className "col-md-2"} field-display)
                        (dom/td #js {:className "col-md-2"} predicate-display)
                        (dom/td #js {:className "col-md-2"} subject)
                        (when selected? (dom/td #js {:className "col-md-1"} (b/button {} "Delete")))))))))
(def ui-maybe-form-condition-row (om/factory MaybeFormConditionRow {:keyfn :db/id}))

(defui ^:once ValidatedConditionForm
       ;static uc/InitialAppState
       ;(initial-state [this params] (f/build-form this (or params {})))
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

