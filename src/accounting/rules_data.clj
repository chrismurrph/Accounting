(ns accounting.rules-data
  (:require [accounting.meta :as meta]
            [accounting.util :as u]))

(def amp (first meta/bank-accounts))
(def coy (second meta/bank-accounts))
(def visa (u/third meta/bank-accounts))

;;
;; We won't know what to do with a record that matches these two rules. We want an error to always happen.
;; Then we will code around it as :investigate-further is obviously being overridden.
;;
(def test-rules {[amp :trash]               [{:field          :out/desc
                                              :logic-operator :and
                                              :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                               [:ends-with "PAYPAL AUSTRALIA"]]}]
                 [amp :investigate-further] [{:field          :out/desc
                                              :logic-operator :and
                                              :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                               [:ends-with "PAYPAL AUSTRALIA"]]}]
                 })

;;
;; For every transaction date that comes thru that is :office-expense we will require a
;; description
;; All the invented accounts will need to have description. So here :niim-trip
;;
(def q3-2017-rules {
                    ;; Upon closer investigation, this one is for trashing
                    [amp :trash]               [{:field          :out/desc
                                                 :logic-operator :and
                                                 :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                                  [:ends-with "PAYPAL AUSTRALIA"]]}]
                    [visa :trash]              [{:field          :out/desc
                                                 :logic-operator :single
                                                 :conditions     [[:starts-with "QANTAS AIRWAYS"]]}
                                                ]
                    [visa :exp/niim-trip]      [{:field          :out/desc
                                                 :logic-operator :or
                                                 :conditions     [[:starts-with "SKYBUS COACH SERVICE"]
                                                                  [:equals "RE & TK WILSDON PTY       KEITH"]
                                                                  [:starts-with "TIGER AIRWAYS AUSTRALIA"]
                                                                  [:starts-with "AUSDRAGON PTY LTD"]]}]
                    [visa :exp/office-expense] [{:field          :out/desc
                                                 :logic-operator :or
                                                 :conditions     [[:equals "TARGET 5009               ADELAIDE"]
                                                                  [:starts-with "DRAKE SUPERMARKETS"]
                                                                  [:starts-with "Z & Y BEYOND INTL PL"]]}]})

;;
;; From which bank account tells you which account to put the transaction to
;; The result of applying these rules will be a list of transactions at the
;; target account.
;; Apart from directing money to accounts it is also directed to :trash where
;; nothing further happens or :investigate-further where the end user will need to
;; investigate as to whether ought to be trashed or go to an (expense) account.
;;
(def permanent-rules
  {[visa :trash]                   [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:starts-with "CITY EAST IGA"]
                                                      [:starts-with "DAN MURPHY'S"]
                                                      [:starts-with "STRATH CORNER BAKERY"]
                                                      [:starts-with "PAYMENT THANKYOU"]
                                                      [:starts-with "WOOLWORTHS"]
                                                      [:starts-with "FEATHERS HOTEL"]
                                                      [:starts-with "BWS LIQUOR"]
                                                      [:starts-with "UNLEY SWIMMING CNTR"]
                                                      [:starts-with "NORWOOD SWIM SCHOOL"]
                                                      [:starts-with "SQ *OUT OF ZULULAND"]
                                                      [:starts-with "PANCAKE HOUSE"]
                                                      [:starts-with "FREWVILLE FOODLAND"]
                                                      [:starts-with "GREAT DREAM PTY LTD       HAYBOROUGH"]
                                                      [:starts-with "JETTY SURF"]
                                                      [:starts-with "COCKLES ON NORTH"]
                                                      [:starts-with "CORIOLE VINEYARDS"]]}]
   [visa :exp/office-expense]      [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "OFFICEWORKS"]]}]
   [visa :exp/petrol]              [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:starts-with "CALTEX"]
                                                      [:starts-with "BP"]
                                                      [:starts-with "X CONVENIENCE MT BARKE"]]}]
   [visa :exp/motor-vehicle]       [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:starts-with "PETER STEVENS MOTORC"]
                                                      [:starts-with "DPTI - EZYREG"]]}]
   [visa :exp/interest-expense]    [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "INTEREST CHARGED ON PURCHASES"]]}]
   [visa :exp/accounting-software] [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "XERO AUSTRALIA PTY LTD"]]}]
   [visa :exp/cloud-expense]       [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:starts-with "GOOGLE*SVCSAPPS SEASOF"]
                                                      [:starts-with "GOOGLE*SVCSAPPS STRAND"]
                                                      [:starts-with "LINODE.COM"]
                                                      [:starts-with "FastMail Pty Ltd"]]}]
   [visa :exp/computer-expense]    [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "ALLNEEDS COMPUTERS"]]}]
   [visa :exp/mobile-expense]      [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:equals "TELSTRA                   MELBOURNE"]]}]
   [visa :non-exp/private-health]  [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "HCF"]]}]
   [amp :trash]                    [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:ends-with "drawings"]
                                                      [:equals "Direct Entry Credit Item Ref: drawings Seaweed Software"]
                                                      [:starts-with "ATM Withdrawal - "]
                                                      [:starts-with "Purchase - Ideal Shoe Sto"]]}]
   [amp :investigate-further]      [{:field          :out/desc
                                     :logic-operator :and
                                     :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                      [:ends-with "PAYPAL AUSTRALIA"]]}]
   [coy :income/mining-sales]      [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "TRANSFER FROM MINES RESCUE PTY CS"]]}]
   [coy :income/poker-parse-sales] [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "TRANSFER FROM R T WILSON"]]}]
   [coy :income/bank-interest]     [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "CREDIT INTEREST PAID"]]}]
   [coy :exp/bank-fee]             [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:equals "ACCOUNT SERVICING FEE"]]}]
   [coy :non-exp/ato-payment]      [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:starts-with "ANZ INTERNET BANKING BPAY TAX OFFICE PAYMENT"]
                                                      [:starts-with "PAYMENT TO ATO"]]}]
   [coy :capital/drawings]         [{:field          :out/desc
                                     :logic-operator :and
                                     :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                      [:ends-with "4509499246191003"]]}
                                    {:field          :out/desc
                                     :logic-operator :and
                                     :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                      [:ends-with "CHRISTOPHER MURP"]]
                                     }]})

