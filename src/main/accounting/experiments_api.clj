(ns accounting.experiments-api
  (:require [datomic.api :as d]))

(defn make-district [connection list-name]
  (let [id (d/tempid :db.part/user)
        tx [{:db/id id :district/name list-name}]
        idmap (:tempids @(d/transact connection tx))
        real-id (d/resolve-tempid (d/db connection) idmap id)]
    real-id))

(defn find-district
  "Find or create a district with the given name. Always returns a valid district ID."
  [conn district-name]
  (if-let [eid (d/q '[:find ?e . :in $ ?n :where [?e :district/name ?n]] (d/db conn) district-name)]
    eid
    (make-district conn district-name)))

;;
;; Just for initial experiments against a different api
;;
(defn read-district [connection query nm]
  (let [list-id (find-district connection nm)
        db (d/db connection)
        rv (d/pull db query list-id)]
    rv))

