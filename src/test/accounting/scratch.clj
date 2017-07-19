(ns accounting.scratch
  (:require [accounting.util :as u]))

;; Will need to do this on all computers
;Please try to change your system settings:  System -> Preferences -> Keyboard -> Layout -> Options -> Alt/Win key behavior.
;Choose there "Meta is mapped to left Win".

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

(-> :b (:a))

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
                   (map first)                              ; fibonacci numbers
                   (f (fn [a b]                             ; both even or both odd
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

(defn x-7 []
  (.indexOf [[[1 2 3] [4 5 6] [7 8 9]] [[10 11] [12 13]] [[14] [15]]] [[14] [15]]))

{:the-one {:db/id :the-one,
           :person-list/people
                  [[:person/by-id 1]
                   [:person/by-id 2]
                   [:person/by-id 3]
                   [:person/by-id 4]]},
 :friends {:person-list/people []}}

(def shifts [5 5 5 5 5 2 2 5 5 5 5 5 2 2])
(def total-shifts (apply + shifts))
(def genes (vec (range total-shifts)))

(defn transition-1 [[answer shifts-remaining genes-remaining]]
  (let [[num-shifts & more-shifts] shifts-remaining
        [daily-genes rest-of-genes] (split-at num-shifts genes-remaining)]
    [(conj answer daily-genes) more-shifts rest-of-genes]))

(defn x-8 []
  (->> (iterate transition-1 [[] shifts genes])
       (drop (count shifts))
       ffirst))

(defn transition [[text n]]
  (let [c (nth text n)
        nxt (if (= c \z) \z (-> c int inc char))
        nxt-str (str (subs text 0 n) nxt (subs text (inc n) (count text)))]
    (if (= \z nxt)
      [nxt-str (inc n)]
      [nxt-str n])))

(defn ongoing-1? [[text n]]
  (not (every? #(= \z %) text)))

(defn ongoing-2? [[text n]]
  (and (not= n (count text))
       (not= \z (nth text n))))

(defn x-11 []
  (->> (iterate transition ["dzs" 0])
       (take-while ongoing-1?)
       (map first)
       ;(take 5)
       ))

(defn insert-mean-between [xs]
  (let [f (fn [x y]
            [(* (+ x y) 0.5) y])]
    (->> xs
         (partition 2 1)
         (mapcat (partial apply f))
         (cons (first xs))
         vec)))

(defn insert-between [g xs]
  (->> xs
       (partition 2 1)
       (mapcat (fn [[x y]] [(g x y) y]))
       (cons (first xs))))

(defn x-9 []
  (insert-mean-between [1 10 15]))

(defn x-10 []
  (insert-between
    (fn [x y] (* (+ x y) 0.5))
    [1 10 15]))

(def some-values [{:key 1, :value 10, :other "bla"}, {:key 2, :value 13, :other "bla"}, {:key 1, :value 7, :other "bla"}])

(defn x-12 []
  (->> [{:key 1, :value 10, :other "bla"}, {:key 2, :value 13, :other "bla"}, {:key 1, :value 7, :other "bla"}]
       (reductions #(assoc %2 :value (+ (:value %1) (:value %2)))
                   {:value 0})
       next
       vec))

["anz-visa"
 "_ANZ_credit_card.csv"
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q1}}}
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q3}}}
 ]

["amp"
 "_AMP_TransactionHistory.csv"
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q1}}}
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q3}}}]

["anz-coy"
 "_ANZ_coy.csv"
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q1}}}
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q3}}}]

["anz-coy"
 "_ANZ_coy.csv"
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q1}}}
 #:actual-period{:year 2017, :period #:period{:type    #:db{:ident :period.type/quarterly},
                                              :quarter #:db{:ident :period.quarter/q3}}}]

["anz-visa" "_ANZ_credit_card.csv" #:actual-period{:year 2017, :period #:period{:type #:db{:ident :period.type/quarterly}, :quarter #:db{:ident :period.quarter/q1}}} #:actual-period{:year 2017, :period #:period{:type #:db{:ident :period.type/quarterly}, :quarter #:db{:ident :period.quarter/q3}}}] ["amp" "_AMP_TransactionHistory.csv" #:actual-period{:year 2017, :period #:period{:type #:db{:ident :period.type/quarterly}, :quarter #:db{:ident :period.quarter/q1}}} #:actual-period{:year 2017, :period #:period{:type #:db{:ident :period.type/quarterly}, :quarter #:db{:ident :period.quarter/q3}}}]