(ns accounting.convert
  (:require [clj-time.core :as t]
            [accounting.util :as u]
            [clojure.string :as s]))

;; :mock is fields I've manually added on purpose when file format changed and I did not want to upset Xero
;; :ignore is a field that holds no purpose for accounting
(def amp-structure [:date :mock :desc :amount :ignore :ignore :mock])
(def leave-outs #{:mock :ignore})

(def bank-account-headings
  {:amp amp-structure})

(def str->month
  {"Jan" 1
   "Feb" 2
   "Mar" 3
   "Apr" 4})

(defn str->date [x]
  (let [[_ day m year] (re-matches #"(\d+) (\w+) (\d+)" x)
        month (str->month m)]
    (assert (-> month nil? not))
    ;(println day month year)
    (t/date-time (u/to-int year) month (u/to-int day))))

;;
;; nil is okay whereas a message will be processed
;;
(defn default-validate [x]
  nil)

(defn amount-validate [x]
  (cond
    (= \$ (first x)) nil
    (and (= \- (first x)) (= \$ (second x))) nil
    :default (str ":amount value needs to start with $ or - but is: " x)))

(defn amount-convert [amount]
  (let [idx (s/index-of amount \$)]
    (condp = idx
      0 (bigdec (apply str (next amount)))
      1 (bigdec (apply str (first amount) (-> amount next next)))
      nil (assert false (str "No $ found in: " amount)))))

(def heading->parse-obj
  {:date {:field-kw :date :validate-fn default-validate :convert-fn str->date}
   :desc {:field-kw :desc :validate-fn default-validate :convert-fn identity}
   :amount {:field-kw :amount :validate-fn amount-validate :convert-fn amount-convert}
   :ignore {:field-kw :ignore}
   :mock {:field-kw :mock}})

(defn x-1 []
  (str->date "21 Mar 2017"))


