(ns accounting.core
  (:require [accounting.meta :as meta]
            [accounting.util :as u]
            [accounting.convert :as c]
            [clojure.string :as s]))

(def current {:bank    :amp
              :year    2017
              :quarter :q3})

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
  (zipmap kws v))

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
;;
(defn parse-csv [bank-account lines]
  (let [heading-objs (map c/heading->parse-obj (c/bank-account-headings bank-account))
        make-record (record-maker heading-objs)]
    (map make-record lines)))

(defn raw-data->csv []
  (let [{:keys [bank year quarter]} current
        file-path (meta/bank-period->file-name bank year quarter)]
    (->> (slurp file-path)
         s/split-lines
         (map u/line->csv))))

(defn x-1 []
  (let [lines (raw-data->csv)]
    (parse-csv :amp lines)))
