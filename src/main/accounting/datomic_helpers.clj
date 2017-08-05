(ns accounting.datomic-helpers
  (:require [datomic.api :as d]
            [om.next :as om]
            [accounting.util :as u]))

(def testing true)
(defn om-tempid? [x]
  (if testing
    (string? x)
    (om/tempid? x)))

(def understood-keys #{:form/new-entities :form/add-relations :form/updates})
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
      vals))

;; We s/be able to assume the numbers are eids
;; :form/updates       {[:rule/by-id 17592186045637] {:rule/logic-operator :or}},
(defn fulcro->updates [updates]
  (mapv (fn [[[table-class id] m]]
          (merge {:db/id id} m)) updates))

(defn place-within [{:keys [eid content-holder-key]}]
  (fn [tx]
    {:db/id             eid
     content-holder-key (mapv :db/id tx)
     }))

(defn collect-ids [class new-entities]
  (->> new-entities
       (filter (fn [[k v]] (= class (first k))))
       (map (fn [[k v]] (second k)))))

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
        tx (-> (if eid (conj tx ((place-within within) tx)) tx)
               (concat (fulcro->updates updates))
               vec)]
    (println "new-entities" new-entities)
    (println "add-relations" add-relations)
    (println "TX: " tx)
    {:tx           tx
     :omid->tempid omid->tempid}))

