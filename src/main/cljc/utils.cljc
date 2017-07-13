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

(defn count-probe-on [xs]
  (println "COUNT" (count xs))
  xs)

(defn count-probe-off [xs]
  xs)

;;
;; name - of the thing we are asserting on
;; value - of the thing we are asserting on
;;
(defn assert-str [name value]
  (str name " (nil?, type, value-of): [" (nil? value) ", " (type value) ", " value "]"))

(defn string->kw [s]
  (-> s
      (subs 1)
      keyword))

(defn kw->string [kw]
  (and kw (subs (str kw) 1)))

(defn log [txt]
  (println (str "LOG: " txt)))

(def log-on log)

(defn log-off [txt])



