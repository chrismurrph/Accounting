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

(def a-1 [{:day "01/01" :balance 100.00}, {:day "05/01" :balance -50.00}, {:day "10/01" :balance -100.00},
          {:day "14/01" :balance 50.00}, {:day "17/01" :balance -200.00}])

(def a-2 [{:day "01/01" :balance 100.00}, {:day "05/01" :balance -50.00}])

(defn x-13 []
  (filterv #(-> % :balance neg?) 1))

(defn dec-date [d]
  d)

(defn x-14 []
  (->> a-1
       (partition-by #(-> % :balance neg?))
       (drop-while #(-> % first :balance pos?))
       (mapcat identity)
       (map (juxt :day :balance))
       (partition-all 2 1)
       (keep (fn [[[date-1 val-1] [date-2 val-2]]]
               (cond
                 (neg? val-1) (cond-> {:start date-1
                                       :value val-1}
                                      date-2 (assoc :end (dec-date date-2)))
                 (pos? val-1) nil
                 :else {:start date-2
                        :value val-1})))))

(defn x-15 []
  (let [[[mentry-k mentry-v]] {:a :b}]
    [mentry-k mentry-v]))

(defn x-16 []
  (let [[mentry-k mentry-v] (first {:a :b})]
    [mentry-k mentry-v]))

(defn x-17 []
  (let [[mentry-k mentry-v] (nth {:a :b} 0)]
    [mentry-k mentry-v]))

(defn x-18 []
  (let [[mentry-k mentry-v] (get {:a :b} :a)]
    [mentry-k mentry-v]))


(def vector-of-maps [{:a 1 :b 2} {:a 3 :b 4}])

(defn update-map [m f]
  (reduce-kv (fn [m k v]
               (assoc m k (f v)))
             {}
             m))

(defn x-19 []
  (map #(update-map % inc) vector-of-maps))

(defonce state (atom {:player
                      {:cells [{:x 123 :y 456 :radius 1.7 :area 10}
                               {:x 456 :y 789 :radius 1.7 :area 10}]}}))
(defn x-20 []
  (let [when-idx 0
        new-x -1
        new-y -1]
    (swap! state update-in [:player :cells]
           (fn [v] (vec (map-indexed (fn [n {:keys [x y] :as m}]
                                       (if (= n when-idx)
                                         (assoc m :x new-x :y new-y)
                                         m))
                                     v))))))

(defn x-21 []
  (let [when-idx 0
        new-x -1
        new-y -1]
    (swap! state update-in [:player :cells when-idx] #(assoc % :x new-x :y new-y))))

(defn replace-in [v [idx new-val]]
  (concat (subvec v 0 idx)
          [new-val]
          (subvec v (inc idx))))

;; (->> haystack
;; (keep-indexed #(when (= %2 needle) %1))
;; first)
(defn x-22 []
  (let [when-x 456
        new-x -1
        new-y -1]
    (swap! state update-in [:player :cells]
           (fn [v] (->> v
                        (keep-indexed (fn [idx {:keys [x] :as m}]
                                        (when (= x when-x) [idx (assoc m :x new-x :y new-y)])))
                        first
                        (replace-in v))))))

(def data-23 {:name ["Wut1" "Wut2"] :desc ["But1" "But2"]})

(defn x-23 []
  (->> (apply map vector (vals data-23))
       (map (fn [ks vs] (mapcat vector ks vs)) (repeat (keys data-23)))
       (map #(->> %
                 (partition 2)
                 (map vec)
                 (into {})))))