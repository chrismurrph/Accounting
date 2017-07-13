(ns b00ks.import
  (:require [datomic.api :as d]
            [accounting.datomic-api :as ada]
            [untangled.datomic.schema :as s]
            [b00ks.migrations.initial-20170705 :as current]
            [accounting.data.meta.seaweed :as seasoft]
            [accounting.data.seaweed :as seasoft-data]
            [accounting.seasoft-context :as seasoft-context]
            [accounting.entities :as e]
            [accounting.util :as u]
            [accounting.time :as t]
            [clj-time.coerce :as c])
  (:import (java.security MessageDigest)))

(defn find-account [accounts kw]
  (assert (seq accounts))
  (let [[in-ns in-name] ((juxt namespace name) kw)
        res (->> accounts
                 (filter
                   (fn [{:keys [account/category
                                account/name]}]
                     (and (= (keyword in-ns)
                             category)
                          (= in-name name))))
                 first)]
    (assert res (str "No account: " kw #_", ALL: " #_(u/pp-str accounts)))
    res))

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn attrs [db-schema]
  (->> db-schema
       (mapcat
         (fn [{:keys [namespace fields]}]
           (map (juxt (constantly namespace) first) fields)))
       (map (partial apply keyword))))

(defn sha [x]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (doto digest
      (.reset)
      (.update (.getBytes x)))
    (.getBytes
      (apply str (map (partial format "%02x") (.digest digest))))))

(defn make-import-template [accounts [k v]]
  (assert (seq accounts))
  {:db/id                        (d/tempid :db.part/user)
   :base/type                    :import-template
   :import-template/template-str v
   :import-template/account      (find-account accounts k)})

(defn make-timespan [start-period end-period]
  {:db/id                      (d/tempid :db.part/user)
   :base/type                  :timespan
   :timespan/commencing-period (e/make-actual-period start-period)
   :timespan/latest-period     (e/make-actual-period end-period)})

(def seaweed-software-org
  {:db/id                         (d/tempid :db.part/user)
   :base/type                     :organisation
   :organisation/key              :seaweed
   :organisation/period-type      :quarterly
   :organisation/timespan         (make-timespan (first seasoft-context/total-range)
                                                 (last seasoft-context/total-range))
   :organisation/name             "Seaweed Software Pty Ltd"
   :organisation/org-type         :tax
   :organisation/possible-reports [:report/trial-balance :report/big-items-first
                                   :report/profit-and-loss :report/balance-sheet]
   :organisation/import-data-root seasoft/import-data-root})

(defn make-account-proportion [accounts [k v]]
  (assert v)
  {:db/id                         (d/tempid :db.part/user)
   :base/type                     :account-proportion
   :account-proportion/account    (find-account accounts k)
   :account-proportion/proportion v})

(defn make-split [accounts [key account-proportions]]
  (assert key)
  {:db/id                     (d/tempid :db.part/user)
   :base/type                 :split
   :split/key                 key
   :split/account-proportions (mapv (partial make-account-proportion accounts) account-proportions)})

(defn make-tax-year [year]
  (assert year)
  {:db/id         (d/tempid :db.part/user)
   :base/type     :tax-year
   :tax-year/year year})

(defn make-time-slot [time-slot]
  (assert time-slot)
  (let [[begin-date end-date] time-slot]
    (assert begin-date)
    (cond->
      {:db/id              (d/tempid :db.part/user)
       :base/type          :time-slot
       :time-slot/start-at (u/err-nil (c/to-date begin-date))}
      end-date (assoc :time-slot/end-at (u/err-nil (c/to-date end-date))))))

(defn make-condition [[field predicate subject]]
  (assert subject)
  (let [
        ;field-kw (keyword (str "condition.field/" (name field)))
        ;predicate-kw (keyword (str "condition.predicate/" (u/kw->string predicate)))
        ]
    {:db/id               (d/tempid :db.part/user)
     :base/type           :condition
     :condition/field     field
     :condition/predicate predicate
     :condition/subject   subject}))

