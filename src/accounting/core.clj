(ns accounting.core
  (:require [accounting.meta :as meta]
            [accounting.util :as u]
            [accounting.convert :as c]
            [accounting.match :as m]
            [accounting.rules-data :as rd]
            [clojure.string :as s]
            [accounting.gl :as gl]
            [accounting.rules-data :as d]
            [accounting.time :as t]))

(defn chk-seq [xs]
  (assert xs)
  (assert (seq xs)))

(defn convert [{:keys [field-kw validate-fn convert-fn]} field-value]
  (when (-> field-kw c/leave-outs not)
    (assert field-kw (str "No :field-kw to convert: <" field-value ">"))
    (assert convert-fn (str "No :convert-fn for: <" field-kw "> to convert: <" field-value ">"))
    (let [err-msg (validate-fn field-value)]
      (if err-msg
        (assert false err-msg)
        (convert-fn field-value)))))

(defn make-map [kws v]
  (zipmap (map c/in->out-kw kws) v))

;; A record is a vector of maps - a parsed line
(defn record-maker [bank-account objs]
  (chk-seq objs)
  (fn [line]
    (chk-seq line)
    (as-> line $
          (u/probe-off $)
          (map convert objs $)
          (filter identity $)
          (make-map (remove c/leave-outs (map :field-kw objs)) $)
          (assoc $ :out/src-bank bank-account))))

;;
;; Given a bank account we can get a structure. The structure shows us how to parse each field so
;; we can create a hash with right keys where vals have correct types.
;; Not hof because intention is that concat lines together from many periods
;;
(defn parse-csv [bank-account lines]
  (let [headings (c/bank-account-headings bank-account)
        _ (assert (every? c/all-headings headings) (str "New heading introduced: " headings ", expected: " c/all-headings))
        heading-objs (map c/heading->parse-obj headings)
        _ (assert (seq heading-objs) (str "No headings yet for " bank-account))
        make-record (record-maker bank-account heading-objs)]
    (map make-record lines)))

(defn -raw-data->csv [bank]
  (fn [period]
    (let [file-path (meta/bank-period->file-name bank period)]
      (->> (slurp file-path)
           s/split-lines
           (map u/line->csv)))))

(defn raw-data->csv [bank periods]
  (let [converter (-raw-data->csv bank)]
    (mapcat converter periods)))

(defn -import-records [periods bank-account]
  (->> (raw-data->csv bank-account periods)
       (parse-csv bank-account)))

(defn import-records [periods bank-accounts]
  (let [importer (partial -import-records periods)]
    (mapcat importer bank-accounts)))

(defn show [record]
  (let [date (-> record :out/date t/format-date)]
    (assoc record :out/date date)))

;;
;; Intention to have `equal` being better then `starts-with`, but would be too much
;; work. Better for the problem to come out to the user, and the user to decide which
;; one overrides. We can make an override group and a sequence in it.
;;
#_(defn not-clear-override? [rec rule-matches]
  (when (= 2 (count rule-matches))
    (u/pp rec)
    (u/pp rule-matches))
  true)

(defn first-without-single-rule-match [bank-accounts periods rules-in]
  (let [rules (m/bank-rules bank-accounts rules-in)
        ;_ (u/pp rules)
        matcher (partial m/records-rule-matches rules)
        records (import-records periods bank-accounts)]
    (first (for [record records
                 :let [rule-matches (matcher record)]
                 :when (and (not= 1 (count rule-matches))
                            ;(not-clear-override? record rule-matches)
                            )]
             [(show record) (mapv :rule/target-account rule-matches)]))))

;;
;; When we know there's one rule for each we can run this. One for each is enough to get
;; a list of transactions for each account, which we do in a later step.
;; All we needed from the rule is the target account
;;
(defn attach-rules [bank-accounts periods rules]
  (let [matcher (partial m/records-rule-matches rules)
        records (import-records periods bank-accounts)]
    (for [record records
          :let [[rule & tail :as matched-rules] (matcher record)
                target-account (:rule/target-account rule)]
          ;; :personal is an event that affects the bank balance of non-company accounts
          ;; With :personal taken into account non-company bank balances will be true from period to period
          ;;:when (not= target-account :personal)
          ]
      (do
        ;; `first-without-single-rule-match` is designed to find this out, so start using it!
        (assert rule (str "No rule will match: <" (select-keys record [:out/desc :out/src-bank]) ">"))
        (u/assrt (empty? tail) (str "Don't expect more than one rule per record:\n" (u/pp-str rule) (u/pp-str tail)))
        (assert target-account (str "rule has no :rule/target-account: <" rule ">"))
        [target-account (assoc record :out/dest-account target-account)]))))

(defn account-grouped-transactions [bank-accounts periods rules]
  (->> (attach-rules bank-accounts periods (m/bank-rules bank-accounts rules))
       (group-by first)
       (map (fn [[k v]] [k (sort-by :out/date (map second v))]))
       ))

(defn accounts-summary [transacts]
  (->> transacts
       (map (fn [[account records]]
              [account (reduce + (map :out/amount records))]))))

