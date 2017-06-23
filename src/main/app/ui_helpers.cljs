(ns app.ui-helpers
  (:require [om.dom :as dom]
            [untangled.ui.forms :as f]))

(defn field-with-label
  "A non-library helper function, written by you to help lay out your form."
  ([comp form name label] (field-with-label comp form name label nil))
  ([comp form name label validation-message]
   (dom/div #js {:className (str "form-group" (if (f/invalid? form name) " has-error" ""))}
            (dom/label #js {:className "col-sm-2" :htmlFor name} label)
            ;; THE LIBRARY SUPPLIES f/form-field. Use it to render the actual field
            (dom/div #js {:className "col-sm-10"} (f/form-field comp form name))
            (when (and validation-message (f/invalid? form name))
              (dom/span #js {:className (str "col-sm-offset-2 col-sm-10" name)} validation-message)))))

;;
;; Can be used in a mutation to assoc-in new options
;;
(defn input-options [[table-name id] field]
  [table-name id ::f/form :elements/by-name field :input/options])

;;
;; Useful for things like changing options in fields in panels
;;
(def year-options (input-options [:user-request/by-id 'USER-REQUEST-FORM] :request/year))
(def replace-year-options [(f/option :a "A")])

