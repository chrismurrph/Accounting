(ns accounting.data.test-croquet
  (:require [accounting.data.croquet :as data]
            [accounting.time :as t]
            [accounting.util :as u]
            [clojure.test :as test]))

(def expected '({:type :income/game-fees, :when "31/01/2017", :amount 26.00M}
                 {:type :income/game-fees, :when "01/02/2017", :amount 48.00M}
                 {:type :income/game-fees, :when "03/02/2017", :amount 36.00M}
                 {:type :income/game-fees, :when "05/02/2017", :amount 30.00M}
                 {:type :income/game-fees, :when "06/02/2017", :amount 20.00M}
                 {:type :income/game-fees, :when "12/02/2017", :amount 45.00M}
                 {:type :income/game-fees, :when "13/02/2017", :amount 49.00M}
                 {:type :income/visiting-club, :when "13/02/2017", :amount 70.00M}
                 {:type :income/game-fees, :when "21/02/2017", :amount 15.00M}))

(defn between-two-dates []
  (let [begin (t/short-date-str->date "31/01/2017")
        end (t/short-date-str->date "21/02/2017")
        within? (t/get-within :when)]
    (->> data/receive-cash
         (within? begin end)
         (map t/show-ledger-record))))

(test/deftest croquet
  (test/is (= (between-two-dates) expected)))
