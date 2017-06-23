(ns app.util
  (:require [cljs.pprint :as cljs-pp]))

(enable-console-print!)

(defn probe-off
  ([x]
   x)
  ([x msg]
   (assert (string? msg))
   x))

(def width 120)

(defn pp
  ([n x]
   (binding [cljs-pp/*print-right-margin* n]
     (-> x cljs-pp/pprint)))
  ([x]
   (pp width x)))

(defn probe-on
  ([x]
   (-> x
       pp)
   x)
  ([x msg]
   (assert (string? msg))
   (println msg x)
   x))

(defn warn [want? txt]
  (when-not want?
    (println (str "WARN: " txt #_" -> >" #_want? #_"<"))))

(defn log [txt]
  (println (str "LOG: " txt)))
