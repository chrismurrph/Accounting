(ns accounting.data.croquet
  (:require
    [accounting.time :as t]))

(def bendigo :bank/bendigo)

(def splits {:house-keeping-and-cleaning {:exp/house-keeping 0.666M :exp/cleaning 0.333M}})

;;
;; whos are important for expenses and are references in bank rules
;;
(def expenses-owed
  [{:when   (t/short-date-str->date "21/02/2017")
    :who    "Bob"
    :amount 244.58M}])

(def receive-cash
  [{:type   :game-fees
    :when   (t/short-date-str->date "21/02/2017")
    :amount 15.00M}
   {:type   :game-fees
    :when   (t/short-date-str->date "22/02/2017")
    :amount 31.00M}
   {:type   :game-fees
    :when   (t/short-date-str->date "24/02/2017")
    :amount 26.00M}
   {:type   :game-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 32.00M}
   {:type   :game-fees
    :when   (t/short-date-str->date "27/02/2017")
    :amount 20.00M}

   {:type   :membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "Lorton"}
   {:type   :membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "Jago"}
   {:type   :membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 470.00M
    :from   "Richmond"}
   {:type   :membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "English"}
   {:type   :membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "Williams"}

   {:type   :visiting-club
    :when   (t/short-date-str->date "26/02/2017")
    :amount 50.00M
    :from   "Millswood"}
   {:type   :member-shirt-payments
    :when   (t/short-date-str->date "26/02/2017")
    :amount 90.00M}])

(def feb-rules
  {
   [bendigo :income/events] [{:logic-operator :and
                              :conditions     [[:out/desc :ends-with "DEPOSIT - FISHER J"]
                                               [:out/amount :equals 200.00M]]}]
   })

(def mar-rules
  {
   })

(def permanent-rules
  {
   [bendigo :income/membership-fees]           [{:logic-operator :and
                                                 :conditions     [[:out/desc :starts-with "DIRECT CREDIT"]
                                                                  [:out/amount :equals 235.00M]]}]
   [bendigo :ledger/cash-deposits]             [{:logic-operator :single
                                                 :conditions     [[:out/desc :equals "DEPOSIT - CASH"]]}]
   [bendigo :ledger/cash-and-cheque-deposits]  [{:logic-operator :single
                                                 :conditions     [[:out/desc :starts-with "DEPOSIT - CASH & CHEQUE(S)"]]}]
   [bendigo :ledger/expenses-owed]             [{:logic-operator :and
                                                 :who            "Bob Vincent"
                                                 :conditions     [[:out/desc :starts-with "PAY ANYONE"]
                                                                  [:out/desc :ends-with "RG VINCENT"]]}]
   [bendigo :exp/printing]                     [{:logic-operator :and
                                                 :conditions     [[:out/desc :starts-with "PAY ANYONE"]
                                                                  [:out/desc :ends-with "JUDY WILLA"]
                                                                  [:out/amount :less-than 100.00M]]}]
   [bendigo :exp/uniforms]                     [{:logic-operator :and
                                                 :conditions     [[:out/desc :starts-with "PAY ANYONE"]
                                                                  [:out/desc :ends-with "VALERIE TR"]]}]
   [bendigo :exp/house-keeping]                [{:logic-operator :and
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - CASH"]
                                                                  [:out/amount :equals -100.00M]]}]
   [bendigo :exp/cleaning]                     [{:logic-operator :and
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - CASH"]
                                                                  [:out/amount :equals -50.00M]]}]
   [bendigo :exp/water]                        [{:logic-operator :single
                                                 :conditions     [[:out/desc :starts-with "BILL PAYMENT BPAY TO: SA WATER"]]}]
   [bendigo :split/house-keeping-and-cleaning] [{:logic-operator :and
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - CASH"]
                                                                  [:out/amount :equals -150.00M]]}]
   })

(def general-ledger {:bank/bendigo           0M
                     :income/membership-fees 0M
                     :exp/uniforms           0M
                     })
