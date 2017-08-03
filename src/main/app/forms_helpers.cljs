(ns app.forms-helpers
  (:require [om.dom :as dom]
            [fulcro.ui.forms :as f]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [om.next :as om]
            [cljc.utils :as us]))

(defn form? [?form]
  (and (f/is-form? ?form)
       #_(some #{:fulcro.ui.forms/form} (keys ?form))
       (get ?form :fulcro.ui.forms/form)))

;;
;; links are top level keys
;; We want to explicitly set them to nil rather than dissoc them
;;
(defn make-links-nil [st links]
  (reduce
    (fn [m link]
      (assoc m link nil))
    st
    links))

(defn fns-over-state [st fns]
  (reduce
    (fn [m f]
      (f m))
    st
    fns))

(defn ->form
  ([form-class]
   (assert form-class)
   (fn [object-map target clear-links]
     (assert (map? object-map) (us/assert-str "object-map" object-map))
     (assert (:db/id object-map) (str "No :db/id in <" object-map ">"))
     (let [object-as-a-form (f/build-form form-class object-map)
           _ (assert (= (:db/id object-map) (:db/id object-as-a-form)))
           _ (assert (form? object-as-a-form))
           ident (om/ident form-class object-as-a-form)]
       (assert (= (second ident) (:db/id object-map)))
       (fn [st]
         (cond-> st
                 true (assoc-in ident object-as-a-form)
                 target (assoc-in target ident)
                 true (make-links-nil (or clear-links []))))))))

;;
;; Can be used in a mutation to assoc-in new options
;;
(defn input-options [[table-name id :as ident] field]
  [table-name id ::f/form :elements/by-name field :input/options])
(defn input-default-value [[table-name id :as ident] field]
  [table-name id ::f/form :elements/by-name field :input/default-value])

(defn options-generator [data->list-fn item->option-fn list->selected-fn]
  (fn [data parent-selection]
    (let [list (data->list-fn parent-selection data)
          options (mapv item->option-fn list)
          selected (list->selected-fn list parent-selection)]
      [selected options])))

(defn field-with-label
  "A non-library helper function, written by you to help lay out your form."
  ([comp form name label] (field-with-label comp form name label nil))
  ([comp form name label {:keys [validation-message onChange label-width-css checkbox-style?] :as params}]
   (assert label (str "No label passed in for " name))
   (assert (or (nil? onChange) (fn? onChange)))
    ;(println params)
   (if checkbox-style?
     (dom/div #js {:className "checkbox"}
              (dom/label nil (f/form-field comp form name params) label))
     (dom/div #js {:className (str "form-group" (if (f/invalid? form name) " has-error" ""))}
              (dom/label #js {:className (or label-width-css "col-sm-1") :htmlFor name} label)
              (dom/div #js {:className "col-sm-2"}
                       (f/form-field comp form name :onChange onChange))
              (when (and validation-message (f/invalid? form name))
                (dom/span #js {:className (str "col-sm-offset-1 col-sm-2" name)} validation-message))))))

(defn field-with-label-in-row
  "A non-library helper function, written by you to help lay out your form."
  ([comp form name label] (field-with-label-in-row comp form name label nil))
  ([comp form name label {:keys [validation-message onChange label-width-css checkbox-style?] :as params}]
   (assert label (str "No label passed in for " name))
   (assert (or (nil? onChange) (fn? onChange)))
    ;(println params)
   (if checkbox-style?
     (dom/td #js {:className "checkbox"}
              (dom/label nil (f/form-field comp form name params) label))
     (dom/td #js {:className (str "form-group" (if (f/invalid? form name) " has-error" ""))}
              (dom/label #js {:className (or label-width-css "col-sm-1") :htmlFor name} label)
              (dom/span #js {:className "col-sm-2"}
                       (f/form-field comp form name :onChange onChange))
              (when (and validation-message (f/invalid? form name))
                (dom/span #js {:className (str "col-sm-offset-1 col-sm-2" name)} validation-message))))))

(comment "Makes no sense - yes it did - no onChange capability"
         (defn checkbox-with-label
           "A helper function to lay out checkboxes."
           ([comp form name label]
            (field-with-label comp form name label nil))
           ([comp form name label params]
            (dom/div #js {:className "checkbox"}
                     (dom/label nil (f/form-field comp form name params) label)))))

;;
;; Current field value and current options get changed together.
;; have one of these for every dropdown.
;;
(defn dropdown-rebuilder [field-whereabouts options-whereabouts default-value-whereabouts]
  (fn [st field-value options-value]
    (-> st
        (assoc-in field-whereabouts field-value)
        (assoc-in default-value-whereabouts field-value)
        (assoc-in options-whereabouts options-value))))