(defn non-relations->tx
  [within form-new-entities form-updates]
  (let [{:keys [eid]} within
        omid->tempid (->> form-new-entities
                          keys
                          (map second)
                          (filter om-tempid?)
                          (map (fn [id] [id (d/tempid :db.part/user)]))
                          (into {}))
        tx (->> form-new-entities
                vals
                (map #(map (fn [[k v]] (if (= k :db/id) [k (omid->tempid v)] [k v])) %))
                (map #(into {} %)))
        tx (-> (if eid (conj tx ((place-within within) tx)) tx)
               (concat (fulcro->updates form-updates))
               vec)]
    {:tx           tx
     :omid->tempid omid->tempid}))

(defn translate-coll [omids->realids]
  (fn [v]
    (into {} (map (fn [[a b]]
                    [a (mapv (fn [[class key]] [class (or (omids->realids key) key)]) b)]) v))))

;; {[:rule/by-id 17592186045640]
;;  {:rule/conditions [[:condition/by-id "aa544ed2-c52d-4211-902e-1cafd0407699"]]}
;; }
(defn relations->tx [omids->realids form-add-relations]
  (println form-add-relations)
  (let [coll-translate-f (translate-coll omids->realids)
        realids-adds (->> form-add-relations
                          (map (fn [[k v]] [(or (omids->realids k) k) (coll-translate-f v)]))
                          (into {}))]
    realids-adds))

(defn datomic-driver
  [db
   within
   {:keys [form/new-entities form/add-relations form/updates] :as form-diff}]
  (u/fulcro-assert (not (unimplemented-keys? form-diff))
                   (str "Not yet coded for these keys: "
                        (unimplemented-keys? form-diff) form-diff))
  (let [{:keys [attribute-value-value attribute]} within
        conn (:connection db)
        ;_ (println "conn: <" conn ">")
        eid (and attribute (d/q '[:find ?e .
                                  :in $ ?o ?a
                                  :where [?e ?a ?o]]
                                (d/db conn) attribute-value-value attribute))
        {:keys [omid->tempid tx]} (non-relations->tx (assoc within :eid eid) new-entities updates)
        result @(d/transact conn tx)
        tempid->realid (:tempids result)
        omids->realids (resolve-ids db omid->tempid tempid->realid)
        tx (relations->tx omids->realids add-relations)]
    @(d/transact conn tx)
    {:tempids omids->realids}))

;;
;; These won't really be tempids but omids. Could not capture omids because of an edn reader problem. It doesn't
;; matter what they are for our purposes.
;;
(def incoming-example-1 {:form/new-entities  {[:people/by-id "279bac0b-cd75-4b7f-824c-e37ad5a5cdb1"]
                                              {:db/id                      (d/tempid :db.part/user),
                                               :person/name                "Chris Murphy",
                                               :person/age                 45,
                                               :person/registered-to-vote? false},
                                              [:phone/by-id "0bcfbf90-eb56-4514-8259-123893bc9652"]
                                              {:db/id        (d/tempid :db.part/user),
                                               :phone/number "0403162669",
                                               :phone/type   :home}
                                              [:people/by-id "279bac0b-cd75-4b7f-824c-e37ad5a5cdb2"]
                                              {:db/id                      (d/tempid :db.part/user),
                                               :person/name                "Jan Marie",
                                               :person/age                 31,
                                               :person/registered-to-vote? true},
                                              [:phone/by-id "0bcfbf90-eb56-4514-8259-123893bc9653"]
                                              {:db/id        (d/tempid :db.part/user),
                                               :phone/number "90866777777",
                                               :phone/type   :home}
                                              },
                         :form/add-relations {
                                              [:people/by-id "279bac0b-cd75-4b7f-824c-e37ad5a5cdb1"]
                                              {:person/phone-numbers [[:phone/by-id "0bcfbf90-eb56-4514-8259-123893bc9652"]]}
                                              [:people/by-id "279bac0b-cd75-4b7f-824c-e37ad5a5cdb2"]
                                              {:person/phone-numbers [[:phone/by-id "0bcfbf90-eb56-4514-8259-123893bc9653"]]}
                                              }})

(def incoming-example-2 {:form/updates      {[:rule/by-id 17592186045637] {:rule/logic-operator :or}},
                         :form/new-entities {[:condition/by-id "615aae0d-c052-4709-a5c3-6ca973cf0b9e"]
                                             {:db/id               "615aae0d-c052-4709-a5c3-6ca973cf0b9e",
                                              :condition/field     :out/desc,
                                              :condition/predicate :equals,
                                              :condition/subject   "SAIGON VIETNAMESE ME      OLD REYNELLA",
                                              }}})

(def incoming-example-3 {:form/new-entities  {[:condition/by-id "aa544ed2-c52d-4211-902e-1cafd0407699"]
                                              {:db/id             "aa544ed2-c52d-4211-902e-1cafd0407699",
                                               :condition/field   :out/desc, :condition/predicate :equals,
                                               :condition/subject "SAIGON VIETNAMESE ME      OLD REYNELLA"}}
                         :form/add-relations {[:rule/by-id 17592186045640]
                                              {:rule/conditions [[:condition/by-id "aa544ed2-c52d-4211-902e-1cafd0407699"]]}}})

(def within-example-1 {
                       :attribute             :organisation/key
                       :content-holder-key    :organisation/people
                       :attribute-value-value :seaweed
                       :master-class          :people/by-id
                       :detail-class          :phone/by-id})

(def within-example-2 {:content-holder-key    :organisation/rules
                       :attribute             :organisation/key
                       :attribute-value-value :seaweed
                       :master-class          :rule/by-id
                       :detail-class          :condition/by-id})

(defn test-relations []
  (relations->tx {"aa544ed2-c52d-4211-902e-1cafd0407699" 777} (:form/add-relations incoming-example-3)))

(defn test-create-nested-a1 []
  (fulcro->nested :phone/by-id incoming-example-1))

(defn test-create-nested-b1 []
  (datomic-driver-1 within-example-1 incoming-example-1))

(defn test-create-nested-a2 []
  (fulcro->nested :condition/by-id incoming-example-2))

(defn test-create-nested-b2 []
  (datomic-driver-1 within-example-2 incoming-example-2))


;;
;; If there are only new entities life is easy...
;;
(defn fulcro->datomic [in]
  (-> in :form/new-entities vals vec))

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn test-new-driver []
  (let [conn (d/connect db-uri)
        db (d/db conn)]
    (datomic-driver db {} incoming-example-3)))

;;
;; Too simple, won't respect the relationships
;;
(defn bring-in []
  (let [conn (d/connect db-uri)]
    #_(fulcro->datomic incoming-example-1)
    @(d/transact conn (fulcro->datomic incoming-example-1))))

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
        nested (:tx (datomic-driver-1 within-example-1 incoming-example-1))]
    @(d/transact conn nested)))

(defn test-what-coming []
  (let [conn (d/connect db-uri)
        coming-in [{:db/id                      (d/tempid :db.part/user),
                    :person/name                "Chris Murphy",
                    :person/age                 35,
                    :person/registered-to-vote? false}]]
    @(d/transact conn coming-in)))
