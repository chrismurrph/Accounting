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
            [app.person :as per]
            [untangled.ui.forms :as f]
            [app.forms-helpers :as fh]
            [app.config :as config]
            [app.domain-ui-helpers :as help]
            [app.operations :as ops]
            [app.time :as t]
            [cljs-time.coerce :as c]))

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
    (assert (not= id -1))
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

(defn display-dates [{:keys [rule/on-dates rule/time-slot]}]
  (let [on-dates-str (cond-> ""
                             (pos? (count on-dates))
                             (str "on: "
                                  (apply str (interpose ", " (map (comp t/show c/from-date) on-dates)))))
        ;; Never gonna work
        time-slot-str (cond-> ""
                              (pos? (count time-slot))
                              (str "betweens: "
                                   (count time-slot)))
        ]
    (str on-dates-str " " time-slot-str)))

;;
;; All the 'just for this period' goes through here as well, but ends up as just ""
;;
(defn display-on-dates [{:keys [rule/on-dates rule/time-slot]}]
  (assert (or (nil? time-slot) (zero? (count time-slot))))
  (let [on-dates-str (cond-> ""
                             (pos? (count on-dates))
                             (str "on: "
                                  (apply str (interpose ", " (map (comp t/show c/from-date) on-dates)))))
        ]
    (str on-dates-str)))

(defui ^:once TimeSlot
  static om/IQuery
  (query [this] [:time-slot/start-at :time-slot/end-at])
  Object
  (render [this]
    (let [{:keys [time-slot/start-at time-slot/end-at]} (om/props this)]
      (dom/label nil (str (t/show start-at) ", " (t/show end-at))))))
(def ui-timeslot (om/factory TimeSlot {:keyfn :time-slot/start-at}))

(defui ^:once Rule
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/rule-num :rule/source-bank :rule/target-account :rule/logic-operator :rule/conditions
                 :rule/on-dates {:rule/time-slot (om/get-query TimeSlot)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [db/id rule/permanent? rule/rule-num rule/source-bank rule/target-account rule/logic-operator rule/conditions]} props
          {:keys [rule-unselected]} (om/get-computed this)]
      (dom/div nil
               (if rule-unselected
                 (dom/div nil
                          (dom/label nil (str "Selector: " logic-operator))
                          (dom/br nil)
                          (dom/label nil (str "Rule Num: " rule-num))
                          (dom/br nil)
                          (dom/button #js {:onClick rule-unselected} "Back"))
                 (dom/div nil
                          (dom/label nil (str "Selector: " logic-operator))
                          (dom/br nil)
                          (dom/label nil (str "Rule Num: " rule-num))
                          (dom/br nil)))
               (dom/table #js {:className "table table-bordered table-sm table-hover"}
                          (dom/tbody nil (map ui-condition conditions)))))))
(def ui-rule (om/factory Rule {:keyfn :db/id}))

