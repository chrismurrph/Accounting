(ns accounting.match
  (:require [clojure.string :as s]
            [accounting.util :as u]
            [accounting.meta :as meta]
            [accounting.rules-data :as d]
            [accounting.time :as t]))

;;
;; Filter so only get the rules of certain bank accounts
;;
(defn bank-rules [bank-accounts in-rules]
  (assert (set? bank-accounts))
  (let [rules (filter (fn [rule]
                        (bank-accounts (:rule/source-bank rule))) in-rules)]
    rules))

(defn -starts-with? [starts-with]
  (fn [field-value]
    (s/starts-with? field-value starts-with)))

(defn -ends-with? [ends-with]
  (fn [field-value]
    (s/ends-with? field-value ends-with)))

(defn -equals? [equals]
  (fn [field-value]
    (= field-value equals)))

(defn -contains? [includes]
  (fn [field-value]
    (s/includes? field-value includes)))

(defn local-some-fn [])

(def condition-functions
  {:starts-with -starts-with?
   :ends-with   -ends-with?
   :equals      -equals?
   :contains    -contains?})

;;
;; preds-fn is either every-pred or some-fn, both hofs,
;; so here the function to be called is returned.
;;
(defn make-many-preds-fn [preds-fn conditions]
  (assert (seq conditions))
  (let [hofs (map (comp condition-functions first) conditions)
        _ (assert (empty? (filter nil? hofs)) (str "Missing functions from: " conditions))
        preds (map (fn [f match-text]
                     (f match-text)) hofs (map second conditions))]
    (apply preds-fn preds)))

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
             (t/within-range? start end (:out/date record))))
       (or (nil? amount) (= amount (:out/amount record)))))

;;
;; Return the rule if there's a match against it
;; Conditions is a vector of vectors, eg: [[:starts-with "TRANSFER FROM R T WILSON"]]
;;
(defn match [record {:keys [field logic-operator conditions] :as rule}]
  (assert (map? rule) (str "rule is suppoed to be a map, got: " rule))
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
                            f (condition-functions how-kw)
                            _ (assert f (str "Unrecognised condition: " how-kw))]
                        (f match-text))
              :and (make-many-preds-fn every-pred conditions)
              :or (make-many-preds-fn some-fn conditions)
              (assert false (str "Only :single :and - got: <" logic-operator ">")))
          _ (assert f (str "Not found a function for " rule))]
      (when-let [res (f field-value)]
        rule))))

;;
;; When 2 matches come through one may have been set as the dominator. If the one dominates the
;; other we can return it. Otherwise return 2 as would normally.
;; If, of all the competing matches, many dominte then we have no way of knowing which the most
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
