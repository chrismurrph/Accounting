(ns accounting.data.croquet
  (:require
    [accounting.time :as t]
    [accounting.common :as c]))

(def bendigo :bank/bendigo)

(def splits {:house-keeping-and-cleaning {:exp/house-keeping 0.666M :exp/cleaning 0.333M}})

(def expenses-owed
  [{:type   :exp/garden
    :when   (t/short-date-str->date "21/02/2017")
    :who    "Bob"
    :amount -99.90M}
   {:type   :exp/general-upkeep
    :when   (t/short-date-str->date "21/02/2017")
    :who    "Bob"
    :amount -72.34M}
   {:type   :exp/stationary
    :when   (t/short-date-str->date "21/02/2017")
    :who    "Bob"
    :amount -72.34M}

   {:type   :exp/stationary
    :when   (t/short-date-str->date "22/02/2017")
    :who    "Bob"
    :amount -9.00M}
   {:type   :exp/garden
    :when   (t/short-date-str->date "22/02/2017")
    :who    "Bob"
    :amount -53.90M}
   {:type   :exp/garden
    :when   (t/short-date-str->date "22/02/2017")
    :who    "Bob"
    :amount -8.05M}
   {:type   :exp/general-upkeep
    :when   (t/short-date-str->date "22/02/2017")
    :who    "Bob"
    :amount -10.65M}
   {:type   :exp/general-upkeep
    :when   (t/short-date-str->date "22/02/2017")
    :who    "Bob"
    :amount -35.00M}
   ])

(def receive-cash-and-cheques
  [{:type   :income/game-fees
    :when   (t/short-date-str->date "07/02/2017")
    :amount 30.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "14/02/2017")
    :amount 27.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "15/02/2017")
    :amount 31.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "17/02/2017")
    :amount 37.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "19/02/2017")
    :amount 10.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "20/02/2017")
    :amount 32.00M}
   {:type   :income/visiting-club
    :when   (t/short-date-str->date "20/02/2017")
    :amount 45.00M}
   {:type   :income/events
    :when   (t/short-date-str->date "20/02/2017")
    :amount 275.00M}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "20/02/2017")
    :amount 235.00M
    :from   "Rose"}])

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
    :when   (t/short-date-str->date "12/02/2017")
    :amount 45.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "13/02/2017")
    :amount 49.00M}
   {:type   :income/visiting-club
    :when   (t/short-date-str->date "13/02/2017")
    :amount 70.00M}

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
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "26/02/2017")
    :amount 117.50M
    :from   "Chotty"}

   {:type   :income/visiting-club
    :when   (t/short-date-str->date "26/02/2017")
    :amount 50.00M
    :from   "Millswood"}
   {:type   :income/member-shirt-payments
    :when   (t/short-date-str->date "26/02/2017")
    :amount 90.00M}

   {:type   :income/game-fees
    :when   (t/short-date-str->date "28/02/2017")
    :amount 10.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "01/03/2017")
    :amount 16.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "03/03/2017")
    :amount 12.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "05/03/2017")
    :amount 14.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "06/03/2017")
    :amount 22.00M}

   {:type   :income/game-fees
    :when   (t/short-date-str->date "07/03/2017")
    :amount 14.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "08/03/2017")
    :amount 26.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "10/03/2017")
    :amount 34.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "12/03/2017")
    :amount 16.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "13/03/2017")
    :amount 35.00M}

   {:type   :income/membership-fees
    :when   (t/short-date-str->date "14/03/2017")
    :amount 235.00M
    :from   "Hugh Duckett"}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "14/03/2017")
    :amount 235.00M
    :from   "Arthur Ruttley"}

   {:type   :income/game-fees
    :when   (t/short-date-str->date "14/03/2017")
    :amount 17.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "15/03/2017")
    :amount 27.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "19/03/2017")
    :amount 28.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "20/03/2017")
    :amount 24.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "17/03/2017")
    :amount 24.00M}
   {:type   :income/membership-fees
    :when   (t/short-date-str->date "20/03/2017")
    :amount 235.00M
    :from   "Margo"}

   {:type   :income/game-fees
    :when   (t/short-date-str->date "21/03/2017")
    :amount 6.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "23/03/2017")
    :amount 18.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "26/03/2017")
    :amount 20.00M}
   {:type   :income/game-fees
    :when   (t/short-date-str->date "27/03/2017")
    :amount 29.00M}

   {:type   :income/events
    :when   (t/short-date-str->date "22/03/2017")
    :amount 195.00M}

   {:type   :exp/pennants
    :when   (t/short-date-str->date "22/03/2017")
    :amount 40.00M}

   ])

;;
;; This is like a post-query step because :already-transferred to have only temporary significance. Needed for when
;; ledger and bank amounts overlap. For instance when cash is collected and banked on the same day, with some of
;; the collection being after the banking.
;;
(defn init [records]
  (mapv #(-> %
            (assoc :already-transferred false)) records))

(def -ledgers {:cash-and-cheque-deposits {:recalc-date (t/short-date-str->date "30/01/2017") :records (init receive-cash-and-cheques) :op <}
               :cash-deposits            {:recalc-date (t/short-date-str->date "30/01/2017") :records (init receive-cash) :op <}
               :expenses-owed            {:recalc-date (t/short-date-str->date "20/02/2017") :records (init expenses-owed) :op >}})

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
   [bendigo :equity/funds-introduced]         [{:logic-operator :and
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
                                                                  [:out/desc :ends-with "RG VINCENT"]
                                                                  [:out/amount :not-equals -120.00M]]}]
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
   [bendigo :exp/hedge-clipping]               [{:logic-operator :and
                                                 :conditions     [[:out/desc :equals "PAY ANYONE BBL147728356THE SOUTH TERRACE 0105739933RG VINCENT"]
                                                                  [:out/amount :equals -120.00M]
                                                                  ;; Perhaps it is 2nd Wed of month, but we can make that condition later if needs be...
                                                                  [:out/date :day-of-month 8]]}]
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

(def data {:gl      {:bank/bendigo                 0M
                     :equity/funds-introduced      0M
                     :income/membership-fees       0M
                     :income/bank-interest         0M
                     :income/game-fees             0M
                     :income/events                0M
                     :income/visiting-club         0M
                     :income/member-shirt-payments 0M
                     :exp/uniforms                 0M
                     :exp/rent                     0M
                     :exp/bank-fees                0M
                     :exp/bank-interest            0M
                     :exp/greenkeeping             0M
                     :exp/printing                 0M
                     :exp/cleaning                 0M
                     :exp/plumbing                 0M
                     :exp/pennants                 0M
                     :exp/hedge-clipping           0M
                     :exp/garden                   0M
                     :exp/general-upkeep           0M
                     :exp/stationary               0M
                     :exp/water                    0M
                     :exp/food                     0M
                     :exp/house-keeping            0M
                     :exp/insurance                0M
                     :exp/building-maintenance     0M
                     :exp/equipment-maintenance    0M
                     :exp/uniform                  0M
                     :exp/po-box                   0M
                     :exp/alcohol                  0M
                     }
           :ledgers -ledgers})