(defui ^:once RuleRow
  static om/Ident
  (ident [this props] [:rule/by-id (:db/id props)])
  static om/IQuery
  (query [this] [:db/id :rule/permanent? :rule/source-bank :rule/target-account :rule/logic-operator :rule/conditions
                 :rule/on-dates {:rule/time-slot (om/get-query TimeSlot)}])
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [db/id rule/permanent? rule/source-bank rule/target-account rule/logic-operator rule/conditions]} props
          {:keys [rule-selected]} (om/get-computed this)
          ;; permanent? is either true or nil
          permanent-display (if permanent? "Permanent" (display-on-dates props))
          ;source-bank-display (us/kw->string source-bank)
          ;target-account-display (us/kw->string target-account)
          logic-operator-display (if (= :single logic-operator) "" (us/kw->string logic-operator))
          ;num-conds (str (count conditions))
          ]
      (dom/tr #js {:onClick #(rule-selected id)}
              (dom/td #js {:className "col-md-2"} permanent-display)
              ;(dom/td #js {:className "col-md-2"} source-bank-display)
              ;(dom/td #js {:className "col-md-2"} target-account-display)
              (dom/td #js {:className "col-md-1"} logic-operator-display)
              ;(dom/td #js {:className "col-md-2"} num-conds)
              (dom/td #js {:className "col-md-12"} (dom/table #js {:className "table table-bordered table-sm table-hover"}
                                                              (dom/tbody nil (map ui-condition conditions))))))))
(def ui-rule-row (om/factory RuleRow {:keyfn :db/id}))

(def rule-table-header
  (dom/tr nil
          (dom/th #js {:className "col-md-2"} "When")
          ;(dom/th #js {:className "col-md-2"} "Source bank account")
          ;(dom/th #js {:className "col-md-2"} "Target ledger account")
          (dom/th #js {:className "col-md-1"} "Logic operator")
          ;(dom/th #js {:className "col-md-2"} "Num conditions")
          (dom/th #js {:className "col-md-12"} "Conditions")))

(defui ^:once RulesList
  static om/Ident
  (ident [this props] [:rules-list/by-id p/RULES_LIST])
  static om/IQuery
  (query [this] [:db/id :rules-list/label [:ui/selected-rule '_] [:ui/only-rule '_] {:rules-list/items (om/get-query RuleRow)}])
  static uc/InitialAppState
  (initial-state [comp-class {:keys [id label]}]
    {:db/id            id
     :rules-list/label label
     :rules-list/items []})
  Object
  (render [this]
    (let [{:keys [db/id rules-list/label ui/selected-rule ui/only-rule rules-list/items]} (om/props this)
          rule-selected (fn [id]
                          (us/log (str "Clicked on " id))
                          (om/transact! this `[(cljs-ops/selected-rule {:selected-ident [:rule/by-id ~id]})]))
          rule-unselected #(om/transact! this `[(cljs-ops/un-select-rule)])]
      (dom/div nil
               (if (or selected-rule only-rule)
                 (ui-rule (om/computed (or selected-rule only-rule) {:rule-unselected (when selected-rule rule-unselected)}))
                 (when (pos? (count items))
                   (dom/div nil
                            (dom/label nil (str "Num matching rules: " (count items) " (click to select)"))
                            ;(dom/label nil (str "ID: " id ", <" label "> " (count items)))
                            ;; table-inverse did not work
                            ;; table-striped doesn't work well with hover as same colour
                            (dom/table #js {:className "table table-bordered table-sm table-hover"}
                                       (dom/thead nil rule-table-header)
                                       (dom/tbody nil (map #(ui-rule-row (om/computed % {:rule-selected rule-selected})) items))))))))))
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
            :params        {:request/organisation organisation}}))

(defn banking-initial-state [id]
  {:db/id                            id
   :banking-form/bank-statement-line (uc/get-initial-state BankStatementLine {:id p/BANK_STATEMENT_LINE})
   :banking-form/existing-rules      (uc/get-initial-state RulesList {:id p/RULES_LIST :label "Invisible??"})
   :person                           nil})

;; Refreshing one higher fixes issue that when first selected type, target was auto-selected to first,
;; but the existing rules were not being displayed.
(defn load-existing-rules [comp organisation source-bank target-ledger]
  (df/load comp :my-existing-rules RuleRow
           {:target        help/rules-list-items-whereabouts
            :refresh       [[:banking-form/by-id p/BANKING_FORM]]
            :params        {:request/organisation organisation :source-bank source-bank :target-ledger target-ledger}
            :post-mutation `cljs-ops/rules-loaded}))

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
              {:person (om/get-query per/PersonForm)}])
  Object
  (render [this]
    (let [{:keys [ui/ledger-type banking-form/config-data banking-form/logic-operator banking-form/bank-statement-line
                  banking-form/target-ledger banking-form/existing-rules person] :as form} (om/props this)
          {:keys [bank-line/src-bank]} bank-statement-line
          {:keys [config-data/ledger-accounts]} config-data
          matching-rules-count (-> existing-rules :rules-list/items count)]
      (dom/div #js {:className "form-horizontal"}
               (dom/label nil (str "Target ledger: " target-ledger))
               #_(dom/label nil (str "DEBUG - Ledger type is " ledger-type
                                     ", and count of existing: " (-> existing-rules :rules-list/items count)))
               (fh/field-with-label this form :ui/ledger-type
                                    "Type"
                                    {:label-width-css "col-sm-2"
                                     :onChange        (fn [evt]
                                                        (let [new-val (u/target-kw evt)]
                                                          (when (and #_(not= new-val :type/personal)
                                                                  (help/ledger-type->desc new-val))
                                                            (om/transact! this `[(cljs-ops/config-data-for-target-ledger-dropdown
                                                                                   {:acct-type      ~new-val
                                                                                    :sub-query-comp ~RuleRow
                                                                                    :src-bank       ~src-bank
                                                                                    :person-form    ~per/PersonForm})]))))})
               (if (and ledger-type (not (#{no-pick :type/personal} ledger-type)))
                 (fh/field-with-label this form :banking-form/target-ledger
                                      (str "Target " (help/ledger-type->desc ledger-type))
                                      {:label-width-css "col-sm-2"
                                       :onChange        (fn [evt]
                                                          (let [new-target-ledger-val (u/target-kw evt)]
                                                            (us/log-on (str "src bank: " src-bank ", target ledger: " new-target-ledger-val))
                                                            (load-existing-rules this :seaweed src-bank new-target-ledger-val)))})
                 (comment "Only diff is 2nd dropdown" (when (= ledger-type :type/personal)
                                                        (dom/button nil "SAVE"))))
               ;Remember that :banking-form is initially how are querying for a rule, that might become the new rule, or
               ; might become editing one of the existing rules. [Select Existing]
               ;whereas :rule is one of many possibilities may want to start editing
               ;(fh/field-with-label this form :banking-form/logic-operator "Selector")
               (if (and (zero? matching-rules-count) (not (#{no-pick #_:type/personal} ledger-type)))
                 (dom/div nil
                          (dom/label nil (str "No matching rules for " (help/ledger-kw->account-name target-ledger)
                                              " from " (help/bank-kw->bank-name src-bank)
                                              ". You need to create a new rule:"))
                          (if person
                            (dom/div nil
                                     (dom/label nil (str "ledger type: " ledger-type))
                                     (per/ui-person-form person))
                            (us/log-off "Better create new person")))
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