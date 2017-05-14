(ns accounting.statements)

;;
;; Sign change can be done afterwards
;; I'll just take the cents out when it comes to the report
;;
(defn add [kws]
  (fn [tb]
    (->> (map tb kws)
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
                           {"Donations" nil}
                           {"Filing Fees" :exp/filing-fees}
                           {"Fines and penalties" :exp/fines-penalties}
                           {"Freight & Cartage" :exp/freight-courier}
                           {"General Expenses" nil}
                           {"Interest Paid" :exp/ato-interest}
                           {"Motor Vehicle Expenses" (fixed-amount 300.00M)}
                           {"Printing & Stationary" nil}
                           {"Office Supplies" nil}
                           ])
