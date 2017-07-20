(ns app.om-helpers)

;;
;; ident function is the same as the one on a component.
;; You give it a hash-map, it will give you an ident back.
;;
(defn sort-idents [upper-limit-count ident-f sort-by-f]
  (fn [st idents]
    (if (> (count idents) upper-limit-count)
      idents
      (->> idents
           (map #(get-in st %))
           (sort-by sort-by-f)
           (mapv ident-f)))))
