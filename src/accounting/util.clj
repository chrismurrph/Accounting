(ns accounting.util
  (:require [clojure.string :as s]))

(defn line->csv [line]
  (s/split line #","))
