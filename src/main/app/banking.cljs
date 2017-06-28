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
    (let [{:keys [db/id bank-line/src-bank bank-line/date bank-line/desc bank-line/amount]} (om/props this)
          ;_ (println (om/props this))
          src-bank-display (help/bank-kw->bank-name src-bank)
          negative? (= \- (first (str amount)))
          amount-display (str "$" (gstring/format "%.2f" (u/abs amount)))]
      (dom/div #js {:className "form-group"}
               (dom/label #js {:className "col-md-1"} date)
               (dom/label #js {:className "col-md-4"} desc)
               (dom/label #js {:style #js {:color (if negative? "red" "")} :className "text-right col-md-1"} amount-display)
               (dom/label #js {:className "col-md-6"} src-bank-display)))))
(def ui-bank-statement-line (om/factory BankStatementLine {:keyfn :db/id}))

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

(defui ^:once RuleForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}]
    (f/build-form this {:db/id            id
                        :rule/config-data nil}))
  static f/IForm
  (form-spec [this] [(f/id-field :db/id)
                     ;; Here hard-coding what will come in at login time
                     (f/dropdown-input :rule/logic-operator
                                       [(f/option :single "")
                                        (f/option :and "AND")
                                        (f/option :or "OR")]
                                       :default-value :single)
                     #_(f/dropdown-input :rule/source-bank
                                       [(f/option :not-yet-1 "Not yet loaded 1")]
                                       :default-value :not-yet-1)
                     (f/dropdown-input :rule/target-account
                                       [(f/option :not-yet-2 "Not yet loaded 2")]
                                       :default-value :not-yet-2)])
  static om/Ident
  (ident [_ props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [_] [:db/id :rule/logic-operator :rule/source-bank :rule/target-account
              {:rule/config-data (om/get-query config/ConfigData)} f/form-root-key f/form-key])
  Object
  (render [this]
    (let [{:keys [rule/config-data rule/logic-operator rule/source-bank rule/target-account] :as form} (om/props this)
          {:keys [config-data/ledger-accounts config-data/bank-accounts]} config-data
          ;period-label (condp = period-type
          ;               :period-type/quarterly "Quarter"
          ;               :period-type/monthly "Month"
          ;               :period-type/unknown "Unknown"
          ;               nil "Unknown")
          ;at-className (if manually-executable? "btn btn-primary" "btn disabled")
          ;at-disabled (if manually-executable? "" "true")
          ]
      (println "the counts: " (count ledger-accounts) (count bank-accounts))
      (dom/div #js {:className "form-horizontal"}
               (fh/field-with-label this form :rule/logic-operator
                                    "Logic"
                                    {:onChange (fn [evt]
                                                 )})
               #_(fh/field-with-label this form :rule/source-bank
                                    "Source"
                                    {:onChange (fn [evt]
                                                 )})
               (fh/field-with-label this form :rule/target-account
                                    "Target"
                                    {:onChange (fn [evt]
                                                 )})
               #_(dom/button #js {:className at-className
                                  :disabled  at-disabled
                                  :onClick   (execute-report this organisation year period report)}
                             (if manually-executable? "Execute Report" "Auto Execute ON"))))))
(def ui-rule-form (om/factory RuleForm))

(defui ^:once Banking
  static om/IQuery
  (query [this] [:page
                 {:banking/bank-line (om/get-query BankStatementLine)}
                 {:banking/rule (om/get-query RuleForm)}])
  static
  uc/InitialAppState
  (initial-state [c params] {:page              :banking
                             :banking/bank-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})
                             :banking/rule      (uc/get-initial-state RuleForm {:id p/RULE_FORM})})
  Object
  (render [this]
    (let [{:keys [page banking/bank-line banking/rule]} (om/props this)]
      (dom/div nil
               (ui-bank-statement-line bank-line)
               (dom/br nil) (dom/br nil)
               (ui-rule-form rule)))))

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
