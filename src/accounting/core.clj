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

(defn first-without-single-rule-match [bank-account periods]
  (let [rules (r/bank-rules bank-account)
        matcher (partial r/rule-matches rules)
        records (->> (raw-data->csv bank-account periods)
                     (parse-csv bank-account))]
    (first (for [record records
                 :let [rule-matches (matcher record)]
                 :when (not (= 1 (count rule-matches)))]
             [record (mapv :target-account rule-matches)]))))

(defn x-1 []
  (let [bank-account (:bank -current)
        periods [(:period -current)]
        lines (raw-data->csv bank-account periods)]
    (parse-csv bank-account lines)))

(defn x-2 []
  (let [bank-account (:bank -current)
        periods [(:period -current)]]
    (u/pp (first-without-single-rule-match bank-account periods))))
