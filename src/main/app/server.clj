(ns app.server
  (:require [untangled.easy-server :as easy]
            [untangled.server :as server]
            app.operations
            [taoensso.timbre :as timbre]
            [untangled.datomic.core :refer [build-database]]))

(defn make-b00ks-system [config-path]
  (easy/make-untangled-server
    :config-path config-path
    :parser (server/untangled-parser)
    :parser-injections #{:b00ks-database}
    :components {:b00ks-database (build-database :b00ks)
                 :logger        {}}))

(defn make-districts-system [config-path]
  (easy/make-untangled-server
    :config-path config-path
    :parser (server/untangled-parser)
    :parser-injections #{:districts-database}
    :components {:districts-database (build-database :districts)
                 :logger        {}}))

