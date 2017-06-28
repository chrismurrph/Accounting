(ns app.banking
  (:require [om.dom :as dom]
            [om.next :as om]
            [untangled.client.core :as uc]
            [om.next :as om :refer [defui]]
            [untangled.client.data-fetch :as df]
            [app.util :as u]
            [goog.string :as gstring]
            [goog.string.format]
            [app.panels :as p]))

(comment
  (dom/div #js {:className (str "form-group" (if (f/invalid? form name) " has-error" ""))}
           (dom/label #js {:className "col-sm-1" :htmlFor name} label)
           (dom/div #js {:className "col-sm-2"} (f/form-field comp form name :onChange onChange))
           (when (and validation-message (f/invalid? form name))
             (dom/span #js {:className (str "col-sm-offset-1 col-sm-2" name)} validation-message))))

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
     :bank-line/amount   0.00M
     })
  Object
  (render [this]
    (let [{:keys [db/id bank-line/src-bank bank-line/date bank-line/desc bank-line/amount]} (om/props this)
          src-bank-display (name src-bank)
          negative? (= \- (first (str amount)))
          amount-display (str "$" (gstring/format "%.2f" (u/abs amount)))]
      (dom/div #js {:className "form-group"}
               (dom/label #js {:className "col-md-1"} date)
               (dom/label #js {:className "col-md-4"} desc)
               (dom/label #js {:style #js {:color (if negative? "red" "")} :className "text-right col-md-1"} amount-display)
               (dom/label #js {:className "col-md-2"} src-bank-display)))))
(def ui-bank-statement-line (om/factory BankStatementLine {:keyfn :db/id}))

;; :logic-operator dropdown :and :or :single
;; :conditions can be a sub form that has list of existing above, and way of entering below.
;; Way of entering:
;; :out/desc was selected from dropdown of everything in bank-line, but with :out namespace
;; :starts-with was selected from dropdown also includes (only) :equals
;; text comes from a text field
;; :rule/source-bank and :rule/target-account are dropdowns - list values that must come in with config data:
;; :config-data/ledger-accounts
;; :config-data/bank-accounts
(def rule {:logic-operator :or,
           :conditions [[:out/desc :starts-with "OFFICEWORKS"] [:out/desc :equals "POST   APPIN LPO          APPIN"]],
           :rule/source-bank :bank/anz-visa,
           :rule/target-account :exp/office-expense,
           :between-dates-inclusive nil,
           :on-dates nil})

(defui ^:once Banking
  static om/IQuery
  (query [this] [:page
                 {:banking/bank-line (om/get-query BankStatementLine)}])
  static
  uc/InitialAppState
  (initial-state [c params] {:page              :banking
                             :banking/bank-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})})
  Object
  (render [this]
    (let [{:keys [page banking/bank-line]} (om/props this)]
      (dom/div nil (str "I'm Banking: " page))
      (ui-bank-statement-line bank-line))))

;;
;; More than a pun. Getting a bank statement line that is either not satisfied by a rule,
;; or has too many rules. In the first unruly case the user must pick one. In the second
;; unruly case the user must pick one from many (assert which one dominates).
;; If nothing comes back a message saying 'all good' is displayed to the user.
;; Once the user has done the rules thing he presses the button which calls this fn again.
;;
(defn load-unruly-bank-statement-line [comp]
  (df/load comp :my-unruly-bank-statement-line BankStatementLine))
