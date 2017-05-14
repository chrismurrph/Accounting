(ns accounting.all-tests
  (:require [clojure.test :as test]))

(defn run []
  #_(test/run-all-tests)
  (test/run-tests 'accounting.test-croquet 'accounting.test-gl 'accounting.test-match 'accounting.test-meta
                  'accounting.test-rules-data 'accounting.test-seaweed 'accounting.test-time
                  'accounting.data.test-croquet 'accounting.data.test-seaweed))
