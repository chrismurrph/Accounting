(ns accounting.rules
  (:require [clojure.string :as s]
            [accounting.util :as u]))

;;
;; From which bank account tells you which account to put the transaction to
;; The result of applying these rules will be a list of transactions at the
;; target account.
;;
(def rules {[:bank-anz-coy :mining-sales]      [{:field       :desc
                                                 :starts-with "TRANSFER FROM MINES RESCUE PTY CS"}]
            [:bank-anz-coy :poker-parse-sales] [{:field       :desc
                                                 :starts-with "TRANSFER FROM R T WILSON"}]
            [:bank-anz-coy :bank-interest]     [{:field       :desc
                                                 :starts-with "CREDIT INTEREST PAID"}]
            [:bank-anz-coy :bank-fee]          [{:field  :desc
                                                 :equals "ACCOUNT SERVICING FEE"}]
            [:bank-anz-coy :ato-payment]       [{:field           :desc
                                                 :logic-operation :or
                                                 :starts-with     ["ANZ INTERNET BANKING BPAY TAX OFFICE PAYMENT" "PAYMENT TO ATO"]}]
            [:bank-anz-coy :drawings]          [{:field           :desc
                                                 :logic-operation :and
                                                 :starts-with     "ANZ INTERNET BANKING FUNDS TFER TRANSFER"
                                                 :ends-with       "4509499246191003"}
                                                {:field           :desc
                                                 :logic-operation :and
                                                 :starts-with     "ANZ INTERNET BANKING FUNDS TFER TRANSFER"
                                                 :ends-with       "CHRISTOPHER MURP"}]})

(defn bank-rules [bank]
  (let [rules (filter (fn [[[src-bank _] v]]
                        (= src-bank bank)) rules)]
    (mapcat (fn [[[_ target-acct] bank-rules]]
              (map #(assoc % :target-account target-acct) bank-rules)) rules)))

(defn only-starts-with? [field-value {:keys [starts-with]}]
  (assert (complement (vector? starts-with)))
  ;(println "only-starts-with?:" field-value starts-with)
  (s/starts-with? field-value starts-with))

(defn many-or-starts-with? [field-value {:keys [starts-with]}]
  (assert (vector? starts-with))
  (let [f (partial s/starts-with? field-value)]
    (seq (filter identity (map f starts-with)))))

(defn starts-and-ends-with? [field-value {:keys [starts-with ends-with] :as rule}]
  (assert (complement (vector? starts-with)))
  (assert (complement (vector? ends-with)))
  (and (s/starts-with? field-value starts-with) (s/ends-with? field-value ends-with)))

(defn equals? [field-value {:keys [equals] :as rule}]
  (assert (complement (vector? equals)))
  (= field-value equals))

(defn match [record {:keys [field logic-operation starts-with ends-with equals] :as rule}]
  (assert field)
  (let [field-value (field record)
        f (cond
            (and (= :or logic-operation) (nil? equals) (nil? ends-with) (vector? starts-with)) many-or-starts-with?
            (and equals (nil? starts-with) (nil? ends-with)) equals?
            (and (nil? equals) (nil? ends-with) starts-with) only-starts-with?
            (and (nil? equals) ends-with starts-with (= :and logic-operation)) starts-and-ends-with?
            :default (assert false (str "No function found suitable for " rule)))]
    (when (f field-value rule)
      rule)))

(defn rule-matches [rules record]
  (keep (partial match record) rules))

(defn x-1 []
  (u/pp (bank-rules :bank-anz-coy)))
