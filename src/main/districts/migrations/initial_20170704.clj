(ns districts.migrations.initial-20170704
  (:require [datomic.api :as d]
            [untangled.datomic.schema :as s]))

(defn -transactions []
  [(s/generate-schema
     [(s/schema district
                (s/fields
                  [name :string :unique-identity "Name of the district"]))

      (s/schema neighborhood
                (s/fields
                  [district :ref :one ""]
                  [name :string "Name of the neighborhood"]))])])
