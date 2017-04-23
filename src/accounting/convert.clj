(ns accounting.convert
  (:require [accounting.time :as t]
            [clojure.string :as s]
            [accounting.util :as u]))

;; :mock is fields I've manually added on purpose when file format changed and I did not want to upset Xero
;; :ignore is a field that holds no purpose for accounting
(def amp-structure [:in/long-date :in/mock :in/desc :in/dollar-amount :in/ignore :in/ignore :in/mock])
(def anz-coy-structure [:in/short-date :in/quoted-amount :in/desc])
(def anz-visa-structure [:in/short-date :in/quoted-amount :in/desc])
(def bendigo-croquet-structure [:in/short-date :in/quoted-amount :in/quoted-desc])
(def leave-outs #{:in/mock :in/ignore})

(def bank-account-headings
  {:bank/amp      amp-structure
   :bank/anz-coy  anz-coy-structure
   :bank/anz-visa anz-visa-structure
   :bank/bendigo  bendigo-croquet-structure})

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
    ((comp u/str-number? u/my-read-string) x) nil
    :default (str ":quoted-amount value needs to be a number - but is: " x)))

(defn dollar-amount-convert [amount]
  (let [idx (s/index-of amount \$)]
    (condp = idx
      0 (bigdec (apply str (next amount)))
      1 (bigdec (apply str (first amount) (-> amount next next)))
      nil (assert false (str "No $ found in: " amount)))))

(def heading->parse-obj
  {:in/long-date     {:field-kw :in/long-date :validate-fn default-validate :convert-fn t/long-date-str->date}
   :in/short-date    {:field-kw :in/short-date :validate-fn default-validate :convert-fn t/short-date-str->date}
   :in/desc          {:field-kw :in/desc :validate-fn default-validate :convert-fn identity}
   :in/quoted-desc   {:field-kw :in/desc :validate-fn default-validate :convert-fn u/my-read-string}
   :in/dollar-amount {:field-kw :in/dollar-amount :validate-fn dollar-amount-validate :convert-fn dollar-amount-convert}
   :in/quoted-amount {:field-kw :in/quoted-amount :validate-fn quoted-amount-validate :convert-fn (comp bigdec u/my-read-string)}
   :in/ignore        {:field-kw :in/ignore}
   :in/mock          {:field-kw :in/mock}})

;; The conversion got rid of the non-canonical stuff
(def -in->out-kw
  {:in/long-date     :out/date
   :in/short-date    :out/date
   :in/desc          :out/desc
   :in/dollar-amount :out/amount
   :in/quoted-amount :out/amount})

(defn in->out-kw [kw]
  (or (-in->out-kw kw) kw))

(def all-headings (-> heading->parse-obj keys set))


