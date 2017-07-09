(ns app.util
  (:require [cljs.pprint :as cljs-pp]))

(enable-console-print!)

(defn kw-like-str? [x]
  (and (string? x) (= \: (first x)) (not= \: (second x))))

(defn keywordize [x]
  (cond-> x (kw-like-str? x) string->kw))

(defn target-kw [evt]
  (keywordize (.. evt -target -value)))

(defn abs [n]
  (if (neg? n) (- n) n))

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