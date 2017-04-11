(ns accounting.core
  (:require [accounting.meta :as meta]
            [accounting.util :as u]
            [accounting.convert :as c]
            [accounting.rules :as r]
            [clojure.string :as s]))

(def amp first)
(def coy second)
(def visa u/third)
(def -current {:bank   (visa meta/bank-accounts)
               :period {:year    2017
                        :quarter :q3}})

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
(defn record-maker [objs]
  (chk-seq objs)
  (fn [line]
    (chk-seq line)
    (->> line
         (map convert objs)
         (filter identity)
         (make-map (remove c/leave-outs (map :field-kw objs))))))

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
        make-record (record-maker heading-objs)]
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

(defn first-without-single-rule-match [bank-accounts periods]
  (let [rules (r/bank-rules bank-accounts)
        matcher (partial r/rule-matches rules)
        records (import-records periods bank-accounts)]
    (first (for [record records
                 :let [rule-matches (matcher record)]
                 :when (not (= 1 (count rule-matches)))]
             [record (mapv :target-account rule-matches)]))))


;;
;; When we know there's one rule for each we can run this. One for each is enough to get
;; a list of transactions for each account, which we do in a later step.
;; All we needed from the rule is the target account
;;
(defn attach-rules [bank-accounts periods]
  (let [rules (r/bank-rules bank-accounts)
        matcher (partial r/rule-matches rules)
        records (import-records periods bank-accounts)]
    (for [record records
          :let [[rule & tail] (matcher record)
                account (:target-account rule)]
          :when (not= account :trash)]
      (do
        (assert (empty? tail))
        [account record]))))

(defn produce-account-transactions [matched-records]
  )

(defn x-2 []
  (u/pp (first-without-single-rule-match (set meta/bank-accounts) [(:period -current)])))

(defn transactions [bank-accounts periods]
  (->> (attach-rules bank-accounts periods)
       (group-by first)
       (map (fn [[k v]] [k (sort-by :date (map second v))]))
       ))

(defn accounts-summary [transactions]
  (->> transactions
       (map (fn [[account records]] [account (reduce + (map :amount records))]))
       ))

(defn x-3 []
  (->> (transactions (set meta/bank-accounts) [(:period -current)])
       (take 10)
       u/pp))

(defn x-4 []
  (->> (transactions (set meta/bank-accounts) [(:period -current)])
       (accounts-summary)
       (sort-by (comp - u/abs second))
       u/pp))

