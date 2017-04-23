(ns accounting.test-croquet
  (:require [accounting.core :as c]
            [accounting.meta.common :as meta]
            [accounting.util :as u]
            [accounting.gl :as gl]
            [accounting.croquet-rules-data :as d]
    ;[accounting.croquet-context :as con]
            [accounting.match :as m]))

(def current-range [{:period/year  2017
                     :period/month :feb}
                    {:period/year  2017
                     :period/month :mar}
                    ])
(def current-rules (m/canonicalise-rules d/rules))
(def croquet-bank-accounts (-> meta/human-meta :croquet :bank-accounts))

(defn x-2 []
  (-> (c/first-without-single-rule-match :croquet (set croquet-bank-accounts) current-range current-rules)
      u/pp))

(defn x-5 []
  (let [transactions (->> (c/attach-rules
                            :croquet
                            (set croquet-bank-accounts)
                            current-range
                            current-rules)
                          u/probe-off
                          (map second)
                          (sort-by :out/date)
                          )]
    (u/pp (reduce gl/apply-trans gl/general-ledger transactions))))