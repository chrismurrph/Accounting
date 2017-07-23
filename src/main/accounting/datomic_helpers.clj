(ns accounting.datomic-helpers
  (:require [datomic.api :as d]))

(def understood-keys #{:form/new-entities :form/add-relations})
(defn unimplemented-keys? [m]
  (let [new-keys (->> m
                      keys
                      (remove understood-keys))]
    (seq new-keys)))

(defn resolve-ids [new-db omids->tempids tempids->realids]
  (reduce
    (fn [acc [cid dtmpid]]
      (assoc acc cid (d/resolve-tempid new-db tempids->realids dtmpid)))
    {}
    omids->tempids))

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
      vals
      vec))

;;
;; Needs to return tx, a transaction vector, and omid->tempid, which maps the ids from add-relations to the ids
;; we create here which are used by Datomic in the transaction.
;; 1. Get the existing ids of the master class as a list
;; 2. Create the omid->tempid map by generating against every one in the list
;; 3. datomic-driver-1 will give us tx but without the Datomic tempids
;; 4. mapv over this with replace-db-ids s/give us tx that Datomic needs
;; 5. Return as hash-map with tx and omid->tempid
;;
(defn datomic-driver [master-class detail-class {:keys [form/new-entities form/add-relations] :as in}]
  (let [master-ids (->> new-entities
                        (filter (fn [[k v]] (= master-class (first k))))
                        (map (fn [[k v]] (second k))))
        omid->tempid (->> master-ids
                          (map (fn [id] [id (d/tempid :db.part/user)]))
                          (into {}))
        tx (fulcro->nested detail-class in)
        tx (mapv (replace-db-ids omid->tempid) tx)]
    {:tx           tx
     :omid->tempid omid->tempid}))

;;
;; These won't really be tempids but omids. Could not capture omids because of an edn reader problem. It doesn't
;; matter what they are for our purposes.
;;
(def incoming-example {:form/new-entities  {[:people/by-id "279bac0b-cd75-4b7f-824c-e37ad5a5cdb1"]
                                            {:db/id                      (d/tempid :db.part/user),
                                             :person/name                "Chris Murphy",
                                             :person/age                 35,
                                             :person/registered-to-vote? false},
                                            [:phone/by-id "0bcfbf90-eb56-4514-8259-123893bc9652"]
                                            {:db/id        (d/tempid :db.part/user),
                                             :phone/number "0403162669",
                                             :phone/type   :home}},
                       :form/add-relations {[:people/by-id "279bac0b-cd75-4b7f-824c-e37ad5a5cdb1"]
                                            {:person/phone-numbers [[:phone/by-id "0bcfbf90-eb56-4514-8259-123893bc9652"]]}}})

(defn test-create-nested-1 []
  (fulcro->nested :phone/by-id incoming-example))

(defn test-create-nested-2 []
  (datomic-driver :people/by-id :phone/by-id incoming-example))

;;
;; If there are only new entities life is easy...
;;
(defn fulcro->datomic [in]
  (-> in :form/new-entities vals vec))

(def db-uri "datomic:dev://localhost:4334/b00ks")

;;
;; Too simple, won't respect the relationships
;;
(defn bring-in []
  (let [conn (d/connect db-uri)]
    #_(fulcro->datomic incoming-example)
    @(d/transact conn (fulcro->datomic incoming-example))))

;; Can't seem to get this to upsert, using either :unique-value or :unique-identity on person entity.
;; Are going to need upserting when we are modifying existing rules.
;; bloody hell - upserting s/be happening:
;; In general, unique temporary ids are mapped to new entity ids. However, there is one exception.
;; When you add a fact about a new entity with a temporary id, and one of the attributes you specify
;; is defined as :db/unique :db.unique/identity, the system will map your temporary id to an existing
;; entity if one exists with the same attribute and value (update) or will make a new entity if one
;; does not exist (insert). All further adds in the transaction that apply to that same temporary id
;; are applied to the "upserted" entity.
;; As well as rule-num going to need condition-num to ensure upserting works so user can edit rules
;; and conditions, rather than new ones being added all the time.
;; WRONG -> forms tells us what we are doing so don't need upserting: entity-update(?), new-entities

(defn test-nested []
  (let [conn (d/connect db-uri)
        nested [{:db/id                      (d/tempid :db.part/user),
                 :person/name                "Chris Murphy",
                 :person/age                 50,
                 :person/registered-to-vote? false
                 :person/phone-numbers       [{:phone/number "0403168891",
                                               :phone/type   :home}]}]]
    @(d/transact conn nested)))

;;
;; This working. We not yet handling modifications
;;
(defn test-creating-nested []
  (let [conn (d/connect db-uri)
        nested (fulcro->nested :phone/by-id incoming-example)]
    @(d/transact conn nested)))

(defn test-what-coming []
  (let [conn (d/connect db-uri)
        coming-in [{:db/id                      (d/tempid :db.part/user),
                    :person/name                "Chris Murphy",
                    :person/age                 35,
                    :person/registered-to-vote? false}]]
    @(d/transact conn coming-in)))
