(ns accounting.croquet-context
  (:require [accounting.meta.seaweed :as meta]
            [accounting.match :as m]
            [accounting.croquet-rules-data :as croquet-d]
            [accounting.util :as u]))

(def current-croquet-rules
  (->> croquet-d/rules
       m/canonicalise-rules))
