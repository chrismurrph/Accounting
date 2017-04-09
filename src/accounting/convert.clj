(ns accounting.convert
  (:require [clj-time.core :as t]
            [accounting.util :as u]
            [clojure.string :as s]))

;; :mock is fields I've manually added on purpose when file format changed and I did not want to upset Xero
;; :ignore is a field that holds no purpose for accounting
(def amp-structure [:long-date :mock :desc :dollar-amount :ignore :ignore :mock])
(def anz-coy-structure [:short-date :quoted-amount :desc])
(def leave-outs #{:mock :ignore})

(def bank-account-headings
  {:amp amp-structure
   :anz-coy anz-coy-structure})

(def str->month
  {"Jan" 1
   "Feb" 2
   "Mar" 3
   "Apr" 4})

(defn long-date-str->date [x]
  (let [[_ day m year] (re-matches #"(\d+) (\w+) (\d+)" x)
        month (str->month m)]
    (assert (-> month nil? not))
    ;(println day month year)
    (t/date-time (u/to-int year) month (u/to-int day))))

(defn short-date-str->date [x]
  (let [[_ d m y] (re-matches #"(\d+)/(\d+)/(\d+)" x)]
    (t/date-time (u/to-int y) (u/to-int m) (u/to-int d))))

;;
;; nil is okay whereas a message will be processed
;;
(defn default-validate [x]
  nil)

(defn dollar-amount-validate [x]
  (cond
    (= \$ (first x)) nil
    (and (= \- (first x)) (= \$ (second x))) nil
    :default (str ":dollar-amount value needs to start with $ or - but is: " x)))

(defn quoted-amount-validate [x]
  ;(println (str x "," (u/str-number? x) "," (type (-> x read-string))))
  (cond
    (u/str-number? x) nil
    :default (str ":quoted-amount value needs to be a number - but is: " x)))

(defn dollar-amount-convert [amount]
  (let [idx (s/index-of amount \$)]
    (condp = idx
      0 (bigdec (apply str (next amount)))
      1 (bigdec (apply str (first amount) (-> amount next next)))
      nil (assert false (str "No $ found in: " amount)))))

(def heading->parse-obj
  {:long-date     {:field-kw :long-date :validate-fn default-validate :convert-fn long-date-str->date}
   :short-date    {:field-kw :short-date :validate-fn default-validate :convert-fn short-date-str->date}
   :desc          {:field-kw :desc :validate-fn default-validate :convert-fn identity}
   :dollar-amount {:field-kw :dollar-amount :validate-fn dollar-amount-validate :convert-fn dollar-amount-convert}
   :quoted-amount {:field-kw :quoted-amount :validate-fn quoted-amount-validate :convert-fn (comp bigdec read-string)}
   :ignore        {:field-kw :ignore}
   :mock          {:field-kw :mock}})

(def all-headings (-> heading->parse-obj keys set))

(defn x-1 []
  (long-date-str->date "21 Mar 2017"))

(defn x-2 []
  (short-date-str->date "31/03/2017"))


