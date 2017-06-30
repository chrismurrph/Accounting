(ns app.banking
  (:require [om.dom :as dom]
            [om.next :as om]
            [untangled.client.core :as uc]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [app.util :as u]
            [goog.string :as gstring]
            [goog.string.format]
            [app.cljs-operations :as cljs-ops]
            [app.panels :as p]
            [untangled.ui.forms :as f]
            [app.forms-helpers :as fh]
            [app.config :as config]
            [app.domain-ui-helpers :as help]))

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
          source-bank-display (u/kw->string source-bank)
          target-account-display (u/kw->string target-account)
          logic-operator-display (u/kw->string logic-operator)
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
                          (u/log (str "Clicked on " id))
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
(defn load-unruly-bank-statement-line [comp]
  (df/load comp :my-unruly-bank-statement-line BankStatementLine
           {:post-mutation `cljs-ops/unruly-bank-statement-line}))

(defn load-existing-rules [comp source-bank target-ledger]
  (df/load comp :my-existing-rules Rule
           {:target  help/rules-list-items-whereabouts
            ;; Refreshing one higher fixes issue that when first selected type, target was auto-selected to first,
            ;; but the existing rules were not being displayed.
            :refresh [[:rule-form/by-id p/RULE_FORM]]
            :params  {:source-bank source-bank :target-ledger target-ledger}}))

(defui ^:once RuleForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id                         id
                        :rule-form/existing-rules      (uc/get-initial-state RulesList {:id p/RULES_LIST :label "Invisible??"})
                        :rule-form/bank-statement-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})}))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/dropdown-input :ui/type help/type-options)
                     (f/dropdown-input :rule-form/target-ledger
                                       [(f/option :not-yet-2 "Not yet loaded 2")]
                                       :default-value :not-yet-2)
                     (f/dropdown-input :rule-form/logic-operator help/logic-options
                                       :default-value :single)])
  static om/Ident
  (ident [_ props] [:rule-form/by-id (:db/id props)])
  static om/IQuery
  (query [_] [:db/id
              :ui/type
              :rule-form/logic-operator
              {:rule-form/bank-statement-line (om/get-query BankStatementLine)}
              :rule-form/target-ledger
              ;; Only when there's a target account will any of these come back
              {:rule-form/existing-rules (om/get-query RulesList)}
              {:rule-form/config-data (om/get-query config/ConfigData)} f/form-root-key f/form-key])
  Object
  (render [this]
    (let [{:keys [ui/type rule-form/config-data rule-form/logic-operator rule-form/bank-statement-line
                  rule-form/target-ledger rule-form/existing-rules] :as form} (om/props this)
          {:keys [bank-line/src-bank]} bank-statement-line
          {:keys [config-data/ledger-accounts]} config-data
          type-description (help/type->desc type)
          matching-rules-count (-> existing-rules :rules-list/items count)]
      (dom/div #js {:className "form-horizontal"}
               #_(dom/label nil (str "DEBUG - Target ledger is " target-ledger
                                   ", and count of existing: " (-> existing-rules :rules-list/items count)))
               (fh/field-with-label this form :ui/type
                                    "Type"
                                    {:label-width-css "col-sm-2"
                                     :onChange (fn [evt]
                                                 (let [new-val (u/target-kw evt)]
                                                   (when (help/type->desc new-val)
                                                     (om/transact! this `[(cljs-ops/config-data-for-target-ledger-dropdown
                                                                            {:acct-type ~new-val})]))))})
               (when type-description (fh/field-with-label this form :rule-form/target-ledger
                                                           (str "Target " type-description)
                                                           {:label-width-css "col-sm-2"
                                                            :onChange (fn [evt]
                                                                        (let [new-target-ledger-val (u/target-kw evt)]
                                                                          (u/log-on (str "src bank: " src-bank ", target ledger: " new-target-ledger-val))
                                                                          (load-existing-rules this src-bank new-target-ledger-val)))}))
               ;Remember that :rule-form is initially how are querying for a rule, that might become the new rule, or
               ; might become editing one of the existing rules. [Select Existing]
               ;whereas :rule is one of many possibilities may want to start editing
               ;(fh/field-with-label this form :rule-form/logic-operator "Selector")
               (if (zero? matching-rules-count)
                 (dom/label nil (str "No matching rules for " (help/ledger-kw->account-name target-ledger) ". Create new rule:"))
                 (ui-rules-list existing-rules))))))

(def ui-rule-form (om/factory RuleForm))

(defui ^:once Banking
  static om/IQuery
  (query [this] [:page
                 {:banking/bank-line (om/get-query BankStatementLine)}
                 {:banking/rule-form (om/get-query RuleForm)}])
  static
  uc/InitialAppState
  (initial-state [c params] {:page              :banking
                             :banking/bank-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})
                             :banking/rule-form (uc/get-initial-state RuleForm {:id p/RULE_FORM})})
  Object
  (render [this]
    (let [{:keys [page banking/bank-line banking/rule-form]} (om/props this)]
      (if (:bank-line/src-bank bank-line)
        (dom/div nil
                 (ui-bank-statement-line bank-line)
                 (dom/br nil) (dom/br nil)
                 (ui-rule-form rule-form))
        (dom/label nil "All imported bank statement lines have been reconciled against rules")))))