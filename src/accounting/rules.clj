(ns accounting.rules
  (:require [clojure.string :as s]
            [accounting.util :as u]))

;;
;; From which bank account tells you which account to put the transaction to
;; The result of applying these rules will be a list of transactions at the
;; target account.
;;
(def rules {[:bank-anz-coy :mining-sales]      [{:field          :desc
                                                 :logic-operator :single
                                                 :conditions     [[:starts-with "TRANSFER FROM MINES RESCUE PTY CS"]]}]
            [:bank-anz-coy :poker-parse-sales] [{:field          :desc
                                                 :logic-operator :single
                                                 :conditions     [[:starts-with "TRANSFER FROM R T WILSON"]]}]
            [:bank-anz-coy :bank-interest]     [{:field          :desc
                                                 :logic-operator :single
                                                 :conditions     [[:starts-with "CREDIT INTEREST PAID"]]}]
            [:bank-anz-coy :bank-fee]          [{:field          :desc
                                                 :logic-operator :single
                                                 :conditions     [[:equals "ACCOUNT SERVICING FEE"]]}]
            [:bank-anz-coy :ato-payment]       [#_{:field           :desc
                                                   :logic-operation :or
                                                   :starts-with     ["ANZ INTERNET BANKING BPAY TAX OFFICE PAYMENT" "PAYMENT TO ATO"]}]
            [:bank-anz-coy :drawings]          [#_{:field           :desc
                                                   :logic-operation :and
                                                   :starts-with     "ANZ INTERNET BANKING FUNDS TFER TRANSFER"
                                                   :ends-with       "4509499246191003"}
                                                #_{:field          :desc
                                                   :logic-operator :and
                                                   :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"] [:ends-with "CHRISTOPHER MURP"]]
                                                   }]})

(defn bank-rules [bank]
  (let [rules (filter (fn [[[src-bank _] v]]
                        (= src-bank bank)) rules)]
    (mapcat (fn [[[_ target-acct] bank-rules]]
              (map #(assoc % :target-account target-acct) bank-rules)) rules)))

(defn only-starts-with? [field-value starts-with]
  (s/starts-with? field-value starts-with))

(defn equals? [field-value equals]
  (= field-value equals))

(def rule-functions
  {:starts-with only-starts-with?
   :ends-with   nil
   :equals      equals?})

;;
;; Return the rule if there's a match against it
;;
(defn match [record {:keys [field logic-operator conditions] :as rule}]
  (assert field)
  (let [field-value (field record)
        [f match-text] (if (= :single logic-operator)
                         (let [_ (assert (= 1 (count conditions)))
                               [how-kw match-text] (first conditions)]
                           (assert how-kw)
                           (assert match-text)
                           [(rule-functions how-kw) match-text])
                         (assert false (str "Only singles")))
        _ (assert f (str "Not found a function for " rule))]
    (when (f field-value match-text)
      rule)))

(defn rule-matches [rules record]
  (keep (partial match record) rules))

(defn x-1 []
  (u/pp (bank-rules :bank-anz-coy)))
