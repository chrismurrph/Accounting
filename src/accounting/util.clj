(ns accounting.util
  (:require [clojure.string :as s]))

(defn line->csv [line]
  (s/split line #","))

(def to-int #(Integer/parseInt %))
(def to-ints (partial map to-int))

(defn str-number? [x]
  (Float/parseFloat x))

;; "206.90" read-string returns a string, go figure - yet works as expected in the REPL
;; Answer was that it was coated twice with double quotes, so need to read-string twice
(defn str-number? [x]
  (assert (string? x))
  (let [first-unquote (-> x read-string)]
    (if ((complement string?) first-unquote)
      (number? first-unquote)
      (-> first-unquote read-string number?))))
