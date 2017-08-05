(ns cljc.utils)

(defn flip [f]
  (fn [& xs]
    (apply f (reverse xs))))

;;
;; Up to and including, inclusive of both from and to
;;
(defn numerical-range [from to]
  (->> from
       (iterate inc)
       (take-while #(<= % to))))

(defn probe-count-on [xs]
  (println "COUNT" (count xs))
  xs)

(defn probe-count-off [xs]
  xs)

;;
;; Only having to do this b/c I suspect that Untangled always wants to see
;; keywords in :option/key. I'm probably wrong and will test properly later
;;
(defn kw->number [kw]
  (assert kw)
  (if (number? kw)
    kw
    (-> kw name #?(:cljs js/parseInt
                   :clj  Integer/parseInt))))

;;
;; name - of the thing we are asserting on
;; value - of the thing we are asserting on
;;
(defn assert-str [name value]
  (str name " (nil?, fn?, type, value-of): ["
       (nil? value) ", " (fn? value)
       (when (-> value fn? not) (str ", " (type value) ", " value)) "]"))

(defn string->kw [s]
  (-> s
      (subs 1)
      keyword))

(defn kw->string [kw]
  (when kw (assert (keyword? kw)))
  (and kw (subs (str kw) 1)))

(defn log [txt]
  (println (str "LOG: " txt)))

(def log-on log)

(defn log-off [txt])



