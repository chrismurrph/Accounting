(ns accounting.util
  (:require [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]))

(def hard-error? true)

(defn err-warn [predicate-res msg]
  (if-not predicate-res
    (if hard-error?
      (assert false msg)
      (do
        (println "WARNING:" msg)
        predicate-res))
    predicate-res))

(defn left-pad-spaces [max-sz]
  (fn [goes-at-right]
    (assert (string? goes-at-right))
    (let [diff-count (- max-sz (count goes-at-right))]
      (if (pos? diff-count)
        (str (apply str (take diff-count (repeat " "))) goes-at-right)
        goes-at-right))))

(def third #(nth % 2))

(defn abs [val]
  (if (neg? val)
    (* -1 val)
    val))

#_(defn round [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn round-dec-pl
  "Round a double to the given number of significant digits"
  [precision]
  (fn [d]
    (let [factor (Math/pow 10 precision)]
      (/ (Math/round (* d factor)) factor))))

(def round2 (round-dec-pl 2))
(def round0 (round-dec-pl 0))

(defn no-dec-pl [s]
  (first (s/split s #"[.]")))

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

(defn x-1 []
  ((left-pad-spaces 40) "hard-right"))

(defn x-2 []
  (no-dec-pl "00.45"))
