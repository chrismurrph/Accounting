(ns app.person
  (:require [fulcro.client.core :as uc]
            [om.next :as om :refer [defui]]
            [fulcro.ui.forms :as f]
            [om.dom :as dom]
            [app.forms-helpers :as fh]
            [app.cljs-operations :as cljs-ops]))

(defui ^:once ValidatedConditionForm
       static uc/InitialAppState
       (initial-state [this params] (f/build-form this (or params {})))
       Object
       (render [this]
               (let [{:keys [condition/field condition/predicate condition/subject]} (om/props this)
                     field-display (and field (subs (str field) 1))
                     predicate-display (and predicate (subs (str predicate) 1))]
                 (dom/tr nil
                         (dom/td #js {:className "col-md-2"} field-display)
                         (dom/td #js {:className "col-md-2"} predicate-display)
                         (dom/td #js {:className "col-md-2"} subject)))))

(defui ^:once ValidatedPhoneForm
       static uc/InitialAppState
       (initial-state [this params] (f/build-form this (or params {})))
       static f/IForm
       (form-spec [this] [(f/id-field :db/id)
                          (f/text-input :phone/number
                                        ;:validator `help/us-phone?
                                        )
                          (f/dropdown-input :phone/type [(f/option :home "Home") (f/option :work "Work")])])
       static om/IQuery
       (query [this] [:db/id :phone/type :phone/number f/form-key])
       static om/Ident
       (ident [this props] [:phone/by-id (:db/id props)])
       Object
       (render [this]
               (let [form (om/props this)]
                 (dom/div #js {:className "form-horizontal"}
                          (fh/field-with-label this form :phone/type "Phone type:")
                          ;; One more parameter to give the validation error message:
                          (fh/field-with-label this form :phone/number "Number:" "Please format as (###) ###-####")))))

(def ui-vphone-form (om/factory ValidatedPhoneForm))

(defui ^:once PersonForm
       static uc/InitialAppState
       (initial-state [this params] (f/build-form this (or params {})))
       static f/IForm
       (form-spec [this] [(f/id-field :db/id)
                          (f/subform-element :person/phone-numbers ValidatedPhoneForm :many)
                          (f/text-input :person/name
                                        ;:validator `help/name-valid?
                                        )
                          (f/integer-input :person/age
                                           ;:validator `f/in-range?
                                           :validator-args {:min 1 :max 110})
                          (f/checkbox-input :person/registered-to-vote?)])
       static om/IQuery
       ; NOTE: f/form-root-key so that sub-forms will trigger render here
       (query [this] [f/form-root-key f/form-key
                      :db/id :person/name :person/age
                      :person/registered-to-vote?
                      {:person/phone-numbers (om/get-query ValidatedPhoneForm)}])
       static om/Ident
       (ident [this props] [:people/by-id (:db/id props)])
       Object
       (render [this]
               (let [{:keys [person/phone-numbers] :as props} (om/props this)]
                 (dom/div #js {:className "form-horizontal"}
                          (fh/field-with-label this props :person/name "Full Name:" "Please enter your first and last name.")
                          (fh/field-with-label this props :person/age "Age:" "That isn't a real age!")
                          (fh/field-with-label this props :person/registered-to-vote? "Registered?" {:checkbox-style? true})
                          (when (f/current-value props :person/registered-to-vote?)
                            (dom/div nil "Good on you!"))
                          (dom/div nil
                                   (mapv ui-vphone-form phone-numbers))
                          (when (f/valid? props)
                            (dom/div nil "All fields have had been validated, and are valid"))
                          (dom/div #js {:className "button-group"}
                                   (dom/button #js {:className "btn btn-primary"
                                                    :onClick   #(om/transact! this
                                                                              `[(cljs-ops/add-phone ~{:id         (om/tempid)
                                                                                                      :person     (:db/id props)
                                                                                                      :phone-form ValidatedPhoneForm})])}
                                               "Add Phone")
                                   (dom/button #js {:className "btn btn-default" :disabled (f/valid? props)
                                                    :onClick   #(f/validate-entire-form! this props)}
                                               "Validate")
                                   (dom/button #js {:className "btn btn-default", :disabled (not (f/dirty? props))
                                                    :onClick   #(f/reset-from-entity! this props)}
                                               "UNDO")
                                   (dom/button #js {:className "btn btn-default", :disabled (not (f/dirty? props))
                                                    :onClick   #(om/transact! this `[(f/validate-form {:form-id ~(f/form-ident props)})
                                                                                     (f/commit-to-entity {:form ~(om/props this) :remote true})])}
                                               "Submit"))))))

(def ui-person-form (om/factory PersonForm))

(def first-person
  (uc/initial-state PersonForm
                    {:db/id                      1
                     :person/name                "Tony Kay"
                     :person/age                 43
                     :person/registered-to-vote? false
                     :person/phone-numbers       [(uc/initial-state ValidatedPhoneForm
                                                                    {:db/id        22
                                                                     :phone/type   :work
                                                                     :phone/number "(123) 412-1212"})
                                                  (uc/initial-state ValidatedPhoneForm
                                                                    {:db/id        23
                                                                     :phone/type   :home
                                                                     :phone/number "(541) 555-1212"})]}))

