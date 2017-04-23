(ns accounting.common)

(defn attach-period [period rules-in]
  (into {} (map (fn [[k v]]
                  [k (mapv #(assoc % :period period) v)]) rules-in)))
