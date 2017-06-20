(ns accounting.scratch
  (:require [accounting.util :as u]))

(defn x-1 []
  (#(let [[lesser greater] (sort [%1 %2])]
      (loop [bigger greater
             smaller lesser]
        (println smaller bigger))) 7 1))

#_(defn find-my-object [my-channel]
    (go-loop []
             (when-some [value (<! my-channel)]
               (if (some-predicate? value)
                 value
                 (recur)))))

(defn exp [x n]
  (reduce * (repeat n x)))

(defn primes
  ([] (primes 1 1))
  ([n m] (if (= n 1)
           (lazy-seq (cons (* (exp 7 n) (exp 11 m)) (primes (+ m 1) 1)))
           (lazy-seq (cons (* (exp 7 n) (exp 11 m)) (primes (- n 1) (+ m 1)))))))

(defn x-1 []
  (doseq [prime (take 4 (primes 2 2))]
    (println prime)))

(defn x-2 []
  (->> (primes 2 2)
       (take 4)
       (apply println)))

(def ex-key [:video/by-id "ABC"])
(defn ex-ident? [x]
  (and (vector? x) (= 2 (count x))))
(defn x-3 []
  (when (ex-ident? ex-key) ex-key))

(defn add-on [fin xs]
  (concat xs [(list fin)]))

(defn f [p v c]
  (if (seq c)
    (let [res (->> c
                   (partition 2 1)
                   u/probe-off
                   (mapcat (juxt (fn [x] (when (apply p x) v)) second))
                   u/probe-on
                   (remove nil?)
                   )]
      (u/probe-off (cons (first c) res)))
    (list)))

(defn x-4 []
  (= '(1 :less 6 :less 7 4 3) (f < :less [1 6 7 4 3])))

(defn x-5 []
  (empty? (f > :more ())))

(defn x-6 []
  (= [0 1 :same 1 2 3 :same 5 8 13 :same 21]
     (take 12 (->> [0 1]
                   (iterate (fn [[a b]] [b (+ a b)]))
                   (map first) ; fibonacci numbers
                   (f (fn [a b] ; both even or both odd
                         (= (mod a 2) (mod b 2)))
                       :same)))))

(def recorded-income 31725)
(def recorded-expenses 30856)
(def not-included-expenses [])
(def tax-expense 268)
(def not-allowed-expenses 71.5)

;; 28.5 is wanted, only 71.5 gives
(defn calc-rate []
  (let [expenses (- recorded-expenses not-allowed-expenses)
        income recorded-income
        profit (- income expenses)]
    (u/round2 (* 100 (/ (float tax-expense) profit)))))
