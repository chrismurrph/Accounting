(ns accounting.datomic-helpers
  (:require [datomic.api :as d]
            [om.next :as om]
            [accounting.util :as u]
            [accounting.test-data :as td]))

(defn om-tempid? [testing?]
  (fn [x]
    (if testing?
      (string? x)
      (om/tempid? x))))

(defn unimplemented-keys? [m]
  (let [new-keys (->> m
                      keys
                      (remove #{:form/new-entities :form/add-relations :form/updates}))]
    (seq new-keys)))

(defn resolve-ids [new-db omids->tempids tempids->realids]
  (reduce
    (fn [acc [cid dtmpid]]
      (assoc acc cid (d/resolve-tempid new-db tempids->realids dtmpid)))
    {}
    omids->tempids))

;; We s/be able to assume the numbers are eids
;; :form/updates       {[:rule/by-id 17592186045637] {:rule/logic-operator :or}},
(defn fulcro->updates [updates]
  (mapv (fn [[[table-class id] m]]
          (merge {:db/id id} m)) updates))

(defn only [xs]
  (let [counted (count xs)]
    (u/fulcro-assert (= 1 counted) (str "Expect to be one only: " (seq xs))))
  (first xs))

(defn include-base-type [m]
  (let [table-name (->> m
                        keys
                        (remove #{:db/id})
                        (map namespace)
                        distinct
                        only
                        keyword)]
    (merge m {:base/type table-name})))

(defn x-1 []
  (include-base-type {:db/id               "87c267cb-8997-4b4b-8bf3-8dcfa19023b0",
                      :condition/field     :out/desc,
                      :condition/predicate :equals,
                      :condition/subject   "SAIGON VIETNAMESE ME      OLD REYNELLA"}))

(defn non-relations->tx
  [testing? within form-new-entities form-updates]
  (let [omid? (om-tempid? testing?)
        omid->tempid (->> form-new-entities
                          keys
                          (map second)
                          (filter omid?)
                          (map (fn [id] [id (d/tempid :db.part/user)]))
                          (into {}))
        tx (->> form-new-entities
                vals
                (map include-base-type)
                (map #(map (fn [[k v]] (if (= k :db/id) [k (omid->tempid v)] [k v])) %))
                (map #(into {} %)))
        tx (-> tx
               (concat (fulcro->updates form-updates))
               vec)]
    {:tx           tx
     :omid->tempid omid->tempid}))

(defn form-relations->datomic [omids->realids]
  (fn [v]
    (into {} (map (fn [[a b]]
                    [a (mapv (fn [[class key]] (or (omids->realids key) key)) b)]) v))))

;; {[:rule/by-id 17592186045640]
;;  {:rule/conditions [[:condition/by-id "aa544ed2-c52d-4211-902e-1cafd0407699"]]}
;; }
;; {[:rule/by-id 17592186045640] {:rule/conditions [[:condition/by-id #om/id["8d64296b-c458-4cc0-93f9-cd94503a1690"]]]}}
(defn relations->tx [omids->realids form-add-relations]
  ;(println "IN:" form-add-relations)
  (let [coll-translate-f (form-relations->datomic omids->realids)
        tx (->> form-add-relations
                (mapv (fn [[k v]] (merge {:db/id (second (or (omids->realids k) k))}
                                         (coll-translate-f v))))
                ;(into {})
                )
        ;_ (println "OUT:" tx)
        ]
    tx))

(defn create-content-holder-relation
  [eid {:keys [content-holder-key content-holder-class master-class]} form-new-entities]
  (let [new-masters (->> form-new-entities
                         (filter (fn [[[class id] v]] (= class master-class)))
                         vals
                         (mapv :db/id))
        k [content-holder-class eid]
        v {content-holder-key new-masters}]
    [k v]))

;;
;; TODO
;; Don't need two transacts, instead call relations->tx with omid->tempid before the first d/transact.
;; I believe that d/transact will be creating and using tempid->realid as it goes along
;;
(defn datomic-driver
  [testing?
   conn
   {:keys [attribute-value-value attribute] :as within}
   {:keys [form/new-entities form/add-relations form/updates] :as form-diff}]
  (u/fulcro-assert (not (unimplemented-keys? form-diff))
                   (str "Not yet coded for these keys: "
                        (unimplemented-keys? form-diff) form-diff))
  (let [db (d/db conn)
        eid (and attribute (d/q '[:find ?e .
                                  :in $ ?o ?a
                                  :where [?e ?a ?o]]
                                db attribute-value-value attribute))
        {:keys [omid->tempid tx]} (non-relations->tx testing? within new-entities updates)
        result @(d/transact conn tx)
        tempid->realid (:tempids result)
        omids->realids (resolve-ids db omid->tempid tempid->realid)
        content-holder-relation (create-content-holder-relation eid within new-entities)
        tx (relations->tx omids->realids (conj add-relations content-holder-relation))]
    @(d/transact conn tx)
    {:tempids omids->realids}))

(defn test-relations []
  (relations->tx {"aa544ed2-c52d-4211-902e-1cafd0407699" 777} (:form/add-relations td/incoming-example-3)))

(defn test-create-content-relation []
  (create-content-holder-relation 2 td/within-example-1 (:form/new-entities td/incoming-example-1)))

;;
;; If there are only new entities life is easy...
;;
(defn fulcro->datomic [in]
  (-> in :form/new-entities vals vec))

(def db-uri "datomic:dev://localhost:4334/b00ks")

(defn test-new-driver []
  (let [conn (d/connect db-uri)]
    (datomic-driver true conn {} td/incoming-example-2)))

;;
;; Too simple, won't respect the relationships
;;
(defn bring-in []
  (let [conn (d/connect db-uri)]
    #_(fulcro->datomic incoming-example-1)
    @(d/transact conn (fulcro->datomic td/incoming-example-1))))

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

(defn test-what-coming []
  (let [conn (d/connect db-uri)
        coming-in [{:db/id                      (d/tempid :db.part/user),
                    :person/name                "Chris Murphy",
                    :person/age                 35,
                    :person/registered-to-vote? false}]]
    @(d/transact conn coming-in)))
