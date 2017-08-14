(ns app.om-helpers
  (:require [om.next :as om]))

;;
;; To remove an id from refs and tables.
;;
(defn delete [state id ident-kw ref-in]
  (assert (and state id ident-kw ref-in))
  (assert (vector? ref-in))
  (let [ident [ident-kw id]]
    (-> state
        (update-in ref-in #(vec (remove #{ident} %)))
        (update ident-kw dissoc id))))

(defn make-temp-id [txt]
  (om/tempid))

(defn make-temp-id-debug [txt]
  (let [res (om/tempid)]
    (println ">>" txt ":" res)
    res))

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

;;
;; selected-f? takes m and returns m, so
;; #(:ui/selected? %)
;; select-f takes a hash-map and returns a hash-map
;; (the table row in default db format)
;; Usually it will be something like:
;; #(assoc % :ui/selected? true)
;; un-select-f:
;; #(assoc % :ui/selected? false)
;;
(defn select-one [ident-f selected-f? select-f unselect-f]
  (fn [st master-join to-select-ident]
    (assert (vector? master-join))
    (let [edge-idents (get-in st master-join)
          _ (assert (seq edge-idents) (str "No details at " master-join))
          edge-maps (map #(get-in st %) edge-idents)
          selected-map (some #(when (selected-f? %) %) edge-maps)
          selected-ident (when selected-map (ident-f selected-map))]
      (cond-> st
              selected-ident (update-in selected-ident #(unselect-f %))
              ;; Ensures that clicking on a selected will un-select
              (not= selected-ident to-select-ident) (update-in to-select-ident #(select-f %))))))
