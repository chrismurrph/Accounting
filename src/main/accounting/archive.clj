(ns accounting.archive
  (:require
    [datomic.api :as d]
    [accounting.datomic-helpers :as dh]
    [accounting.test-data :as td]))

;;
(comment "Instead of this within can be a relation we make up on the client in fulcro way"
         (defn place-within [{:keys [eid content-holder-key]}]
           (fn [tx]
             (println "within" tx)
             {:db/id             eid
              content-holder-key (mapv :db/id tx)
              })))

(defn collect-ids [class new-entities]
  (->> new-entities
       (filter (fn [[k v]] (= class (first k))))
       (map (fn [[k v]] (second k)))))

(defn replace-db-ids [omid->tempid]
  (fn [m]
    (->> m
         (map (fn [[k v]]
                (if (= k :db/id)
                  [k (or (omid->tempid v) v)]
                  [k v])))
         (into {}))))

;;
;; Part of the driver converting what Fulcro creates to what Datomic accepts. So far just looking at new
;; entities and relations that are added. Yet to come is existing entities and relations to be removed.
;; From Datomic's point of view the reference to the nested map must be a component attribute.
;;
(defn fulcro->nested [detail-class {:keys [form/new-entities form/add-relations]}]
  (-> (reduce-kv
        (fn [m k v]
          (let [[relation-k [merge-k merge-idents]] (some (fn [[rk rv]] (when (= rk k) [rk (first rv)])) add-relations)]
            (if relation-k
              (merge m {k (merge v {merge-k (mapv (fn [ident] (dissoc (get new-entities ident) :db/id)) merge-idents)})})
              (if (= detail-class (first k))
                m
                (merge m {k v})))))
        {}
        new-entities)
      vals))

;;
;; Needs to return tx, a transaction vector, and omid->tempid, which maps the ids from add-relations to the ids
;; we create here which are used by Datomic in the transaction.
;; 1. Get the existing ids of the master class as a list
;; 2. Create the omid->tempid map by generating against every one in the list
;; 3. fulcro->nested will give us tx but without the Datomic tempids
;; 4. mapv over this with replace-db-ids s/give us tx that Datomic needs
;; 5. Return as hash-map with tx and omid->tempid
;;
(defn datomic-driver-1 [within {:keys [form/new-entities form/add-relations form/updates] :as fulcro-in}]
  (let [{:keys [eid attribute attribute-value-value master-class detail-class]} within
        new-master-ids (collect-ids master-class new-entities)
        new-detail-ids (collect-ids detail-class new-entities)
        omid->tempid (->> (concat new-master-ids new-detail-ids)
                          (map (fn [id] [id (d/tempid :db.part/user)]))
                          (into {}))
        ids-replacer (replace-db-ids omid->tempid)
        tx (->> fulcro-in
                (fulcro->nested detail-class)
                (map ids-replacer))
        tx (-> tx
               (concat (dh/fulcro->updates updates))
               vec)]
    (println "new-entities" new-entities)
    (println "add-relations" add-relations)
    (println "TX: " tx)
    {:tx           tx
     :omid->tempid omid->tempid}))

(defn test-create-nested-b1 []
  (datomic-driver-1 td/within-example-1 td/incoming-example-1))

(defn test-create-nested-a1 []
  (fulcro->nested :phone/by-id td/incoming-example-1))

(defn test-create-nested-a2 []
  (fulcro->nested :condition/by-id td/incoming-example-2))

(defn test-create-nested-b2 []
  (datomic-driver-1 td/within-example-2 td/incoming-example-2))

(def db-uri "datomic:dev://localhost:4334/b00ks")

;;
;; This working. We not yet handling modifications
;;
(defn test-creating-nested []
  (let [conn (d/connect db-uri)
        nested (:tx (datomic-driver-1 td/within-example-1 td/incoming-example-1))]
    @(d/transact conn nested)))
