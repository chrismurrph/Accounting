(ns accounting.rules
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

(def condition-functions
  {:starts-with -starts-with?
   :ends-with   -ends-with?
   :equals      -equals?})

(defn make-many-preds-fn [preds-fn conditions]
  (assert (> (count conditions) 1))
  (let [hofs (map (comp condition-functions first) conditions)
        _ (assert (empty? (filter nil? hofs)) (str "Missing functions from: " conditions))
        preds (map (fn [f match-text]
                     (f match-text)) hofs (map second conditions))]
    (apply preds-fn preds)))

;;
;; Return the rule if there's a match against it
;; Conditions is a vector of vectors, eg: [[:starts-with "TRANSFER FROM R T WILSON"]]
;;
(defn match [record {:keys [field logic-operator conditions period] :as rule}]
  (assert (map? rule) (str "rule is suppoed to be a map, got: " rule))
  (assert field (str "No field in rule: " rule))
  (assert (map? record))
  (when (or (nil? period) (t/within-period? period (:out/date record)))
    (let [field-value (field record)
          _ (assert field-value (str "No field " field " in: " (keys record)))
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
        rule))))

(defn records-rule-matches [rules record]
  (keep (partial match record) rules))

(defn -merged-rules []
  (merge-with (comp vec concat) d/permanent-rules d/q3-2017-rules))

(defn canonicalise-rules [rules-in]
  (mapcat (fn [[[source-bank target-account] v]]
            (map #(assoc % :rule/source-bank source-bank :rule/target-account target-account) v)) rules-in))

;; Select one or the other
(def current-rules
  (let [initial-rules (-merged-rules) #_d/test-rules]
    (->> initial-rules
         canonicalise-rules)))
