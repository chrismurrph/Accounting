(ns accounting.statements
  (:require [accounting.util :as u]))

(defn tb-access [tb]
  (fn [kw]
    (u/err-warn (kw tb) (str kw " not found in trial balance\n" (u/pp-str tb)))))

;;
;; Sign change can be done afterwards
;; I'll just take the cents out when it comes to the report
;;
(defn add [kws]
  (fn [tb]
    (->> (map (tb-access tb) kws)
         (remove nil?)
         (reduce +))))

(defn fixed-amount [amt]
  (fn [tb]
    amt))

;;
;; Each heading has a function that can be applied to a trial balance. When hof, the input required for the
;; returned inner function is the tb. Thus can create a P/L or B/S or whatever from any tb.
;; Some other function will change the sign so don't worry about that here
;;
(def income-headings [{"Computer Support" :income/mining-sales}
                      {"Interest Received" (add [:income/bank-interest
                                                 :income/div-7a-interest
                                                 :income/ato-interest
                                                 ]
                                                )}])

(def expenditure-headings [{"Accountancy Fees" :exp/accounting-expense}
                           {"Bank Charges" :exp/bank-fee}
                           {"Bookkeeping Fees" nil}
                           {"Computer Expenses" :exp/computer-expenses}
                           {"Depreciation" (add [:exp/plant-equip-deprec
                                                 :exp/low-value-pool-deprec])}
                           {"Directors' Bonus" :exp/directors-bonus}
                           {"Donations" nil}
                           {"Filing Fees" :exp/filing-fees}
                           {"Fines and penalties" :exp/fines-penalties}
                           {"Freight & Cartage" :exp/freight-courier}
                           {"General Expenses" nil}
                           {"Interest Paid" :exp/ato-interest}
                           {"Light & Power" :exp/light-power-heating}
                           {"Motor Vehicle Expenses" (fixed-amount 300.00M)}
                           {"Printing & Stationary" nil}
                           {"Office Supplies" :exp/books-periodicals}
                           {"Rent" :exp/rent}
                           {"Storage Fees" nil}
                           {"Subscriptions" :exp/subscriptions}
                           {"Telephone" :exp/mobile-expense}
                           {"Travelling Expenses" :exp/national-travel}
                           {"Travel Allowance - Overseas" nil}
                           ])

;;
;; string on a line that has a name and a value
;;
(defn ->line
  ([tb sign]
   (fn [[heading f]]
     (let [txt (if f (u/no-dec-pl (str (u/round0 (sign (f tb))))) "n/a")]
       (str heading ((u/left-pad-spaces (- 40 (count heading))) txt)))))
  ([tb]
   (->line tb +)))

;;
;; A section is a vector of strings where each string is a line
;;
(defn ->report-section [tb]
  (assert (map? tb))
  (fn [sign]
    (fn [report-template]
      (assert (vector? report-template))
      (let [rep-line-fn (comp (->line tb sign) first)]
        (mapv rep-line-fn report-template)))))

;;
;; Produces a line that can be conj-ed onto the end of the section
;; it is summing. The sum will be needed for other things so is put
;; in first place.
;;
(defn sum-section [tb]
  (assert (map? tb))
  (fn [sign]
    (fn [report-template]
      (assert (vector? report-template))
      (let [val-fn (comp (fn [f] (when f (u/round0 (f tb)))) val first)
            sum (->> report-template
                     (map val-fn)
                     (remove nil?)
                     (reduce +))]
        [sum ((u/left-pad-spaces 40) (u/no-dec-pl (str (sign sum))))]))))
