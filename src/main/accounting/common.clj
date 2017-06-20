(ns accounting.common)

(defn attach-period [period rules-in]
  (assert period)
  (assert (seq rules-in) (str "No rules for " period))
  (into {} (map (fn [[k v]]
                  [k (mapv #(assoc % :period period) v)]) rules-in)))
