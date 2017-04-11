(ns accounting.util
  (:require [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]))

(def third #(nth % 2))

(defn abs [val]
  (if (neg? val)
    (* -1 val)
    val))

(defn line->csv [line]
  (s/split line #","))

(def to-int #(Integer/parseInt %))
(def to-ints (partial map to-int))

;; "206.90" read-string returns a string, go figure - yet works as expected in the REPL
;; Answer was that it was coated twice with double quotes, so need to read-string twice
(defn str-number? [x]
  (assert (string? x))
  (let [first-unquote (-> x read-string)]
    (if ((complement string?) first-unquote)
      (number? first-unquote)
      (-> first-unquote read-string number?))))

(def width 150)

(defn pp-str
  ([n x]
   (binding [pp/*print-right-margin* n]
     (-> x clojure.pprint/pprint with-out-str edn/read-string)))
  ([x]
   (pp-str width x)))

(defn pp
  ([n x]
   (binding [pp/*print-right-margin* n]
     (-> x clojure.pprint/pprint)))
  ([x]
   (pp width x)))

(defn probe-off
  ([x]
   x)
  ([x msg]
   x))

(defn probe-on
  ([x]
   (-> x
       pp)
   x)
  ([x msg]
   (println msg x)
   x))

