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

;;
;; When working put in utils and use from there, and rename to remove-quotes
;;
(defn remove-outer-quotes [x]
  (subs x 1 (-> x count dec)))

#_(defn round2
  "Round a double to the given precision (number of significant digits)"
  [d]
  (let [factor (Math/pow 10M 2M)]
    (/ (Math/round (* d factor)) factor)))

;; "206.90" read-string returns a string, go figure - yet works as expected in the REPL
;; Answer was that it was coated twice with double quotes, so need to read-string twice
(defn str-number-old? [x]
  (assert (string? x))
  (let [first-unquote (read-string x)]
    (if ((complement string?) first-unquote)
      (number? first-unquote)
      (-> first-unquote read-string number?))))

(defn str->number? [x]
  (try
    (Float/parseFloat (re-matches #"-?\d+\.?\d*" x))
    (catch Exception _
      nil)))

(def width 120)

(defn pp-str
  ([n x]
   (binding [pp/*print-right-margin* n]
     (-> x clojure.pprint/pprint with-out-str)))
  ([x]
   (pp-str width x)))

(defn pp
  ([n x]
   (binding [pp/*print-right-margin* n]
     (-> x clojure.pprint/pprint)))
  ([x]
   (pp width x)))

(def pp-off identity)

(defn probe-off
  ([x]
   x)
  ([msg x]
   x))

(defn probe-on
  ([x]
   (-> x
       pp)
   x)
  ([msg x]
   (println msg x)
   x))

(defn sleep [n]
  (Thread/sleep n))

(defmacro assrt
  "Useful to use (rather than official version that this is o/wise a copy of) when don't want intermingling of
  the stack trace produced here with trace output that want to come before"
  {:added "1.0"}
  ([x]
   (when *assert*
     `(when-not ~x
        (sleep 400)
        (throw (new AssertionError (str "Assert failed: " (pr-str '~x)))))))
  ([x message]
   (when *assert*
     `(when-not ~x
        (sleep 400)
        (throw (new AssertionError (str "Assert failed: " ~message "\n" (pr-str '~x))))))))

(defn warning [txt]
  (println "WARN: " txt))

