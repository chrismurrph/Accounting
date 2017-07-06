(ns b00ks.migrations.initial-20170705
  (:require [datomic.api :as d]
            [untangled.datomic.schema :as s]))

(def parts
  [(s/part "app")])

(def db-schema
  [(s/schema base
             (s/fields
               [uuid :uuid :unique-identity]
               [type :keyword]
               [date :instant]
               [proportion :bigint]))
   (s/schema length-of-time
             (s/fields
               [start-at :instant]
               ;; end-at will often be optional
               [end-at :instant]))
   (s/schema account
             (s/fields
               [category :enum [:exp :liab :non-exp :personal
                                :bank :income :equity]]
               [name :string "Name of the account, for example \"bank-fee\""]
               [desc :string "Description of the account, for example \"Bank Fee\""]
               [organisation :ref :one]
               [length-of-time :ref :one]))
   (s/schema split
             (s/fields
               [name :string]
               [proportion :ref :many]))
   (s/schema period
             (s/fields
               [type :enum [:quarterly :monthly]]
               [month :enum [:jan :feb :mar :apr :may :jun :jul :aug :sep :oct :nov :dec]]
               [quarter :enum [:q1 :q2 :q3 :q4]]))
   (s/schema actual-period
             (s/fields
               [year :long]
               [period :ref :one]))
   (s/schema account-balance
             (s/fields
               [account :ref :one]
               [amount :ref :one]))
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
               [predicate :enum [:starts-with :equals :ends-with]]
               [subject :string]))

   (s/schema rule
             (s/fields
               [field :enum [:desc]]
               [logic-operator :enum [:or :and :single]]
               [dominates :ref :many]
               [between-dates-inclusive :ref :one]
               [conditions :ref :many]
               [source-bank :ref :one]
               [target-account :ref :one]
               [period :ref :one]
               [on-dates :ref :many]))

   (s/schema organisation
             (s/fields
               [name :string "Name of the organisation"]
               [period-type :enum [:quarterly :monthly]]
               [org-type :enum [:charity :tax]]
               [import-templates :ref :many]
               [import-data-root :string]
               [on-gst :ref :many]
               [bank-accounts :ref :many]
               [ledger-accounts :ref :many]))

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
