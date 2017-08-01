(ns app.cljs-operations
  (:require
    [fulcro.client.mutations :as m :refer [defmutation]]
    [om.next :as om]
    [app.domain-ui-helpers :as help]
    [app.util :as u]
    [cljc.utils :as us]
    [fulcro.ui.forms :as f]
    [app.forms-helpers :as fh]
    [app.om-helpers :as oh]
    [clojure.set :as set]
    [fulcro.client.data-fetch :as df]
    [fulcro.client.core :as uc]
    [app.panels :as p]))

;;
;; Only when the report is done do we show its title properly. Consider going from grayed out to black.
;;
(defmutation post-report
  [no-params]
  (action [{:keys [state]}]
          (swap! state #(-> %
                            help/set-report-title
                            (assoc-in help/executable-field-whereabouts false)))))

(defmutation enable-report-execution
  [no-params]
  (action [{:keys [state]}]
          (swap! state assoc-in help/executable-field-whereabouts true)))

;;
;; Touch any of the inputs and the report is no longer valid, so remove it from the screen
;;
(defmutation touch-report
  [no-params]
  (action [{:keys [state]}]
          (swap! state help/blank-out-report)))

;;
;; Assumes that state has already been changed, so that year-field-whereabouts has what the user has just chosen
;; Changes the period options to those that are possible ... to those that reflect the data available
;;
(defmutation year-changed
  [no-params]
  (action [{:keys [state]}]
          (let [st @state
                ident [:potential-data/by-id p/POTENTIAL_DATA]
                potential-data (get-in st ident)
                selected-year (get-in st help/year-field-whereabouts)
                [selected-period period-options] (help/periods-options-generator potential-data selected-year)
                ]
            (swap! state #(-> %
                              (help/period-dropdown-rebuilder selected-period period-options))))))

(defmutation post-potential-data
  [no-params]
  (action [{:keys [state]}]
          (let [st @state
                ident (:my-potential-data-new st)
                potential-data (get-in st ident)
                [selected-year year-options] (help/years-options-generator potential-data nil)
                [selected-period period-options] (help/periods-options-generator potential-data selected-year)
                [selected-report report-options] (help/reports-options-generator potential-data nil)]
            (us/log-off (str "year: " selected-report))
            (us/log-off (str "year options: " year-options))
            (us/log-off (str "period options: " period-options))
            (swap! state #(-> %
                              help/blank-out-report
                              (help/year-dropdown-rebuilder selected-year year-options)
                              (help/period-dropdown-rebuilder selected-period period-options)
                              (help/report-dropdown-rebuilder selected-report report-options)
                              (dissoc :my-potential-data-new))))))

