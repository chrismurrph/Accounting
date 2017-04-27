(ns accounting.data.croquet
  (:require
    [accounting.time :as t]
    [accounting.common :as c]))

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
  [{:type   :income/game-fees
    :when   (t/short-date-str->date "31/01/2017")
    :amount 26.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "01/02/2017")
    :amount 48.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "03/02/2017")
    :amount 36.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "05/02/2017")
    :amount 30.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "06/02/2017")
    :amount 20.00M}

   {:type   :income/game-fees
    :when   (t/short-date-str->date "21/02/2017")
    :amount 15.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "22/02/2017")
    :amount 31.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "24/02/2017")
    :amount 26.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 32.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "27/02/2017")
    :amount 20.00M}

   {:type   :income/membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "Lorton"}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "Jago"}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 470.00M
    :from   "Richmond"}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "English"}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 235.00M
    :from   "Williams"}

   {:type   :income/visiting-club
    :when   (t/short-date-str->date "26/02/2017")
    :amount 50.00M
    :from   "Millswood"}
   {:type   :income/member-shirt-payments
    :when   (t/short-date-str->date "26/02/2017")
    :amount 90.00M}])

(def -ledgers {:cash-deposits {:recalc-date (t/short-date-str->date "30/01/2017") :records receive-cash}
               :expenses-owed {:recalc-date (t/short-date-str->date "07/02/2017") :records expenses-owed}})

(def -feb-rules
  {
   [bendigo :exp/food] [{:logic-operator :and
                         :on-dates       #{(t/long-date-str->date "14 Feb 2017")}
                         :conditions     [[:out/desc :ends-with "GILLIAN JU"]
                                          [:out/amount :equals -50.00M]]}]
   })

(def feb-rules (c/attach-period {:period/year  2017
                                 :period/month :feb} -feb-rules))

(def -mar-rules
  {
   [bendigo :exp/uniform] [{:logic-operator :single
                            :conditions     [[:out/desc :ends-with "P FAIRLIE  UNIFORM"]]}]
   })
(def mar-rules (c/attach-period {:period/year  2017
                                 :period/month :mar} -mar-rules))

(def permanent-rules
  {
   [bendigo :capital/funds-introduced]         [{:logic-operator :and
                                                 :on-dates       #{(t/long-date-str->date "08 Feb 2017")}
                                                 :conditions     [[:out/desc :equals "DIRECT CREDIT SOUTH TERRACE CR    0723041266 TRANSFER FROM BANK"]
                                                                  [:out/amount :equals 4581.83M]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :equals "PAY ANYONE BBL157471327THE SOUTH TERRACE 0115887966SOUTH TERR"]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :starts-with "TRANSFER FROM CLOSED"]]}]
   [bendigo :income/events]                    [{:logic-operator :and
                                                 :conditions     [[:out/desc :contains "FISHER J"]
                                                                  [:out/amount :equals 200.00M]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :ends-with "WARREN SEY"]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :contains "BESPOKE SA"]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :equals "DEPOSIT - CASH BAR TAKINGS"]]}
                                                ]
   [bendigo :income/membership-fees]           [{:logic-operator :and
                                                 :conditions     [[:out/desc :starts-with "DIRECT CREDIT"]
                                                                  [:out/amount :less-than 251.00M]
                                                                  [:out/amount :greater-than 234.00M]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :equals "PAY ANYONE CBA00602964 THE SOUTH TERRACE 0117513711JODIE LUIJ"]]}
                                                {:logic-operator :and
                                                 :conditions     [[:out/desc :not-starts-with "DIRECT CREDIT"]
                                                                  [:out/desc :ends-with "MEM SUB"]
                                                                  [:out/amount :less-than 251.00M]
                                                                  [:out/amount :greater-than 234.00M]]}]
   [bendigo :income/bank-interest]             [{:logic-operator :single
                                                 :conditions     [[:out/desc :equals "INTEREST"]]}]
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
                                                                  [:out/amount :equals -50.00M]]}
                                                {:logic-operator :and
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - CASH CLEANER"]
                                                                  [:out/amount :equals -50.00M]]}]
   [bendigo :exp/water]                        [{:logic-operator :single
                                                 :conditions     [[:out/desc :starts-with "BILL PAYMENT BPAY TO: SA WATER"]]}]
   [bendigo :exp/building-maintenance]         [{:logic-operator :single
                                                 :conditions     [[:out/desc :ends-with "OLDE STYLE"]]}]
   [bendigo :exp/equipment-maintenance]        [{:logic-operator :single
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - EFTPOS TRINITY MOWER CENTR    NORWOOD 7090"]]}]
   [bendigo :exp/general-upkeep]               [{:logic-operator :and
                                                 :conditions     [[:out/desc :starts-with "WITHDRAWAL - EFTPOS BUNNINGS"]
                                                                  [:out/amount :greater-than -100.00M]]}]
   [bendigo :exp/insurance]                    [{:logic-operator :single
                                                 :conditions     [[:out/desc :contains "VERO INSURANCE"]]}]
   [bendigo :exp/plumbing]                     [{:logic-operator :single
                                                 :conditions     [[:out/desc :contains "PLUMBER"]]}]
   [bendigo :exp/pennants]                     [{:logic-operator :single
                                                 :conditions     [[:out/desc :ends-with "CROQUET SA"]]}]
   [bendigo :exp/greenkeeping]                 [{:logic-operator :single
                                                 :conditions     [[:out/desc :ends-with "LENNON FAM"]]}]
   [bendigo :exp/po-box]                       [{:logic-operator :single
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - CASH POST OFFICE"]]}]
   [bendigo :exp/rent]                         [{:logic-operator :single
                                                 :conditions     [[:out/desc :ends-with "ADELAIDE C"]]}]
   [bendigo :exp/alcohol]                      [{:logic-operator :or
                                                 :conditions     [[:out/desc :starts-with "WITHDRAWAL - EFTPOS DAN MURPHY'S"]
                                                                  [:out/desc :equals "RETAIL PURCHASE ARAB STEED HOTE"]]}]
   [bendigo :exp/stationary]                   [{:logic-operator :and
                                                 :conditions     [[:out/desc :ends-with "MRS MARY M"]
                                                                  [:out/amount :less-than 20.00M]]}
                                                ]
   [bendigo :exp/bank-fees]                    [{:logic-operator :single
                                                 :conditions     [[:out/desc :starts-with "TRANSACTION FEES CHARGED"]]}
                                                {:logic-operator :single
                                                 :conditions     [[:out/desc :starts-with "DEBIT CARD FEE"]]}]
   [bendigo :split/house-keeping-and-cleaning] [{:logic-operator :and
                                                 :conditions     [[:out/desc :equals "WITHDRAWAL - CASH"]
                                                                  [:out/amount :equals -150.00M]]}]
   })

(def data {:gl      {:bank/bendigo             0M
                     :capital/funds-introduced 0M
                     :income/membership-fees   0M
                     :income/bank-interest     0M
                     :income/game-fees         0M
                     :exp/uniforms             0M
                     :exp/rent                 0M
                     :exp/bank-fees            0M
                     :exp/bank-interest        0M
                     :exp/greenkeeping         0M
                     :exp/printing             0M
                     :exp/cleaning             0M
                     :exp/plumbing             0M
                     :exp/pennants             0M
                     }
           :ledgers -ledgers})
