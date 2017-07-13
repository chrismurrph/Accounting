(ns accounting.entities
  (:require [datomic.api :as d]
            [clj-time.coerce :as c]
            [accounting.util :as u]
            [cljc.utils :as us]
            [accounting.queries :as q]))

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

(defn make-line-item [{:keys [out/date out/amount out/desc] :as record}]
  ;(println record)
  {:db/id            (d/tempid :db.part/user)
   :base/type        :line-item
   :line-item/date   (u/err-nil (c/to-date date))
   :line-item/amount amount
   :line-item/desc   desc
   })

(defn quarter->number [quarter]
  (Integer/parseInt (subs (str quarter) 2)))

(defn statement-comparator [one two]
  (let [{year-one :actual-period/year quarter-one :actual-period/quarter type-one :actual-period/type} one
        {year-two :actual-period/year quarter-two :actual-period/quarter type-two :actual-period/type} two
        _ (assert year-one (us/assert-str "one" one))
        _ (assert year-two (us/assert-str "two" two))
        years-diff (- year-one year-two)]
    (assert (= type-one type-two :quarterly))
    (if (zero? years-diff)
      (if (= quarter-one quarter-two)
        0
        (- (quarter->number quarter-one) (quarter->number quarter-two)))
      years-diff)))

(defn update-organisations-ordinal [conn org-key new-current-ordinal]
  (let [db (d/db conn)
        org-id (q/read-organisation conn org-key)
        new-org (assoc (d/pull db [:db/id] org-id) :organisation/current-ordinal new-current-ordinal)]
    @(d/transact conn [new-org])))

;;
;; Does all the checking and updates the bank account. If nothing was done
;; because the statement already exists then returns nil. All the action is
;; here because the UI is almost always going to be at a particular period and
;; for a particular bank account.
;;
(defn make-statement [conn {:keys [bank-acct-name actual-period records]}]
  (let [db (d/db conn)
        bank-acct-id (q/read-account conn bank-acct-name :bank)
        existing-periods (->> (d/pull db [{:bank-account/statements
                                           [{:statement/actual-period
                                             [:actual-period/year :actual-period/quarter
                                              :actual-period/month :actual-period/type]}]}]
                                      bank-acct-id)
                              :bank-account/statements
                              (map :statement/actual-period)
                              (into (sorted-set-by statement-comparator)))]
    (when (not (existing-periods actual-period))
      (let [all-periods (conj existing-periods actual-period)
            idx (u/index-of actual-period all-periods)]
        (assert bank-acct-id (str "Entity is blank for " bank-acct-name))
        (let [new-statement {:db/id                   (d/tempid :db.part/user)
                             :base/type               :statement
                             :statement/ordinal       idx
                             :statement/actual-period (make-actual-period actual-period)
                             :statement/line-items    (mapv make-line-item records)
                             }
              new-account (update (d/pull db [:db/id] bank-acct-id) :bank-account/statements conj new-statement)]
          @(d/transact conn [new-account]))))))