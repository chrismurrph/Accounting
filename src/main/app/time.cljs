(ns app.time
  (:require
    [cljs-time.format :as f]))

(def -date-formatter (f/formatter "dd/MM/yyyy"))
(def format-date #(f/unparse -date-formatter %))
(def show format-date)

