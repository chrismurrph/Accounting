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
            [:bank-anz-coy :ato-payment]       [{:field          :desc
                                                 :logic-operator :or
                                                 :conditions     [[:starts-with "ANZ INTERNET BANKING BPAY TAX OFFICE PAYMENT"]
                                                                  [:starts-with "PAYMENT TO ATO"]]}]
            [:bank-anz-coy :drawings]          [{:field          :desc
                                                 :logic-operator :and
                                                 :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                                  [:ends-with "4509499246191003"]]}
                                                {:field          :desc
                                                 :logic-operator :and
                                                 :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                                  [:ends-with "CHRISTOPHER MURP"]]
                                                 }]})

(defn bank-rules [bank]
  (let [rules (filter (fn [[[src-bank _] v]]
                        (= src-bank bank)) rules)]
    (mapcat (fn [[[_ target-acct] bank-rules]]
              (map #(assoc % :target-account target-acct) bank-rules)) rules)))

(defn starts-with? [starts-with]
  (fn [field-value]
    (s/starts-with? field-value starts-with)))

(defn ends-with? [ends-with]
  (fn [field-value]
    (s/ends-with? field-value ends-with)))

(defn equals? [equals]
  (fn [field-value]
    (= field-value equals)))

(def condition-functions
  {:starts-with starts-with?
   :ends-with   ends-with?
   :equals      equals?})

(defn make-many-preds-fn [preds-fn conditions]
  (let [fs (map (comp condition-functions first) conditions)
        _ (assert (empty? (filter nil? fs)) (str "Missing functions from: " conditions))
        preds (map (fn [f match-text]
                     (f match-text)) fs (map second conditions))]
    (apply preds-fn preds)))

;;
;; Return the rule if there's a match against it
;; Conditions is a vector of vectors, eg: [[:starts-with "TRANSFER FROM R T WILSON"]]
;;
(defn match [record {:keys [field logic-operator conditions] :as rule}]
  (assert field)
  (let [field-value (field record)
        f (condp = logic-operator
            :single (let [_ (assert (= 1 (count conditions)))
                          [how-kw match-text] (first conditions)]
                      (assert how-kw)
                      (assert match-text)
                      ((condition-functions how-kw) match-text))
            :and (make-many-preds-fn every-pred conditions)
            :or (make-many-preds-fn some-fn conditions)
            (assert false (str "Only :single :and - got: <" logic-operator ">")))
        _ (assert f (str "Not found a function for " rule))]
    (when (f field-value)
      rule)))

(defn rule-matches [rules record]
  (keep (partial match record) rules))

(defn x-1 []
  (u/pp (bank-rules :bank-anz-coy)))
