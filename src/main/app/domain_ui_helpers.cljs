(ns app.domain-ui-helpers
  (:require [om.dom :as dom]
            [fulcro.ui.forms :as f]
            [app.util :as u]
            [app.panels :as p]
            [app.forms-helpers :as fh]
            [cljc.domain-helpers :as dhs]))

;; Sample validator that requires there be at least two words
(defmethod f/form-field-valid? `name-valid? [_ value args]
  (let [trimmed-value (clojure.string/trim value)]
    (clojure.string/includes? trimmed-value " ")))

(defmethod f/form-field-valid? `us-phone? [sym value args]
  (seq (re-matches #"[(][0-9][0-9][0-9][)] [0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]" value)))

(def report-kw->report-name
  {:report/profit-and-loss "Profit & Loss"
   :report/balance-sheet   "Balance Sheet"
   :report/trial-balance   "Trial Balance"
   :report/big-items-first "Biggest first"})

(defn make-example-bank-line [id]
  {:db/id              id
   :bank-line/src-bank :bank/anz-visa
   :bank-line/date     "24/08/2016"
   :bank-line/desc     "OFFICEWORKS SUPERSTO      KESWICK"
   :bank-line/amount   0.00M})

#_(defn make-condition [id [field predicate subject]]
  {:db/id               id
   :condition/field     field
   :condition/predicate predicate
   :condition/subject   subject})

(def ledger-type->desc
  {:type/exp      "Expense"
   :type/non-exp  "Non-Expense"
   :type/income   "Income"
   :type/personal "Personal"
   :type/liab     "Liability"})

(def ledger-type-options
  [(f/option :type/exp "Expense")
   (f/option :type/non-exp "Non-Expense")
   (f/option :type/income "Income")
   ;; C/be done with checkbox. Sep ui/personal? then we don't need to display the
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
   (f/option :type/personal "Personal")
   (f/option :type/liab "Liability")])

(def logic-options
  [(f/option :single "")
   (f/option :and "AND")
   (f/option :or "OR")])

(defn bank-kw->bank-name [kw]
  (and kw (-> kw
              name
              (clojure.string/replace #"-" " ")
              clojure.string/upper-case)))

;;
;; Not yet worth the effort, but the idea was any short words
;; to be upper-cased, because seeing ATO much better than Ato.
;; Probably later we won't just be doing everything off keywords
;;
(comment (defn upper-or-capitalize [s]
           (if (> (count s) 3)
             (clojure.string/capitalize s)
             (clojure.string/upper-case s))))

(defn ledger-kw->account-name [kw]
  (and kw (-> kw
              name
              (clojure.string/replace #"-" " ")
              clojure.string/capitalize)))

(def years-options-generator (fh/options-generator
                               dhs/range-of-years
                               #(f/option (keyword (str %)) (str %))
                               #(-> % first str keyword)))

(def period-kw->period-name
  {:q1 "Q1" :q2 "Q2" :q3 "Q3" :q4 "Q4"
   :jan  "Jan" :feb "Feb" :mar "Mar"
   :apr  "Apr" :may "May" :jun "Jun"
   :jul  "Jul" :aug "Aug" :sep "Sep"
   :oct  "Oct" :nov "Nov" :dec "Dec"})

(def periods-options-generator (fh/options-generator
                                 dhs/range-of-periods
                                 #(f/option % (period-kw->period-name %))
                                 last))

(def reports-options-generator (fh/options-generator
                                 #(:organisation/possible-reports %2)
                                 #(f/option % (report-kw->report-name %))
                                 first))

(def ledger-type->first-selection
  {:type/exp     :exp/office-expense
   :type/non-exp :non-exp/ato-payment
   :type/income  :income/bank-interest
   :type/liab    :liab/drawings})

(defn select-auto-selected [xs ledger-type]
  (->> xs
       (filter (fn [kw] (= (ledger-type->first-selection ledger-type) kw)))
       first))

(def target-account-options-generator (fh/options-generator
                                        (fn [_ list] list)
                                        #(f/option % (ledger-kw->account-name %))
                                        select-auto-selected))

;;
;; Useful for things like changing options in fields in panels
;;
(def request-form-ident [:user-request/by-id p/USER_REQUEST_FORM])
(def year-field-whereabouts (conj request-form-ident :request/year))
(def period-field-whereabouts (conj request-form-ident :request/period))
(def report-field-whereabouts (conj request-form-ident :request/report))
(def executable-field-whereabouts (conj request-form-ident :request/manually-executable?))

(def request-form-input-options (partial fh/input-options request-form-ident))
(def year-options-whereabouts (request-form-input-options :request/year))
(def period-options-whereabouts (request-form-input-options :request/period))
(def report-options-whereabouts (request-form-input-options :request/report))

(def request-form-input-default-value (partial fh/input-default-value request-form-ident))
(def year-default-value-whereabouts (request-form-input-default-value :request/year))
(def period-default-value-whereabouts (request-form-input-default-value :request/period))
(def report-default-value-whereabouts (request-form-input-default-value :request/report))

(def banking-form-ident [:banking-form/by-id p/BANKING_FORM])
(def banking-form-config-data-whereabouts (conj banking-form-ident :banking-form/config-data))
(def banking-form-rule-whereabouts (conj banking-form-ident :banking-form/creating-rule))
; Not where they go:
;(def banking-form-existing-rules-whereabouts (conj banking-form-ident :banking-form/existing-rules))

(def rules-list-ident [:rules-list/by-id p/RULES_LIST])
(def rules-list-items-whereabouts (conj rules-list-ident :rules-list/items))

;; ui/ are supposed to be kept at the top level
#_(def rules-list-selected-rule (conj rules-list-ident :ui/selected-rule))
(def selected-rule [:ui/selected-rule])
(def only-rule [:ui/only-rule])

;
;(def source-bank-field-whereabouts (conj banking-form-ident :banking-form/source-bank))
(def target-account-field-whereabouts (conj banking-form-ident :banking-form/target-ledger))

(def banking-form-input-options (partial fh/input-options banking-form-ident))
(def source-bank-options-whereabouts (banking-form-input-options :banking-form/source-bank))
(def target-account-options-whereabouts (banking-form-input-options :banking-form/target-ledger))

(def banking-form-input-default-value (partial fh/input-default-value banking-form-ident))
(def source-bank-default-value-whereabouts (banking-form-input-default-value :banking-form/source-bank))
(def target-account-default-value-whereabouts (banking-form-input-default-value :banking-form/target-ledger))

;;
;; When the user changes the year we need to rebuild the quarters (or months i.e. periods)
;;
(def period-dropdown-rebuilder
  (fh/dropdown-rebuilder
    period-field-whereabouts period-options-whereabouts period-default-value-whereabouts))

(def year-dropdown-rebuilder
  (fh/dropdown-rebuilder
    year-field-whereabouts year-options-whereabouts year-default-value-whereabouts))

(def report-dropdown-rebuilder
  (fh/dropdown-rebuilder
    report-field-whereabouts report-options-whereabouts report-default-value-whereabouts))

#_(def source-bank-dropdown-rebuilder
    (fh/dropdown-rebuilder
      source-bank-field-whereabouts source-bank-options-whereabouts source-bank-default-value-whereabouts))

(def target-account-dropdown-rebuilder
  (fh/dropdown-rebuilder
    target-account-field-whereabouts target-account-options-whereabouts target-account-default-value-whereabouts))

;;
;; REPORT
;;

;;
;; In reality we might have the name of the report but faded out, only to be
;; full when the data has returned.
;;
(def report-placeholder "report placeholder ...")
(def report-ident [:ledger-item-list/by-id p/LEDGER_ITEMS_LIST])
(def report-title-whereabouts (conj report-ident :ledger-item-list/label))
(def report-items-whereabouts (conj report-ident :ledger-item-list/items))

;;
;; Done from post-report, hence
;; now that the report's data is on the screen we'll set its title - pessimistic update
;;
(defn set-report-title [st]
  (let [rep-kw (get-in st report-field-whereabouts)
        rep-name (rep-kw report-kw->report-name)]
    (assoc-in st report-title-whereabouts rep-name)))

;;
;; Need to do when change anything, even when execute report. We never want the user to see a title
;; for a report that is not correct.
;;
(defn blank-out-report [st]
  ;(println "Blanked out")
  (-> st
      (assoc-in report-title-whereabouts report-placeholder)
      (assoc-in report-items-whereabouts [])))

(defn sort-selected-items-by*
  "Sort the idents in the selected-items ledger-item list. Returns the new app-state."
  [st field]
  (let [items (get-in st report-items-whereabouts [])
        selected-items (map (fn [item-ident] (get-in st item-ident)) items)
        sorted-selected-items (sort-by field selected-items)
        new-idents (mapv (fn [item] [:ledger-item/by-id (:db/id item)]) sorted-selected-items)]
    ;(println (str "SORTED by " field " -> " new-idents))
    (assoc-in st report-items-whereabouts new-idents)))


