(ns user
  (:require [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.stacktrace]
            [clojure.pprint :as pp]))

;(clojure.main/repl :print pp)

;;
;; To run open a REPL and:
;; (reset)
;; (accounting.core/<some-cmd>)
;; Repeat those two commands after every source code change you make
;;

(defn reset []
  (refresh))