(ns b00ks.import
  (:require [datomic.api :as d]
            [untangled.datomic.schema :as s]
            [b00ks.migrations.initial-20170705 :as current]
            [accounting.data.meta.seaweed :as seasoft]
            [accounting.data.seaweed :as seasoft-data]
            [accounting.util :as u])
  (:import (java.security MessageDigest)))

(defn find-account [accounts kw]
  (let [[in-ns in-name] ((juxt namespace name) kw)
        res (->> accounts
                 (filter
                   (fn [{:keys [account/category
                                account/name]}]
                     (and (= (keyword (str "account.category/" in-ns))
                             category)
                          (= in-name name))))
                 first)]
    (assert res (str "No account: " kw))
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
  {:db/id                        (d/tempid :db.part/user)
   :base/type                    :import-template
   :import-template/template-str v
   :import-template/account      (find-account accounts k)})

(def seaweed-software-org
  {:db/id                         (d/tempid :db.part/user)
   :base/type                     :organisation
   :organisation/name             "Seaweed Software Pty Ltd"
   :organisation/org-type         :organisation.org-type/tax
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

(defn make-account [kw]
  (let [[ns name] ((juxt namespace name) kw)
        category (keyword (str "account.category/" ns))]
    (assert name)
    (assert category)
    ;(println category (type category))
    {:db/id            (d/tempid :db.part/user)
     :base/type        :account
     :account/category category
     :account/desc     name
     :account/name     name
     ;; Do reverse if need this, an organisation has many accounts
     ;:account/organisation (assoc seaweed-software-org :organisation/splits
     ;                                                  (mapv make-split seasoft/splits))
     }))

(defn make-condition [[field predicate subject]]
  (assert subject)
  (let [field-kw (keyword (str "condition.field/" (name field)))
        predicate-kw (keyword (str "condition.predicate/" (u/kw->string predicate)))]
    {:db/id               (d/tempid :db.part/user)
     :base/type           :condition
     :condition/field     field-kw
     :condition/predicate predicate-kw
     :condition/subject   subject}))

(defn make-length-of-time [between-dates-inclusive]
  (assert between-dates-inclusive)
  (let [[begin-date end-date] between-dates-inclusive]
    (assert begin-date)
    (assert end-date)
    {:db/id                   (d/tempid :db.part/user)
     :base/type               :length-of-time
     :length-of-time/start-at begin-date
     :length-of-time/end-at   end-date}))

;;
;; Unlikely to be importing monthly, asserting here so know to change this function
;;
(defn make-period [quarter]
  {:db/id          (d/tempid :db.part/user)
   :base/type      :period
   :period/type    (keyword (str "period.type/" "quarterly"))
   :period/quarter (keyword (str "period.quarter/" (u/kw->string quarter)))})

(defn make-actual-period [{:keys [period/tax-year period/quarter] :as period}]
  (assert tax-year (str "Unlikely to be importing monthly: <" period ">"))
  (assert quarter)
  {:db/id                (d/tempid :db.part/user)
   :base/type            :actual-period
   :actual-period/year   tax-year
   :actual-period/period (make-period quarter)})

(defn make-rule
  [accounts {:keys [logic-operator conditions rule/source-bank rule/target-account
                    dominates between-dates-inclusive rule/period on-dates]}]
  (let [logic-operator-kw (keyword (str "rule.logic-operator/" (u/kw->string logic-operator)))]
    (cond->
      {:db/id                        (d/tempid :db.part/user)
       :base/type                    :rule
       :rule/logic-operator          logic-operator-kw
       :rule/dominates               (mapv (partial find-account accounts) dominates)
       :rule/conditions              (mapv make-condition conditions)
       :rule/source-bank             (find-account accounts source-bank)
       :rule/target-account          (find-account accounts target-account)
       :rule/on-dates                (if on-dates (vec on-dates) [])
       }
      between-dates-inclusive
      (assoc :rule/between-dates-inclusive (make-length-of-time between-dates-inclusive))
      period
      (assoc :rule/period (make-actual-period period)))))

(def to-import-accounts
  {:exp      (mapv make-account seasoft/exp-accounts)
   :non-exp  (mapv make-account seasoft/non-exp-accounts)
   :income   (mapv make-account seasoft/income-accounts)
   :personal (mapv make-account seasoft/personal-accounts)
   :liab     (mapv make-account seasoft/liab-accounts)
   :equity   (mapv make-account seasoft/equity-accounts)
   :asset    (mapv make-account seasoft/asset-accounts)
   :bank     (mapv make-account seasoft/bank-accounts)
   :split    (mapv make-account (keys seasoft/splits))
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

(defn amend-org [all-accounts org]
  (-> org
      (assoc :organisation/splits
             (mapv (partial make-split all-accounts) seasoft/splits)
             :organisation/import-templates
             (mapv (partial make-import-template all-accounts) seasoft/import-templates)
             :organisation/tax-years
             (mapv make-tax-year seasoft/tax-years))))

(defn datomic-import []
  (d/delete-database db-uri)
  (d/create-database db-uri)
  (let [all-accounts (->> to-import-accounts vals (apply concat))
        rule-make (partial make-rule all-accounts)
        rules (mapv rule-make seasoft-data/all-rules)
        org-amend (partial amend-org all-accounts)
        organisation (-> seaweed-software-org
                         amend-org-with-individual-accounts
                         org-amend)
        c (d/connect db-uri)]
    @(d/transact c (s/generate-schema current/db-schema))
    (doseq [a (attrs current/db-schema)]
      (assert (seq (d/entity (d/db c) a))))
    (let [groups
          [{:db/id (d/tempid :db.part/user) :base/type :group :group/name "Admin"}]
          owner
          {:db/id       (d/tempid :db.part/user)
           :base/type   :user
           :person/name "Bob"
           :auth/pwd    (sha "bobby2015")                   ;; use bcrypt for real please:
           ;; https://github.com/xsc/pandect or https://github.com/weavejester/crypto-password
           :auth/login  "bob@example.com"}
          ]
      @(d/transact c (concat groups [owner organisation] all-accounts rules)))))
