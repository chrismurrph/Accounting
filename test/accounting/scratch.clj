(ns accounting.scratch)

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
