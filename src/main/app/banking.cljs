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
    {:db/id              id
     :bank-line/src-bank :bank/anz-visa
     :bank-line/date     "24/08/2016"
     :bank-line/desc     "OFFICEWORKS SUPERSTO      KESWICK"
     :bank-line/amount   0.00M})
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

(defui ^:once Rule-1
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/source-bank :rule/target-account :rule/logic-operator :rule/conditions])
  Object
  (render [this]
    (let [{:keys [db/id rule/permanent? rule/source-bank rule/target-account rule/logic-operator rule/conditions]} (om/props this)
          _ (assert (some? permanent?))
          permanent-display (if permanent? "true" "false")
          source-bank-display (u/kw->string source-bank)
          target-account-display (u/kw->string target-account)
          logic-operator-display (u/kw->string logic-operator)
          num-conds (str (count conditions))
          conds (str conditions)
          ]
      (dom/tr nil
              (dom/td #js {:className "col-md-2"} num-conds)
              (dom/td #js {:className "col-md-2"} permanent-display)
              (dom/td #js {:className "col-md-2"} source-bank-display)
              (dom/td #js {:className "col-md-2"} target-account-display)
              (dom/td #js {:className "col-md-2"} logic-operator-display)
              (dom/td #js {:className "col-md-2"} conds)))))

(defui ^:once Rule-2
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/source-bank :rule/target-account])
  Object
  (render [this]
    (let [{:keys [db/id rule/source-bank rule/target-account rule/conditions]} (om/props this)
          source-bank-display (and source-bank (subs (str source-bank) 1))
          target-account-display (and target-account (subs (str target-account) 1))
          num-conds (str (count conditions))
          ]
      (dom/label nil
                 (str id " " source-bank-display " " target-account-display " NUM conditions: " num-conds)
                 ))))
(def Rule Rule-1)
(def ui-rule (om/factory Rule {:keyfn :db/id}))

(defn make-condition [[field predicate subject]]
  {:condition/field     field
   :condition/predicate predicate
   :condition/subject   subject})

(defui ^:once RulesList
  static om/Ident
  (ident [this props] [:rules-list/by-id p/RULES_LIST])
  static om/IQuery
  (query [this] [:db/id :rules-list/label {:rules-list/items (om/get-query Rule)}])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id label]}]
    {:db/id            id
     :rules-list/label label
     :rules-list/items []})
  Object
  (render [this]
    (let [{:keys [db/id rules-list/label rules-list/items]} (om/props this)]
      (if (= 1 (count items))
        (let [conditions (map make-condition (:rule/conditions (first items)))]
          (dom/table #js {:className "table table-bordered table-sm table-hover"}
                     (dom/tbody nil (map ui-condition conditions))))
        (dom/div nil
                 (dom/label nil (str "Num rules: " (count items)))
                 (dom/div nil
                          ;(dom/label nil (str "ID: " id ", <" label "> " (count items)))
                          ;; table-inverse did not work
                          ;; table-striped doesn't work well with hover as same colour
                          (dom/table #js {:className "table table-bordered table-sm table-hover"}
                                     (dom/tbody nil (map ui-rule items)))
                          #_(map ui-rule items)
                          ))))))
(def ui-rules-list (om/factory RulesList))

(def rule {:logic-operator          :or,
           :conditions              [[:out/desc :starts-with "OFFICEWORKS"] [:out/desc :equals "POST   APPIN LPO          APPIN"]],
           :rule/source-bank        :bank/anz-visa,
           :rule/target-account     :exp/office-expense,
           :between-dates-inclusive nil,
           :on-dates                nil})
;; :logic-operator dropdown :and :or :single
;; :conditions can be a sub form that has list of existing above, and way of entering below.
;; Way of entering:
;; :out/desc was selected from dropdown of everything in bank-line, but with :out namespace
;; :starts-with was selected from dropdown also includes (only) :equals
;; text comes from a text field
;; :rule/source-bank and :rule/target-account are dropdowns - list values that must come in with config data:
;; :config-data/ledger-accounts
;; :config-data/bank-accounts

;;
;; More than a pun. Getting a bank statement line that is either not satisfied by a rule,
;; or has too many rules. In the first unruly case the user must pick one. In the second
;; unruly case the user must pick one from many (assert which one dominates).
;; If nothing comes back a message saying 'all good' is displayed to the user.
;; Once the user has done the rules thing he presses the button which calls this fn again.
;;
(defn load-unruly-bank-statement-line [comp]
  (df/load comp :my-unruly-bank-statement-line BankStatementLine
           {:post-mutation `cljs-ops/unruly-bank-statement-line}))

(defn load-existing-rules [comp source-bank target-ledger]
  (df/load comp :my-existing-rules Rule
           {:target  help/rules-list-items-whereabouts
            :refresh [[:rules-list/by-id p/RULES_LIST]]
            :params  {:source-bank source-bank :target-ledger target-ledger}}))

(def type->desc
  {:type/exp       "Expense"
   :type/non-exp   "Non-Expense"
   :type/income    "Income"
   :type/personal  "Personal"
   :type/liability "Liability"})

(defui ^:once RuleForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id                         id
                        :rule-form/existing-rules      (uc/get-initial-state RulesList {:id p/RULES_LIST :label "Invisible??"})
                        :rule-form/bank-statement-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})}))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     (f/dropdown-input :ui/type
                                       [(f/option :type/exp "Expense")
                                        (f/option :type/non-exp "Non-Expense")
                                        (f/option :type/income "Income")
                                        ;; S/be done with checkbox. Sep ui/personal? then we don't need to display the
                                        ;; target-ledger, which will be auto-set to same bank account but with :personal
                                        ;; namespace at the beginning.
                                        ;; For examples rule on server will end up being:
                                        ;; :rule/source-bank :bank/amp, :rule/target-account :personal/amp
                                        ;; OR
                                        ;; :rule/source-bank :bank/anz-visa, :rule/target-account :personal/anz-visa
                                        ;; For this to happen there will need to be a mutation to set the target
                                        ;; account (known here as target-ledger). Un-setting to of course un-set.
                                        ;; When personal/trash is on the target-ledger drop down will disappear.
                                        ;;
                                        ;(f/option :type/personal "Personal")
                                        (f/option :type/liab "Liability")]
                                       )
                     (f/dropdown-input :rule-form/target-ledger
                                       [(f/option :not-yet-2 "Not yet loaded 2")]
                                       :default-value :not-yet-2)
                     (f/dropdown-input :rule-form/logic-operator
                                       [(f/option :single "")
                                        (f/option :and "AND")
                                        (f/option :or "OR")]
                                       :default-value :single)])
  static om/Ident
  (ident [_ props] [:rule-form/by-id (:db/id props)])
  static om/IQuery
  (query [_] [:db/id :ui/type :rule-form/logic-operator
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
          {:keys [config-data/ledger-accounts config-data/bank-accounts]} config-data
          ;period-label (condp = period-type
          ;               :period-type/quarterly "Quarter"
          ;               :period-type/monthly "Month"
          ;               :period-type/unknown "Unknown"
          ;               nil "Unknown")
          ;at-className (if manually-executable? "btn btn-primary" "btn disabled")
          ;at-disabled (if manually-executable? "" "true")
          type-description (type->desc type)
          ]
      (u/log-off (str "the counts: " (count ledger-accounts) (count bank-accounts)))
      (u/log-off type)
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this form :ui/type
                                    "Type"
                                    {:onChange (fn [evt]
                                                 (let [new-val (u/target-kw evt)]
                                                   (when (type->desc new-val)
                                                     (om/transact! this `[(cljs-ops/config-data-for-target-dropdown
                                                                            {:acct-type ~new-val})]))))})
               (when type-description (fh/field-with-label this form :rule-form/target-ledger
                                                           type-description
                                                           {:onChange (fn [evt]
                                                                        (let [new-val (u/target-kw evt)]
                                                                          (u/log-on (str "src bank: " src-bank ", target ledger: " new-val))
                                                                          (load-existing-rules this src-bank new-val)))}))
               (ui-rules-list existing-rules)))))

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