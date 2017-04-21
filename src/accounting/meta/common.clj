(ns accounting.meta.common
  (:require [clojure.string :as s]
            [accounting.meta.seaweed :as seasoft]
            [accounting.meta.croquet :as croquet]))

(def quarters #{:q1 :q2 :q3 :q4})

(defn quarter->str [quarter]
  (-> quarter name s/capitalize))

(defn -dir-for [{:keys [tax-years data-root]} {:keys [period/tax-year period/quarter] :as period}]
  (assert tax-years)
  (assert data-root)
  (assert (quarters quarter))
  (assert (tax-years tax-year))
  (let [tax-year (str "Tax Year " tax-year)
        qtr (quarter->str quarter)]
    (str data-root tax-year "/" qtr)))

(defn bank-period->file-name [{:keys [file-names tax-years data-root] :as meta}]
  (fn [bank {:keys [period/tax-year period/quarter] :as period}]
    (let [qtr (quarter->str quarter)
          file-name (str qtr (file-names bank))
          file-path (str (-dir-for meta period) "/" file-name)]
      file-path)))

(def human-meta
  {:seaweed {:file-names seasoft/file-names
             :tax-years seasoft/tax-years
             :data-root seasoft/data-root
             :bank-accounts seasoft/bank-accounts}
   :croquet {:file-names croquet/file-names
             :tax-years croquet/tax-years
             :data-root croquet/data-root
             :bank-accounts croquet/bank-accounts}})