(defn make-rule
  [accounts {:keys [logic-operator conditions rule/source-bank rule/target-account
                    dominates time-slot rule/period on-dates]}]
  (cond->
    {:db/id               (d/tempid :db.part/user)
     :base/type           :rule
     :rule/logic-operator logic-operator
     :rule/dominates      (mapv (partial find-account accounts) dominates)
     :rule/conditions     (mapv make-condition conditions)
     :rule/source-bank    (find-account accounts source-bank)
     :rule/target-account (find-account accounts target-account)
     :rule/on-dates       (if on-dates (vec on-dates) [])}
    time-slot
    (assoc :rule/time-slot (make-time-slot time-slot))
    period
    (assoc :rule/actual-period (e/make-actual-period period))))

(def to-import-accounts
  {:exp      (mapv e/make-account seasoft/exp-accounts)
   :non-exp  (mapv e/make-account seasoft/non-exp-accounts)
   :income   (mapv e/make-account seasoft/income-accounts)
   :personal (mapv e/make-account seasoft/personal-accounts)
   :liab     (mapv e/make-account seasoft/liab-accounts)
   :equity   (mapv e/make-account seasoft/equity-accounts)
   :asset    (mapv e/make-account seasoft/asset-accounts)
   :bank     (mapv e/make-bank-account
                   (->> accounting.convert/bank-account-headings
                        ;; there are 4 and last is for croquet
                        (take 3)))
   :split    (mapv e/make-account (keys seasoft/splits))
   })

(defn amend-org-with-individual-accounts [org]
  (let [{:keys [exp non-exp income personal liab equity asset bank split]} to-import-accounts]
    (-> org
        (assoc :organisation/exp-accounts exp)
        (assoc :organisation/non-exp-accounts non-exp)
        (assoc :organisation/income-accounts income)
        (assoc :organisation/personal-accounts personal)
        (assoc :organisation/liab-accounts liab)
        (assoc :organisation/equity-accounts equity)
        (assoc :organisation/asset-accounts asset)
        (assoc :organisation/bank-accounts bank)
        (assoc :organisation/split-accounts split))))

(defn amend-org-misc [all-accounts org]
  (let [rule-make (partial make-rule all-accounts)]
    (-> org
        (assoc :organisation/splits
               (mapv (partial make-split all-accounts) seasoft/splits)
               :organisation/import-templates
               (mapv (partial make-import-template all-accounts) seasoft/import-templates)
               :organisation/tax-years
               (mapv make-tax-year seasoft/tax-years)
               :organisation/rules
               (mapv rule-make seasoft-data/all-rules)
               ))))

(defn delete-all []
  (d/delete-database db-uri))

(defn datomic-import []
  ;Doesn't seem to work, use delete-all above
  ;(d/delete-database db-uri)
  (d/create-database db-uri)
  (let [commencing (-> seaweed-software-org :organisation/timespan :timespan/commencing-period)
        _ (assert commencing)
        _ (println commencing)
        all-accounts (->> to-import-accounts vals (apply concat))
        ;rules (mapv rule-make seasoft-data/all-rules)
        org-amend (partial amend-org-misc all-accounts)
        organisation (-> seaweed-software-org
                         amend-org-with-individual-accounts
                         org-amend)
        conn (d/connect db-uri)]
    @(d/transact conn (s/generate-schema current/db-schema))
    (doseq [a (attrs current/db-schema)]
      (assert (seq (d/entity (d/db conn) a))))
    (let [groups
          [{:db/id (d/tempid :db.part/user) :base/type :group :group/name "Admin"}]
          owner
          {:db/id       (d/tempid :db.part/user)
           :base/type   :user
           :person/name "Bob"
           ;; use bcrypt for real please:
           :auth/pwd    (sha "bobby2015")
           ;; https://github.com/xsc/pandect or
           ;; https://github.com/weavejester/crypto-password
           :auth/login  "bob@example.com"}
          ]
      @(d/transact conn (concat groups [owner organisation] all-accounts)))
    (ada/import-bank-statements)
    (ada/set-default-current-ordinal)))
