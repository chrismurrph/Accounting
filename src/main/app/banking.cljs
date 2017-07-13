(ns app.banking
  (:require [om.dom :as dom]
            [om.next :as om]
            [untangled.client.core :as uc]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [app.util :as u]
            [cljc.utils :as us]
            [goog.string :as gstring]
            [goog.string.format]
            [app.cljs-operations :as cljs-ops]
            [app.panels :as p]
            [untangled.ui.forms :as f]
            [app.forms-helpers :as fh]
            [app.config :as config]
            [app.domain-ui-helpers :as help]
            [app.operations :as ops]))

;;
;; Expect to only see one of these, the one that has no rule or too many rules.
;;
(defui ^:once BankStatementLine
  static om/Ident
  (ident [this props] [:bank-line/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :bank-line/src-bank :bank-line/date :bank-line/desc :bank-line/amount])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id]}]
    (help/make-example-bank-line id))
  Object
  (render [this]
    (let [{:keys [db/id bank-line/src-bank bank-line/date bank-line/desc bank-line/amount]} (om/props this)]
      (let [src-bank-display (help/bank-kw->bank-name src-bank)
            negative? (= \- (first (str amount)))
            amount-display (str "$" (gstring/format "%.2f" (u/abs amount)))]
        (dom/div #js {:className "form-group"}
                 (dom/label #js {:className "col-md-1"} date)
                 (dom/label #js {:className "col-md-4"} desc)
                 (dom/label #js {:style #js {:color (if negative? "red" "")} :className "text-right col-md-1"} amount-display)
                 (dom/label #js {:className "col-md-6"} src-bank-display))))))
(def ui-bank-statement-line (om/factory BankStatementLine {:keyfn :db/id}))

(defui ^:once Condition
  Object
  (render [this]
    (let [{:keys [condition/field condition/predicate condition/subject]} (om/props this)
          field-display (and field (subs (str field) 1))
          predicate-display (and predicate (subs (str predicate) 1))]
      (dom/tr nil
              (dom/td #js {:className "col-md-2"} field-display)
              (dom/td #js {:className "col-md-2"} predicate-display)
              (dom/td #js {:className "col-md-2"} subject)))))
(def ui-condition (om/factory Condition {:keyfn :condition/subject}))

(defui ^:once Rule
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/source-bank :rule/target-account :rule/logic-operator :rule/conditions])
  Object
  (render [this]
    (let [{:keys [db/id rule/permanent? rule/source-bank rule/target-account rule/logic-operator rule/conditions]} (om/props this)
          {:keys [rule-selected]} (om/get-computed this)
          _ (assert (some? permanent?))
          permanent-display (if permanent? "true" "false")
          source-bank-display (us/kw->string source-bank)
          target-account-display (us/kw->string target-account)
          logic-operator-display (us/kw->string logic-operator)
          num-conds (str (count conditions))
          conds (str conditions)
          ]
      (dom/tr #js {:onClick #(rule-selected id)}
              (dom/td #js {:className "col-md-2"} permanent-display)
              (dom/td #js {:className "col-md-2"} source-bank-display)
              (dom/td #js {:className "col-md-2"} target-account-display)
              (dom/td #js {:className "col-md-2"} logic-operator-display)
              (dom/td #js {:className "col-md-2"} num-conds)
              (dom/td #js {:className "col-md-2"} conds)))))
(def ui-rule (om/factory Rule {:keyfn :db/id}))

(def rule-header
  (dom/tr nil
          (dom/th #js {:className "col-md-2"} "Permanent?")
          (dom/th #js {:className "col-md-2"} "Source bank account")
          (dom/th #js {:className "col-md-2"} "Target ledger account")
          (dom/th #js {:className "col-md-2"} "Logic operator")
          (dom/th #js {:className "col-md-2"} "Num conditions")
          (dom/th #js {:className "col-md-2"} "Conditions")))

(defui ^:once RulesList
  static om/Ident
  (ident [this props] [:rules-list/by-id p/RULES_LIST])
  static om/IQuery
  (query [this] [:db/id :rules-list/label :ui/selected-rule {:rules-list/items (om/get-query Rule)}])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id label]}]
    {:db/id            id
     :rules-list/label label
     :rules-list/items []})
  Object
  (render [this]
    (let [{:keys [db/id rules-list/label ui/selected-rule rules-list/items]} (om/props this)
          rule-selected (fn [id]
                          (us/log (str "Clicked on " id))
                          (om/transact! this `[(cljs-ops/selected-rule {:selected ~id})]))
          rule-unselected #(om/transact! this `[(cljs-ops/un-select-rule)])]
      (dom/div nil
               ;For debugging, display s/be handling this aspect
               #_(dom/label nil (str "DEBUG - Matching rules count: " (count items) (when selected-rule (str ", selected rule: " (inc selected-rule)))))
               (if (or selected-rule (= 1 (count items)))
                 (let [{:keys [rule/logic-operator] :as the-rule} (nth items (or selected-rule 0))
                       conditions (map help/make-condition (:rule/conditions the-rule))]
                   (dom/div nil
                            (when selected-rule
                              (dom/div nil
                                       (dom/label nil (str "Selector: " logic-operator))
                                       (dom/button #js {:onClick rule-unselected} "Back")
                                       (dom/label nil (str "Selected rule: " (inc selected-rule) " of " (count items)))))
                            (dom/table #js {:className "table table-bordered table-sm table-hover"}
                                       (dom/tbody nil (map ui-condition conditions)))))
                 (when (pos? (count items))
                   (dom/div nil
                            (dom/label nil (str "Num matching rules: " (count items) " (click to select)"))
                            ;(dom/label nil (str "ID: " id ", <" label "> " (count items)))
                            ;; table-inverse did not work
                            ;; table-striped doesn't work well with hover as same colour
                            (dom/table #js {:className "table table-bordered table-sm table-hover"}
                                       (dom/thead nil rule-header)
                                       (dom/tbody nil (map #(ui-rule (om/computed % {:rule-selected rule-selected})) items))))))))))
(def ui-rules-list (om/factory RulesList))

;; :logic-operator dropdown :and :or :single
;; :conditions can be a sub form that has list of existing above, and way of entering below.
;; Way of entering:
;; :out/desc was selected from dropdown of everything in bank-line, but with :out namespace (FIELD)
;; :starts-with was selected from dropdown also includes (only) :equals (PREDICATE)
;; text comes from a text field (SUBJECT)
;; :rule/target-account is a dropdown - list of values that must come in with config data:
;; :config-data/ledger-accounts

;;
;; 'unruly' is more than a pun. Getting a bank statement line that is either not satisfied by a rule,
;; or has too many rules. In the first unruly case the user must pick one. In the second
;; unruly case the user must pick one from many (assert which one dominates).
;; If nothing comes back a message saying 'all good' is displayed to the user.
;; Once the user has done the rules thing he presses the button which calls this fn again.
;; Something else that may come is a [Merge edit] button, which won't be available if there
;; are two and one is permanent and the other is temporal. In that case user has to understand
;; that can edit one and come back, then merge will be available.
;;
(defn load-unruly-bank-statement-line [comp organisation]
  (df/load comp :my-unruly-bank-statement-line BankStatementLine
           {:post-mutation `cljs-ops/unruly-bank-statement-line
            :params {:request/organisation organisation}}))

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

(defn banking-initial-state [id]
  {:db/id                            id
   :banking-form/bank-statement-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})
   :banking-form/existing-rules      (uc/get-initial-state RulesList {:id p/RULES_LIST :label "Invisible??"})
   :person                           nil})

;; Refreshing one higher fixes issue that when first selected type, target was auto-selected to first,
;; but the existing rules were not being displayed.
(defn load-existing-rules [comp source-bank target-ledger]
  (df/load comp :my-existing-rules Rule
           {:target  help/rules-list-items-whereabouts
            :refresh [[:banking-form/by-id p/BANKING_FORM]]
            :params  {:source-bank source-bank :target-ledger target-ledger}}))

#_(defn load-existing-rules-bad [comp source-bank target-ledger]
    (om/transact! comp `[(cljs-ops/load-existing-rules
                           {:sub-query-comp ~Rule
                            :source-bank    ~source-bank
                            :target-ledger  ~target-ledger})]))

(def yet-to-be-selected-ledger-type :not-yet-3)
(def no-pick :untangled.ui.forms/none)

(defui ^:once BankingForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this (banking-initial-state id)))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/dropdown-input :ui/ledger-type help/ledger-type-options)
                     (f/dropdown-input :banking-form/target-ledger
                                       [(f/option yet-to-be-selected-ledger-type "Not yet loaded 3")]
                                       :default-value yet-to-be-selected-ledger-type)
                     (f/dropdown-input :banking-form/logic-operator help/logic-options
                                       :default-value :single)])
  static om/Ident
  (ident [_ props] [:banking-form/by-id (:db/id props)])
  static om/IQuery
  (query [_] [:db/id
              :ui/ledger-type
              :banking-form/logic-operator
              {:banking-form/bank-statement-line (om/get-query BankStatementLine)}
              :banking-form/target-ledger
              ;; Only when there's a target account will any of these come back
              {:banking-form/existing-rules (om/get-query RulesList)}
              {:banking-form/config-data (om/get-query config/ConfigData)} f/form-root-key f/form-key
              {:person (om/get-query PersonForm)}])
  Object
  (render [this]
    (let [{:keys [ui/ledger-type banking-form/config-data banking-form/logic-operator banking-form/bank-statement-line
                  banking-form/target-ledger banking-form/existing-rules person] :as form} (om/props this)
          {:keys [bank-line/src-bank]} bank-statement-line
          {:keys [config-data/ledger-accounts]} config-data
          type-description (help/ledger-type->desc ledger-type)
          matching-rules-count (-> existing-rules :rules-list/items count)]
      (dom/div #js {:className "form-horizontal"}
               #_(dom/label nil (str "DEBUG - Target ledger is " target-ledger
                                     ", and count of existing: " (-> existing-rules :rules-list/items count)))
               (fh/field-with-label this form :ui/ledger-type
                                    "Type"
                                    {:label-width-css "col-sm-2"
                                     :onChange        (fn [evt]
                                                        (let [new-val (u/target-kw evt)]
                                                          (when (help/ledger-type->desc new-val)
                                                            (om/transact! this `[(cljs-ops/config-data-for-target-ledger-dropdown
                                                                                   {:acct-type      ~new-val
                                                                                    :sub-query-comp ~Rule
                                                                                    :src-bank       ~src-bank
                                                                                    :person-form    ~PersonForm})]))))})
               (when type-description (fh/field-with-label this form :banking-form/target-ledger
                                                           (str "Target " type-description)
                                                           {:label-width-css "col-sm-2"
                                                            :onChange        (fn [evt]
                                                                               (let [new-target-ledger-val (u/target-kw evt)]
                                                                                 (us/log-off (str "src bank: " src-bank ", target ledger: " new-target-ledger-val))
                                                                                 (load-existing-rules this src-bank new-target-ledger-val)))}))
               ;Remember that :banking-form is initially how are querying for a rule, that might become the new rule, or
               ; might become editing one of the existing rules. [Select Existing]
               ;whereas :rule is one of many possibilities may want to start editing
               ;(fh/field-with-label this form :banking-form/logic-operator "Selector")
               (if (and (zero? matching-rules-count) (not= ledger-type no-pick) #_(not= target-ledger yet-to-be-selected-ledger-type))
                 (dom/div nil
                          (dom/label nil (str "No matching rules for " (help/ledger-kw->account-name target-ledger)
                                              " from " (help/bank-kw->bank-name src-bank)
                                              ". You need to create a new rule:"))
                          (if person
                            (dom/div nil
                                     (dom/label nil (str "ledger type: " ledger-type))
                                     (ui-person-form person))
                            (us/log "Better create new person")))
                 (ui-rules-list existing-rules))))))

(def ui-banking-form (om/factory BankingForm))

(defui ^:once Banking
  static om/IQuery
  (query [this] [:page
                 {:banking/bank-line (om/get-query BankStatementLine)}
                 {:banking/banking-form (om/get-query BankingForm)}])
  static
  uc/InitialAppState
  (initial-state [c params] {:page                 :banking
                             :banking/bank-line    (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})
                             :banking/banking-form (uc/get-initial-state BankingForm {:id p/BANKING_FORM})})
  Object
  (render [this]
    (let [{:keys [page banking/bank-line banking/banking-form]} (om/props this)]
      (if (:bank-line/src-bank bank-line)
        (dom/div nil
                 (ui-bank-statement-line bank-line)
                 (dom/br nil) (dom/br nil)
                 (ui-banking-form banking-form))
        (dom/label nil "All imported bank statement lines have been reconciled against rules")))))