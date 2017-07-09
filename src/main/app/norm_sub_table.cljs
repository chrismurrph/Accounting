(ns app.norm-sub-table
  (:require [app.util :as u]))

;;
;; Proving too hard to do this. Time to move to Datomic
;;

;;
;; Would not be necessary if was using Datomic, where queries go straight across to the
;; server and results come back into your app state all normalized.
;;

;; See those 3 conditions? They need to be put into their own (default db) table.
(def example-state
  {:rule/by-id
   {0
    {:rule/on-dates                nil,
     :rule/permanent?              false,
     :rule/source-bank             :bank/anz-visa,
     :rule/conditions
                                   [[:out/desc :equals "TARGET 5009               ADELAIDE"]
                                    [:out/desc :starts-with "DRAKE SUPERMARKETS"]
                                    [:out/desc :starts-with "Z & Y BEYOND INTL PL"]],
     :db/id                        0,
     :rule/time-slot nil,
     :rule/period                  {:period/tax-year 2017, :period/quarter :q3},
     :rule/logic-operator          :or,
     :rule/target-account          :exp/office-expense}}})

(def ex-st {:rule/by-id
            {0
             {:rule/on-dates nil,
              :rule/permanent? false,
              :rule/source-bank :bank/anz-visa,
              :rule/conditions [[:out/desc :equals "TARGET 5009               ADELAIDE"]
                                [:out/desc :starts-with "DRAKE SUPERMARKETS"]
                                [:out/desc :starts-with "Z & Y BEYOND INTL PL"]],
              :db/id 0, :rule/time-slot nil,
              :rule/period {:period/tax-year 2017, :period/quarter :q3},
              :rule/logic-operator :or, :rule/target-account :exp/office-expense}}})

(defn norm-updater [table-kw where-at f]
  (fn [st]
    (->> st
         (map (fn [[k v]]
                (if (and (map? v) (= table-kw k))
                  [k (into {} (map (fn [[k v]] [k (update v where-at f)]) v))]
                  [k v])))
         (into {}))))

(defn ident-creator [ids-start-at new-table-kw]
  (fn [idx _]
    [new-table-kw (+ idx ids-start-at)]))

(defn conds-updater-hof [value->ident]
  (fn [conds]
    (mapv value->ident conds)))

;;
;; You have a vector of vectors you need to normalise out into their own table,
;; and reference back to it with idents
;;
(defn normalizer [table-kw where-at new-table-kw make-object ids-start-at]
  (let [make-ident (ident-creator ids-start-at new-table-kw)]
    (fn [st]
      (let [value->ident (->> (-> st table-kw vals)
                              (mapcat where-at)
                              (into #{})
                              (map-indexed (juxt (fn [idx value] value) make-ident))
                              (into {}))
            ;_ (println (str "state: " table-kw " -> " (table-kw st)))
            conds-updater (conds-updater-hof value->ident)
            updater (norm-updater table-kw where-at conds-updater)
            with-idents (updater st)
            new-table (->> value->ident
                           (map (fn [[k v]]
                                  [(second v) (make-object k)]))
                           (into {}))]
        (assoc with-idents new-table-kw new-table)))))

;; [condition/field condition/predicate condition/subject]
(defn v->condition [[field predicate subject]]
  {:condition/field field :condition/predicate predicate :condition/subject subject})

(defn x-1 []
  ((normalizer :rule/by-id :rule/conditions :condition/by-id v->condition 1000) ex-st))

