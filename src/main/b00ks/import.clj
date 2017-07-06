(ns b00ks.import
  (:require [datomic.api :as d]
            [untangled.datomic.schema :as s]
            [b00ks.migrations.initial-20170705 :as current]
            [accounting.data.meta.seaweed :as seasoft]
            [accounting.util :as u])
  (:import (java.security MessageDigest)))

(defn find-account [accounts kw]
  (let [[in-ns in-name] ((juxt namespace name) kw)]
    (first (filter
             (fn [{:keys [account/category
                          account/name]}]
               (and (= (keyword (str "account.category/" in-ns))
                       category)
                    (= in-name name)))
             accounts))))

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
   :import-template/account      (find-account accounts k)
   })

(def seaweed-software-org
  {:db/id                         (d/tempid :db.part/user)
   :base/type                     :organisation
   :organisation/name             "Seaweed Software Pty Ltd"
   :organisation/org-type         :organisation.org-type/tax
   :organisation/import-data-root seasoft/import-data-root
   })

(defn make-account-proportion [accounts [k v]]
  {:db/id                         (d/tempid :db.part/user)
   :base/type                     :account-proportion
   :account-proportion/account    (find-account accounts k)
   :account-proportion/proportion v
   })

(defn make-split [accounts [name account-proportions]]
  {:db/id                     (d/tempid :db.part/user)
   :base/type                 :split
   :split/name                name
   :split/account-proportions (mapv (partial make-account-proportion accounts) account-proportions)
   })

(defn make-tax-year [year]
  {:db/id                     (d/tempid :db.part/user)
   :base/type                 :tax-year
   :split/year                year
   })

(defn create-account [kw]
  (let [[ns name] ((juxt namespace name) kw)
        category (keyword (str "account.category/" ns))]
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

(def bank-accounts (mapv create-account seasoft/all-accounts))
(def ledger-accounts (mapv create-account seasoft/all-accounts))

(defn imp []
  (d/delete-database db-uri)
  (d/create-database db-uri)
  (let [ledger-accounts (mapv create-account seasoft/ledger-accounts)
        bank-accounts (mapv create-account seasoft/bank-accounts)
        all-accounts (concat ledger-accounts bank-accounts)
        organisation (assoc seaweed-software-org :organisation/splits
                                                 (mapv (partial make-split all-accounts)
                                                       seasoft/splits)
                                                 :organisation/import-templates
                                                 (mapv (partial make-import-template all-accounts)
                                                       seasoft/import-templates)
                                                 :organisation/tax-years
                                                 (mapv make-tax-year seasoft/tax-years)
                                                 )
        c (d/connect db-uri)]
    @(d/transact c (s/generate-schema current/db-schema))
    (doseq [a (attrs current/db-schema)]
      (assert (not (nil? (d/entity (d/db c) a)))))
    (let [groups
          [{:db/id (d/tempid :db.part/user) :base/type :group :group/name "Admin"}]
          owner
          {:db/id       (d/tempid :db.part/user)
           :base/type   :user
           :person/name "Bob"
           :auth/pwd    (sha "bobby2015")                   ;; use bcrypt for real please:
           ;; https://github.com/xsc/pandect or https://github.com/weavejester/crypto-password
           :auth/login  "bob@example.com"}]
      @(d/transact c (concat groups [owner] ledger-accounts bank-accounts))
      )))
