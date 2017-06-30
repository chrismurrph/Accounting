(ns app.uis-used-by-mutations
  (:require [app.forms-helpers :as fh]
            [om.dom :as dom]
            [om.next :as om]
            [om.next :as om :refer [defui]]
            [untangled.ui.forms :as f]
            [untangled.client.core :as uc]
            [app.domain-ui-helpers :as help]))

(defui ^:once ValidatedPhoneForm
       static uc/InitialAppState
       (initial-state [this params] (f/build-form this (or params {})))
       static f/IForm
       (form-spec [this] [(f/id-field :db/id)
                          (f/text-input :phone/number :validator `help/us-phone?) ; Addition of validator
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
