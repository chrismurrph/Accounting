(ns accounting.util
  (:require [clojure.string :as s]))

(defn line->csv [line]
  (s/split line #","))

(def to-int #(Integer/parseInt %))
(def to-ints (partial map to-int))
