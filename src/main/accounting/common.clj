(ns accounting.common)

(defn attach-period [period rules-in]
  (assert period)
  (assert (seq rules-in) (str "No rules for " period))
  (->> rules-in
       (map (fn [[k v]]
              [k (mapv #(assoc % :rule/actual-period period :rule/permanent? false) v)]))
       (into {})))
