(ns accounting.entities
  (:require [datomic.api :as d]
            [clj-time.coerce :as c]
            [accounting.util :as u]))

;;
;; Unlikely to be importing monthly, asserting here so know to change this function
;;
(defn make-period [quarter]
  {:db/id          (d/tempid :db.part/user)
   :base/type      :period
   :period/type    :quarterly
   :period/quarter quarter})

(defn make-actual-period [{:keys [actual-period/tax-year actual-period/year actual-period/quarter actual-period/month] :as actual-period}]
  (assert (or tax-year year) "Need to make all data in terms of actual periods")
  (assert (nil? month) (str "Unlikely to be importing monthly: <" actual-period ">"))
  (assert quarter)
  {:db/id                 (d/tempid :db.part/user)
   :base/type             :actual-period
   :actual-period/year    (or tax-year year)
   :actual-period/quarter quarter
   :actual-period/type    :quarterly
   })

(defn make-account [kw]
  (let [[ns name] ((juxt namespace name) kw)
        category (keyword ns)]
    (assert name)
    (assert category)
    ;(println category (type category))
    {:db/id            (d/tempid :db.part/user)
     :base/type        :account
     :account/category category
     :account/desc     name
     :account/name     name
     ; No reason to repeat when the organisation began
     ;:account/time-slot (make-time-slot [beginning-period nil])
     ;; Do reverse if need this, an organisation has many accounts
     ;:account/organisation (assoc seaweed-software-org :organisation/splits
     ;                                                  (mapv make-split seasoft/splits))
     }))

(defn make-heading [n kw]
  {:db/id           (d/tempid :db.part/user)
   :base/type       :heading
   :heading/key     kw
   :heading/ordinal n
   ;; don't need anything more
   })

(defn make-bank-account [[heading structure]]
  (assert (vector? structure))
  (let [account (make-account heading)]
    (assoc account :bank-account/headings (vec (map-indexed make-heading structure))
                   :bank-account/statements [])))

(defn read-account [conn account-name account-category]
  (assert account-name)
  (let [db (d/db conn)]
    (d/q '[:find ?a .
           :in $ ?an ?ac
           :where
           [?a :account/name ?an]
           [?a :account/category ?ac]
           ] db account-name account-category)))

(defn make-line-item [{:keys [out/date out/amount out/desc] :as record}]
  ;(println record)
  {:db/id            (d/tempid :db.part/user)
   :base/type        :line-item
   :line-item/date   (u/err-nil (c/to-date date))
   :line-item/amount amount
   :line-item/desc   desc
   })

;;
;; Does all the checking and updates the bank account. If nothing was done
;; because the statement already exits then returns nil.
;;
(defn make-statement [conn {:keys [bank-acct-name actual-period records]}]
  (let [db (d/db conn)
        bank-acct-id (read-account conn bank-acct-name :bank)
        existing-periods (->> (d/pull db [{:bank-account/statements
                                           [{:statement/actual-period
                                             [:actual-period/year :actual-period/quarter
                                              :actual-period/month :actual-period/type]}]}]
                                      bank-acct-id)
                              :bank-account/statements
                              (map :statement/actual-period)
                              (into #{}))]
    (when (not (existing-periods actual-period))
      ;(println "PERIODS: " existing-periods)
      ;(println "Existing period: " actual-period)
      (assert bank-acct-id (str "Entity is blank for " bank-acct-name))
      ;(println "ID: " bank-acct-id)
      (let [new-statement {:db/id                   (d/tempid :db.part/user)
                           :base/type               :statement
                           ;:statement/bank-account  bank-acct-id
                           :statement/actual-period (make-actual-period actual-period)
                           :statement/line-items    (mapv make-line-item records)
                           }
            new-account (update (d/pull db [:db/id] bank-acct-id) :bank-account/statements conj new-statement)]
        @(d/transact conn [new-account])))))