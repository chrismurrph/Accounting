(ns accounting.core
  (:require [accounting.meta :as meta]
            [accounting.util :as u]
            [clojure.string :as s]))

(def current {:bank    :amp
              :year    2017
              :quarter :q3})

;;
;; Given a bank account we can get a structure. The structure shows us how to parse each field so
;; we can create a hash with right keys where vals have correct types.
;;
(defn parse-csv [bank-account lines])

(defn x-1 []
  (let [{:keys [bank year quarter]} current
        file-path (meta/bank-period->file-name bank year quarter)]
    (->> (slurp file-path)
         s/split-lines
         (map u/line->csv))))
