(ns b00ks.migrations.initial-20170705
  (:require [datomic.api :as d]
            [untangled.datomic.schema :as s]))

(def parts
  [(s/part "app")])

(def db-schema
  [(s/schema base
             (s/fields
               ;[uuid :uuid :unique-identity]
               [type :keyword]
               ;[date :instant]
               ;[year :long]
               ))
   (s/schema time-slot
             (s/fields
               [start-at :instant]
               ;; end-at will often be optional
               [end-at :instant]))
   (s/schema tax-year
             (s/fields
               [year :long]))
   (s/schema heading
             (s/fields
               ;; Don't need anything but key. Use heading->parse-obj
               [key :keyword "Examples :in/long-date, :in/dollar-amount"]
               [ordinal :long]))
   (s/schema account
             (s/fields
               [category :keyword #_[:exp :liab :non-exp :personal :bank :income :equity :asset :split]]
               [name :string "Name of the account, for example \"bank-fee\""]
               [desc :string "Description of the account, for example \"Bank Fee\""]
               [time-slot :ref :one]))
   ;; We never create one of these, its just a schema
   (s/schema bank-account
             (s/fields
               [headings :ref :many]
               [statements :ref :many]))
   (s/schema account-balance
             (s/fields
               [account :ref :one]
               [amount :bigdec]))
   (s/schema line-item
             (s/fields
               [date :instant]
               [amount :bigdec]
               [desc :string]))
   (s/schema account-proportion
             (s/fields
               [account :ref :one]
               [proportion :bigdec]))
   (s/schema split
             (s/fields
               [key :keyword]
               [account-proportions :ref :many]))
   (s/schema period
             (s/fields
               [type :keyword #_[:quarterly :monthly]]
               [month :keyword #_[:jan :feb :mar :apr :may :jun :jul :aug :sep :oct :nov :dec]]
               [quarter :keyword #_[:q1 :q2 :q3 :q4]]))
   (s/schema actual-period
             (s/fields
               [type :keyword]
               [year :long]
               [month :keyword]
               [quarter :keyword]))
   (s/schema statement
             (s/fields
               #_[bank-account :ref :one]
               [actual-period :ref :one]
               [line-items :ref :many]
               [ordinal :long :one]
               ))
   (s/schema start-bank-balances
             (s/fields
               [actual-period :ref :one]
               [bank-balances :ref :many]))
   (s/schema import-template
             (s/fields
               [account :ref :one]
               [template-str :string]))

   (s/schema condition
             (s/fields
               [field :keyword #_[:desc]]
               [predicate :keyword #_[:starts-with :equals :ends-with :contains]]
               [subject :string]))

   (s/schema rule
             (s/fields
               [logic-operator :keyword #_[:or :and :single]]
               [dominates :ref :many]
               [time-slot :ref :one]
               [conditions :ref :many]
               [source-bank :ref :one]
               [target-account :ref :one]
               [actual-period :ref :one]
               [on-dates :instant :many]))

   (s/schema timespan
             (s/fields
               [commencing-period :ref :actual-period]
               [latest-period :ref :actual-period]))

   (s/schema organisation
             (s/fields
               [name :string "Name of the organisation"]
               [key :keyword "Keyword of the organisation"]
               [period-type :keyword #_[:quarterly :monthly]]
               ;; Not so much over-engineered as a de-normalization for
               ;; when the front end wants to show all you have
               [timespan :ref :one]
               [org-type :keyword #_[:charity :tax]]
               [import-templates :ref :many]
               [import-data-root :string]
               [tax-years :ref :many]
               [on-gst :ref :many]
               [exp-accounts :ref :many]
               [non-exp-accounts :ref :many]
               [income-accounts :ref :many]
               [personal-accounts :ref :many]
               [liab-accounts :ref :many]
               [equity-accounts :ref :many]
               [asset-accounts :ref :many]
               [liab-accounts :ref :many]
               [bank-accounts :ref :many]
               [split-accounts :ref :many]
               [splits :ref :many]
               [possible-reports :keyword :many]
               [rules :ref :many]
               [current-ordinal :long :one]
               ))

   (s/schema auth
             (s/fields
               [login :string]
               [pwd :bytes]))

   (s/schema person
             (s/fields
               [name :string "Name of the person"]))

   (s/schema group
             (s/fields
               [name :string]))])

(defn transactions []
  [(s/generate-parts parts) (s/generate-schema db-schema)])
