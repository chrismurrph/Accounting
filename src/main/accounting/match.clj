(ns accounting.match
  (:require [clojure.string :as s]
            [accounting.util :as u]
            [accounting.data.seaweed :as d]
            [accounting.time :as t]
            [accounting.data.common :as c]))

;;
;; Filter so only get the rules of certain bank accounts
;;
(defn bank-rules [bank-accounts in-rules]
  (assert (set? bank-accounts))
  (let [rules (filter #(bank-accounts (:rule/source-bank %)) in-rules)]
    rules))

;;
;; Filter so only get the rules of certain bank accounts
;;
(defn filter-rules [bank-accounts ledger-accounts in-rules]
  (assert (set? bank-accounts))
  (assert (set? ledger-accounts))
  (assert (seq bank-accounts))
  (assert (seq ledger-accounts))
  (let [rules (filter #(and (bank-accounts (:rule/source-bank %)) (ledger-accounts (:rule/target-account %))) in-rules)]
    rules))

;;   Only matches if they were chosen
;;   :on-dates
;;   :between-dates-inclusive
;;   :amount
;;   - already has :period
(defn matches-chosen-specifics? [record {:keys [period on-dates between-dates-inclusive amount] :as rule}]
  (and (or (nil? period) (t/within-period? period (:out/date record)))
       (or (nil? on-dates) (t/in-set? on-dates (:out/date record)))
       (or (nil? between-dates-inclusive)
           (let [[start end] between-dates-inclusive]
             ((t/within-range-hof? start end) (:out/date record))))
       (or (nil? amount) (= amount (:out/amount record)))))

;;
;; preds-fn is either every-pred or some-fn, both hofs,
;; so here the function to be called is returned.
;; Example condition:
;; [:out/desc :starts-with "DIRECT CREDIT"]
;;
(defn make-many-preds-fn [preds-fn conditions]
  (assert (seq conditions))
  (let [hofs (map (comp c/condition-functions first) conditions)
        _ (assert (empty? (filter nil? hofs)) (str "Missing functions from: " conditions))
        preds (map (fn [f match-text]
                     (f match-text)) hofs (map second conditions))]
    (apply preds-fn preds)))

;;
;; Return the rule if there's a match against it
;; Conditions is a vector of vectors, eg: [[:starts-with "TRANSFER FROM R T WILSON"]]
;; This is old because an example condition now looks like this:
;; [:out/desc :starts-with "DIRECT CREDIT"]
;; The field is in each condition and no longer exists in the rule
;;
(defn match-old [record {:keys [field logic-operator conditions] :as rule}]
  (assert (map? rule) (str "rule is supposed to be a map, got: " rule))
  (assert field (str "No field in rule: " rule))
  (assert (map? record))
  (when (matches-chosen-specifics? record rule)
    (let [field-value (field record)
          _ (assert field-value (str "No field " field " in: " (keys record)))
          f (condp = logic-operator
              :single (let [_ (assert (= 1 (count conditions)) (str "More than 1 condition for single: " conditions))
                            [how-kw match-text] (first conditions)
                            _ (assert how-kw)
                            _ (assert match-text)
                            f (c/condition-functions how-kw)
                            _ (assert f (str "Unrecognised condition: " how-kw))]
                        (f match-text))
              :and (make-many-preds-fn every-pred conditions)
              :or (make-many-preds-fn some-fn conditions)
              (assert false (str "Only :single :and - got: <" logic-operator ">")))
          _ (assert f (str "Not found a function for " rule))]
      (when-let [res (f field-value)]
        rule))))

(defn chk-condition [[field how-kw match-text]]
  (assert field)
  (assert how-kw)
  (assert match-text))

(defn field-calculator-hof [record]
  (fn [[field how-kw match-text]]
    (let [f (c/condition-functions how-kw)]
      (assert f (str "Not found a function for: " how-kw))
      [(f match-text) (field record)])))

;; Return the rule if there's a match against it
;; A condition looks like:
;; [:out/desc :starts-with "DIRECT CREDIT"]
(defn match [record {:keys [logic-operator conditions] :as rule}]
  (assert (map? rule) (str "rule is supposed to be a map, got: " rule))
  (assert (map? record) (str "record is supposed to be a map, got: " record ", of type: " (type record)))
  (when (matches-chosen-specifics? record rule)
    (let [field-calculator (field-calculator-hof record)
          res (condp = logic-operator
                :single (let [_ (assert (= 1 (count conditions)) (str "More than 1 condition for single: " conditions))
                              [field how-kw match-text :as condition] (first conditions)
                              _ (chk-condition condition)
                              f (c/condition-functions how-kw)
                              _ (assert f (str "Unrecognised condition: " how-kw))]
                          ((f match-text) (field record)))
                :and (let [f-field-values (map field-calculator conditions)]
                       (->> f-field-values
                            (map (fn [[f value]] (f value)))
                            u/probe-off
                            (every? identity)))
                :or (let [f-field-values (map field-calculator conditions)]
                      (->> f-field-values
                           (some (fn [[f value]] (f value))))))]
      (when res
        rule))))

;;
;; When 2 matches come through one may have been set as the dominator. If the one dominates the
;; other we can return it. Otherwise return 2 as would normally.
;; If, of all the competing matches, many dominate then we have no way of knowing which the most
;; dominant. Then we would have to start using ordinality.
;;
(defn records-rule-matches [rules record]
  (let [xs (keep (partial match record) rules)
        res-count (count xs)]
    (if (not= 2 res-count)
      xs
      (let [[[dominator & more-dominators] [recessive & _]] (split-with :dominates xs)
            dominated-set (:dominates dominator)]
        (if (and (nil? more-dominators)
                 dominated-set
                 (dominated-set (:rule/target-account recessive)))
          [dominator]
          xs)))))
