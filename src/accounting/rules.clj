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
            [:bank-anz-coy :ato-payment]       [{:field       :desc
                                                 :starts-with "PAYMENT TO ATO"}]
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
  ;(println "only-starts-with?:" field-value starts-with)
  (when (s/starts-with? field-value starts-with)
    true))

(defn starts-and-ends-with? [field-value {:keys [starts-with ends-with] :as rule}]
  (when (and (s/starts-with? field-value starts-with) (s/ends-with? field-value ends-with))
    true))

(defn match [record {:keys [field logic-operation starts-with ends-with] :as rule}]
  (assert field)
  (let [field-value (field record)
        f (cond
            (and (nil? ends-with) starts-with) only-starts-with?
            (and ends-with starts-with (= :and logic-operation)) starts-and-ends-with?
            :default (assert false (str "No function found suitable for " rule)))]
    (when f
      (f field-value rule))))

(defn matches [rules record]
  (keep (partial match record) rules))

(defn x-1 []
  (u/pp (bank-rules :bank-anz-coy)))
