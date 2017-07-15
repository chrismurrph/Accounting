(ns accounting.core
  (:require [accounting.data.meta.common :as common-meta]
            [accounting.util :as u]
            [accounting.convert :as c]
            [accounting.match :as m]
            #_[accounting.data.meta.seaweed :as m]
            [clojure.string :as s]
            [accounting.gl :as gl]
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
        (assert false (str "ERR: " err-msg ", FIELD: " field-value))
        (do
          ;; Doesn't accept commas. Note we won't be using read-string forever
          ;(println "CONV: " field-value)
          (convert-fn field-value))))))

(defn make-map [kws xs]
  (zipmap (map c/in->out-kw kws) xs))

;; A record is a vector of maps - a parsed line
(defn record-maker [bank-account objs]
  (chk-seq objs)
  (fn [line]
    (chk-seq line)
    (assert (= (count objs) (count line))
            (str "Incompatible lengths: " (mapv :field-kw objs) ", line:" line))
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
(defn parse-csv [headings bank-account lines]
  (assert (not (keyword? headings)) headings)
  (let [heading-objs (map c/heading->parse-obj headings)
        _ (assert (seq heading-objs) (str "No headings yet for " bank-account))
        make-record (record-maker bank-account heading-objs)]
    (map make-record lines)))

#_(defn -slurp-raw-data->csv-old! [customer-kw bank]
  (fn [period]
    (let [file-path ((common-meta/bank-period->file-name (common-meta/human-meta customer-kw)) bank period)]
      (->> (slurp file-path)
           s/split-lines
           (map u/line->csv)))))

(defn slurp-raw-data->csv-datomic! [org-meta period-type bank-file-name actual-period]
  (let [file-path (common-meta/bank-period->file-name org-meta period-type bank-file-name actual-period)]
    (->> (slurp file-path)
         s/split-lines
         (map u/line->csv))))

#_(defn slurp-raw-data->csv! [customer-kw bank periods]
  (let [converter (-slurp-raw-data->csv-datomic! customer-kw bank)]
    (mapcat converter periods)))

#_(defn import-bank-records! [customer-kw periods bank-account]
  (->> (slurp-raw-data->csv! customer-kw bank-account periods)
       (parse-csv bank-account)))

;(defn import-bank-records! [customer-kw periods bank-accounts]
;  (let [importer (partial -import-bank-records! customer-kw periods)]
;    (mapcat importer bank-accounts)))
;(defn import-bank-records-datomic! [customer-kw periods bank-accounts]
;  (let [importer (partial -import-bank-records! customer-kw periods)]
;    (mapcat importer bank-accounts)))

(defn records-without-single-rule-match [{:keys [bank-accounts bank-records]} rules-in]
  (let [rules (m/bank-rules bank-accounts rules-in)
        _ (assert (seq rules) (str "No rules found for " bank-accounts " from " (count rules-in)
                                   ", first: " (:rule/source-bank (first rules-in))))
        ;_ (u/pp rules)
        matcher (partial m/records-rule-matches rules)
        ;records (import-bank-records! customer-kw periods bank-accounts)
        ]
    (for [record bank-records
          :let [rule-matches (matcher record)]
          :when (and (not= 1 (count rule-matches))
                     ;(not-clear-override? record rule-matches)
                     )]
      [(t/show-trans-record record) (mapv :rule/target-account rule-matches)])))

;; Because out formula matches on whole amounts, this sort of thing can stuff us up:
;; 14/03/2017,"20.00","DEPOSIT - CASH"
;; 14/03/2017,"575.00","DEPOSIT - CASH"
;; So we make these two entries above into one.
;;
(defn compact-trans [transactions]
  (let [summed-amounts (reduce + (map :out/amount transactions))]
    (assoc (first transactions) :out/amount summed-amounts)))

(defn compact-transactions [transactions]
  ;(println "first of" (count transactions) "is\n" (-> transactions first t/show-trans-record u/pp-str))
  ;(println "partitioned count: " (count (partition-by (juxt :out/date :out/desc) transactions)))
  (->> transactions
       (partition-by (juxt :out/date :out/desc :out/src-bank #_:out/dest-account))
       (map compact-trans)))

(def correct-keys #{:db/id :out/date :out/amount :out/desc :out/src-bank})
;;
;; Used for reporting, when your data is 'all good'! This kind of talk is an artifact of manual processing.
;; In reality we will silo the imported transactions. In fact in database each can have a rule-matched-against
;; attribute. And only bank statement lines with that attribute will be used in reporting.
;; This function is what we will use to fill that attribute!
;;
;; When we know there's one rule for each we can run this. One for each is enough to get
;; a list of transactions for each account, which we do in a later step.
;; All we needed from the rule is the target account
;;
(defn attach-rules [bank-records rules]
  (let [matcher (partial m/records-rule-matches rules)
        ;records (import-bank-records! customer-kw periods bank-accounts)
        ]
    (for [record bank-records
          :let [[rule & tail :as matched-rules] (matcher record)
                target-account (:rule/target-account rule)]
          ;; :personal is an event that affects the bank balance of non-company accounts
          ;; With :personal taken into account non-company bank balances will be true from period to period
          ;; :when (not= target-account :personal)
          ]
      (do
        (assert (every? correct-keys (keys record)) (str "Record has wrong keys: " (keys record)))
        ;; `first-without-single-rule-match` is designed to find this out, so start using it!
        (assert rule (str "No rule will match: <" (select-keys record [:out/desc :out/src-bank]) #_", " #_record ">"))
        (u/assrt (empty? tail) (str "Don't expect more than one rule per record\nRULE:\n" (u/pp-str rule) "\nTAIL:\n" (u/pp-str tail) "\nFOR:\n" (u/pp-str (t/show-trans-record record))))
        (assert target-account (str "rule has no :rule/target-account: <" rule ">"))
        [target-account (assoc record :out/dest-account target-account)]))))

(defn trial-balance [{:keys [bank-records]} rules splits starting-balances]
  ;(assert ((complement set?) bank-accounts))
  (let [transactions (->> (attach-rules bank-records rules)
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          compact-transactions)]
    (:gl (reduce (partial gl/apply-trans {:splits splits}) starting-balances transactions))))

;;
;; Everything did in order, by account, so can be lots...
;;
(defn account-grouped-transactions [{:keys [bank-accounts bank-records]} rules]
  (->> (attach-rules bank-records (m/bank-rules (set bank-accounts) rules))
       (group-by first)
       (map (fn [[k v]] [k (sort-by :out/date (map second v))]))))

(defn accounts-summary [transactions]
  (->> transactions
       (map (fn [[account records]]
              [account (reduce + (map :out/amount records))]))))

