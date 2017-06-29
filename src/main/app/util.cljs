(ns app.util
  (:require [cljs.pprint :as cljs-pp]))

(enable-console-print!)

(defn string->kw [s]
  (-> s
      (subs 1)
      keyword))

(defn kw->string [kw]
  (and kw (subs (str kw) 1)))

(defn kw-like-str? [x]
  (and (string? x) (= \: (first x)) (not= \: (second x))))

(defn keywordize [x]
  (cond-> x (kw-like-str? x) string->kw))

(defn target-kw [evt]
  (keywordize (.. evt -target -value)))

(defn abs [n]
  (if (neg? n) (- n) n))

;;
;; Only having to do this b/c I suspect that Untangled always wants to see
;; keywords in :option/key. I'm probably wrong and will test properly later
;;
(defn kw->number [kw]
  (-> kw name js/parseInt))

;;
;; Up to and including, inclusive of both from and to
;;
(defn numerical-range [from to]
  (->> from
       (iterate inc)
       (take-while #(<= % to))))

(defn probe-off
  ([x]
   x)
  ([x msg]
   (assert (string? msg))
   x))

(def width 120)

(defn pp-str
  ([n x]
   (binding [cljs-pp/*print-right-margin* n]
     (-> x cljs-pp/pprint with-out-str)))
  ([x]
   (pp-str width x)))

(defn pp
  ([n x]
   (binding [cljs-pp/*print-right-margin* n]
     (-> x cljs-pp/pprint)))
  ([x]
   (pp width x)))

(defn symbol->str [x]
  (.toString x))

(defn probe-on
  ([x]
   (-> x
       pp)
   x)
  ([x msg]
   (assert (string? msg))
   (js/console msg x)
   x))

(defn warn [want? txt]
  (when-not want?
    (println (str "WARN: " txt #_" -> >" #_want? #_"<"))))

(defn log [txt]
  (println (str "LOG: " txt)))

(def log-on log)

(defn log-off [txt])
