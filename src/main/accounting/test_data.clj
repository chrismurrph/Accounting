(ns accounting.test-data
  (:require [datomic.api :as d]))

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
                       :content-holder-class  :organisation/by-id
                       ;:detail-class          :phone/by-id
                       })

(def within-example-2 {:content-holder-key    :organisation/rules
                       :attribute             :organisation/key
                       :attribute-value-value :seaweed
                       :master-class          :rule/by-id
                       :content-holder-class  :organisation/by-id
                       ;:detail-class          :condition/by-id
                       })

