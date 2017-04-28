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
