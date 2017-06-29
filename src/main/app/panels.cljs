(ns app.panels)

;;
;; While there is only one of them they are kept here. When a panel starts to be used
;; multiple times it gets a proper ident with a :db/id, and disappears from here.
;; Note that forms require a :db/id, even although there is often only one case.
;; They can still be put here, we just feed in the value with initial data.
;;
(def POTENTIAL_DATA 'POTENTIAL-DATA)
(def CONFIG_DATA 'CONFIG-DATA)
(def LEDGER_ITEMS_LIST 'LEDGER-ITEMS-LIST)
(def RULES_LIST 'RULES-LIST)
(def USER_REQUEST_FORM 'USER-REQUEST-FORM)
(def BANK_STATEMENT_LINE 'BANK-STATEMENT-LINE)
(def RULE_FORM 'RULE-FORM)
(def BOOKKEEPING 'BOOKKEEPING)
(def BANKING 'BANKING)