;;
;; We already know which bank account the money is coming from. User is picking the account
;; the money is going to. But first picking the type of that account.
;;
(def always-remove #{"bank"})
(def type->what-remove
  {:type/exp     (set/union always-remove #{"income" "non-exp" "personal" "liab"})
   :type/non-exp (set/union always-remove #{"income" "exp" "personal" "liab"})
   ;; personal doesn't even have a drop down.
   ;; :type/personal (set/union always-remove #{"income" "non-exp" "exp" "liab"})
   :type/income  (set/union always-remove #{"personal" "non-exp" "exp" "liab"})
   :type/liab    (set/union always-remove #{"personal" "non-exp" "exp" "income"})
   })

(defn make-rule []
  {:db/id               (oh/make-temp-id "make-rule")
   :rule/logic-operator :single
   :rule/conditions     []})

(defmutation add-condition-1
  [{:keys [id rule condition-form]}]
  (action [{:keys [state]}]
          ;newly created one will have a tempid
          (assert id)
          (let [new-condition (f/build-form condition-form {:db/id               id
                                                            :condition/field     :out/desc
                                                            :condition/predicate :equals
                                                            :condition/subject   ""
                                                            })
                rule-ident [:rule/by-id rule]
                condition-ident (om/ident condition-form new-condition)]
            (swap! state assoc-in condition-ident new-condition)
            (uc/integrate-ident! state condition-ident :prepend (conj rule-ident :rule/conditions)))))

;;
;; Making a new condition and putting it in its own table then putting the ident within the rule.
;; So why isn't the rule being marked as dirty?
;; Doc says:
;; A form is considered dirty? when:
;; Any field of the form or its declared subforms has a value different from the initial (or most recently committed) value.
;; Any form or subform in a set has an Om tempid (e.g. server remaps have not yet taken effect)
;;
;; Here the second of those statements must be true.
(defmutation add-condition-2
  [{:keys [id rule condition-form]}]
  (action [{:keys [state]}]
          ;newly created one will have a tempid
          (assert id)
          (assert (om/tempid? id))
          (let [new-condition (f/build-form condition-form
                                            {:db/id               id
                                             :condition/field     :out/desc
                                             :condition/predicate :equals
                                             :condition/subject   (get-in @state [:bank-line/by-id p/BANK_STATEMENT_LINE :bank-line/desc])
                                             })
                rule-ident [:rule/by-id rule]
                condition-ident (om/ident condition-form new-condition)]
            (swap! state assoc-in condition-ident new-condition)
            (uc/integrate-ident! state condition-ident :replace (conj rule-ident :rule/upserting-condition)))))

(def conditions-count-sorter (oh/sort-idents 10
                                             (fn [m] [:rule/by-id (:db/id m)])
                                             (comp count :rule/conditions)))

;; rule-form -> really want that as parameter -> instead I've had to put it at top level in state, under
;; :global-form/rule-form
;; Used the word 'rule-form' because it is never going to change and can always be there to be picked by by others
(defmutation rules-loaded
  [{:keys [no-params]}]
  (action [{:keys [state]}]
          (let [st @state
                rules-count (-> (get-in st help/rules-list-items-whereabouts) count)
                _ (println "rules-loaded, rules count: " rules-count)]
            (condp = rules-count
              0 (let [form-f (fh/->form (get @state :global-form/rule-form)
                                        (make-rule)
                                        help/banking-form-rule-whereabouts
                                        [help/only-rule help/selected-rule])]
                  (swap! state form-f))
              1 (swap! state #(-> %
                                  (assoc-in help/only-rule (first (get-in st help/rules-list-items-whereabouts)))
                                  (assoc-in help/selected-rule nil)))
              (swap! state #(-> %
                                (assoc-in help/only-rule nil)
                                (assoc-in help/selected-rule nil)
                                (update-in help/rules-list-items-whereabouts
                                           (fn [rules] (conditions-count-sorter % rules)))))))))

(defn ledger-drop-down [st acct-type]
  (if (= acct-type :type/personal)
    [nil nil nil]
    (let [to-remove (type->what-remove acct-type)
          _ (assert to-remove (str "No set found from <" acct-type ">"))
          config-ident [:config-data/by-id p/CONFIG_DATA]
          {:keys [config-data/ledger-accounts]} (get-in st config-ident)
          ledger-accounts (remove #(to-remove (namespace %)) ledger-accounts)
          [selected-target-account target-account-options] (help/target-account-options-generator ledger-accounts acct-type)]
      [ledger-accounts selected-target-account target-account-options])))

;;
;; If user has selected :type/personal then the next drop down is not even going to appear
;;
(defmutation config-data-for-target-ledger-dropdown
  [{:keys [sub-query-comp acct-type src-bank rule-form]}]
  (action [{:keys [state]}]
          (assert sub-query-comp (us/assert-str "sub-query-comp" sub-query-comp))
          (assert acct-type (us/assert-str "acct-type" acct-type))
          (assert src-bank (us/assert-str "src-bank" src-bank))
          (println (str "config data, if have, for " acct-type))
          (let [personal-key (keyword (str "personal/" (name src-bank)))
                [ledger-accounts selected-target-account target-account-options] (ledger-drop-down @state acct-type)]
            (if (not= :type/personal acct-type)
              (let [alphabetic-target-account-options (sort-by :option/label target-account-options)
                    ;; On this one we will need to do the same event as if the user had selected it
                    ;; Hmm - that involves a load which I don't want to do from a mutation
                    ;; See below for how to do the load - load-action does everything load can do
                    ;; Interestingly this whole mutation now becomes remote
                    ;; This is not 'chaining' because there is only one remote call
                    ]
                (assert (pos? (count ledger-accounts)))
                (swap! state help/target-account-dropdown-rebuilder selected-target-account alphabetic-target-account-options))
              (swap! state assoc-in help/target-account-field-whereabouts personal-key))
            ;; 'rules-loaded (see post-mutation below) doesn't take a parameter so have to put one in here
            (swap! state assoc :global-form/rule-form rule-form)
            (let [st @state
                  org-ident [:organisation/by-id p/ORGANISATION]
                  {:keys [organisation/key]} (get-in st org-ident)]
              ;; If no rules have been loaded from the server then we need to create one for the
              ;; user to fill out. Hence the post mutation here:
              (df/load-action state :my-existing-rules sub-query-comp
                              {:target        help/rules-list-items-whereabouts
                               :refresh       [[:banking-form/by-id p/BANKING_FORM]]
                               :params        {:source-bank          src-bank
                                               :target-ledger        (if (= :type/personal acct-type)
                                                                       personal-key
                                                                       selected-target-account)
                                               :request/organisation key}
                               :post-mutation 'app.cljs-operations/rules-loaded
                               }))))
  (remote [env] (df/remote-load env)))

#_(defmutation select-rule-old
    [{:keys [selected-ident]}]
    (action [{:keys [state]}]
            (swap! state assoc-in help/selected-rule selected-ident)))

(defmutation select-rule
  [{:keys [selected-ident]}]
  (action [{:keys [state]}]
          (let [st @state
                form-f (fh/->form (get st :global-form/rule-form)
                                  (get-in st selected-ident)
                                  help/selected-rule)]
            (swap! state #(-> %
                              ;; just for debugging
                              ;; (fh/make-links-nil help/selected-rule)
                              form-f)))))

(defmutation un-select-rule
  [{:keys [selected]}]
  (action [{:keys [state]}]
          (swap! state assoc-in help/selected-rule nil)))

(defmutation unruly-bank-statement-line
  [no-params]
  (action [{:keys [state]}]
          (swap! state #(-> %
                            (dissoc :my-unruly-bank-statement-line)
                            (assoc-in help/selected-rule nil)
                            (assoc-in help/only-rule nil)))))

;;
;; Use when have done a big query from Datomic, and all results normalised. However there
;; are intended edges that are empty that need to be filled. (If not empty then need to be
;; overwritten).
;;
(defmutation target-neighborhoods
  [no-params]
  (action [{:keys [state]}]
          (let [st @state
                to-transfer (-> st :district-query vals first :neighborhood/_district)]
            (swap! state assoc-in [:results-list/by-id 'RESULTS_LIST_PANEL :neighborhoods] to-transfer))))
