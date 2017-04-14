(ns accounting.rules-data
  (:require [accounting.meta :as meta]
            [accounting.util :as u]
            [accounting.time :as t]))

(def amp (first meta/bank-accounts))
(def coy (second meta/bank-accounts))
(def visa (u/third meta/bank-accounts))

(defn attach-period [period rules-in]
  (into {} (map (fn [[k v]]
                  [k (mapv #(assoc % :period period) v)]) rules-in)))

;;
;; For every transaction date that comes thru that is :office-expense we will require a
;; description TODO - there are more descriptions, need to take all from Xero
;; All the invented accounts will need to have description. So here :niim-trip
;;
;; Have introduce more specificity to these rules, that will be used if not nil:
;;   :on-date
;;   :between-dates-inclusive
;;   :amount
;;   - already has :period
;; Here the PayPal can be :on-date, and the niim-trip between dates
;; Will test the PayPal by having on wrong date - works!
;; TODO
;; S/also have whether multiple matches are allowed. For permanent ones default will be true.
;; For temporary ones like here the user migth have to specify
;;
(def -q3-2017-rules {
                     ;; Upon closer investigation, this one was personal spending
                     [amp :personal/amp]        [{:field          :out/desc
                                                  :on-dates       #{(t/long-date-str->date "20 Feb 2017")}
                                                  :logic-operator :and
                                                  :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                                   [:ends-with "PAYPAL AUSTRALIA"]]}]
                     [visa :personal/anz-visa]  [{:field          :out/desc
                                                  :logic-operator :single
                                                  :conditions     [[:starts-with "QANTAS AIRWAYS"]]}
                                                 ]
                     [visa :exp/niim-trip]      [{:field                   :out/desc
                                                  :between-dates-inclusive (t/inclusive-range "25 Jan 2017" "08 Feb 2017")
                                                  :logic-operator          :or
                                                  :conditions              [[:starts-with "SKYBUS COACH SERVICE"]
                                                                            [:equals "RE & TK WILSDON PTY       KEITH"]
                                                                            [:starts-with "TIGER AIRWAYS AUSTRALIA"]
                                                                            [:starts-with "AUSDRAGON PTY LTD"]]}]
                     [visa :exp/office-expense] [{:field          :out/desc
                                                  :logic-operator :or
                                                  :conditions     [[:equals "TARGET 5009               ADELAIDE"]
                                                                   [:starts-with "DRAKE SUPERMARKETS"]
                                                                   [:starts-with "Z & Y BEYOND INTL PL"]]}]})

(def -q2-2017-rules {
                     [amp :personal/amp] [{:field          :out/desc
                                           :on-dates       #{(t/long-date-str->date "12 Dec 2016") (t/long-date-str->date "8 Dec 2016")}
                                           :logic-operator :and
                                           :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                            [:ends-with "PAYPAL AUSTRALIA"]]}]})

(def -q1-2017-rules {
                     [amp :exp/travel]      [{:field          :out/desc
                                              :logic-operator :or
                                              :conditions     [[:equals "Internet banking external transfer 062202 10481871 - car rental"]
                                                               [:equals "ROAM TOLLING P/L          MELBOURNE"]]}]
                     [amp :exp/office-rent] [{:field          :out/desc
                                              :logic-operator :single
                                              :conditions     [[:equals "Internet banking external transfer 062202 10481871 - renting caravan"]]}]})

(def q3-2017-rules (attach-period {:period/tax-year 2017
                                   :period/quarter  :q3} -q3-2017-rules))

(def q2-2017-rules (attach-period {:period/tax-year 2017
                                   :period/quarter  :q2} -q2-2017-rules))

(def q1-2017-rules (attach-period {:period/tax-year 2017
                                   :period/quarter  :q1} -q1-2017-rules))

;;
;; From which bank account tells you which account to put the transaction to
;; The result of applying these rules will be a list of transactions at the
;; target account.
;; Apart from directing money to accounts it is also directed to :personal where
;; nothing further happens or :investigate-further where the end user will need to
;; investigate as to whether ought to be trashed or go to an (expense) account.
;; :personal is personal spending (above money came in from drawings) from a non-company bank account.
;; Good to have it in an account to see how much of a drain personal spending makes
;; from quarter to quarter.
;; Also good to have so that this system can correctly calculate the final bank balance.
;; In the future we could order trash by amount, including description, so we can see
;; what is causing the drain on finances.
;; Any -ive on :personal is good, means drew out into the account and did not spend it that quarter
;; So good defintion is decrease in personal wealth over that quarter, in the bank account
;;
(def permanent-rules
  {[visa :personal/anz-visa]       [{:field          :out/desc
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
                                                      [:starts-with "CORIOLE VINEYARDS"]
                                                      [:equals "HAZELS POOLSIDE CAFE      HAZELWOOD PAR"]
                                                      [:equals "J TSIMBINOS & PARTNERS    ADELAIDE"]
                                                      [:equals "NRMA LIMITED - 2          NORTH STRATHF"]
                                                      [:equals "COLES EXPRESS 1943        ROSE PARK"]
                                                      [:equals "HOTEL ROYAL               TORRENSVILLE"]
                                                      [:equals "NATIONAL PHARMACIES       VICTOR HARBOR"]
                                                      [:equals "RECOVERIES CORP           MELBOURNE"]
                                                      [:starts-with "CASH ADVANCE"]
                                                      [:equals "LUNCH ON ANGAS            ADELAIDE"]
                                                      [:equals "IGA HUTT ST               ADELAIDE"]
                                                      [:equals "THE YIROS HUTT BY YA      ADELAIDE"]
                                                      [:equals "GENERAL HAVELOCK PTY L    ADELAIDE"]
                                                      [:equals "PARADE SUPERMARKET PTY    NORWOOD"]]}]
   [visa :exp/office-expense]      [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "OFFICEWORKS"]]}]
   [visa :exp/petrol]              [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:starts-with "CALTEX"]
                                                      [:starts-with "BP"]
                                                      [:contains "PETROL"]
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
                                                      [:equals "GOOGLE*SVCSAPPSSTRANDZ-OR SINGAPORE"]
                                                      [:starts-with "GOOGLE*SVCSAPPS STRAND"]
                                                      [:starts-with "LINODE.COM"]
                                                      [:starts-with "FastMail Pty Ltd"]]}]
   [visa :exp/computer-expense]    [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "ALLNEEDS COMPUTERS"]]}]
   [visa :exp/mobile-expense]      [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:equals "TELSTRA                   MELBOURNE"]]}]
   [visa :exp/isp]                 [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:equals "IINET LIMITED             PERTH"]]}]
   [visa :non-exp/private-health]  [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "HCF"]]}]
   [visa :exp/bank-fee]            [{:field          :out/desc
                                     :dominates      #{:personal/anz-visa}
                                     :logic-operator :or
                                     :conditions     [[:equals "INTEREST CHARGED ON CASH"]
                                                      [:equals "REWARD PROGRAM FEE"]
                                                      [:equals "ANNUAL FEE"]
                                                      [:equals "CASH ADVANCE FEE - INTERNET"]
                                                      [:equals "LATE PAYMENT FEE"]]}]
   [amp :personal/amp]             [{:field          :out/desc
                                     :logic-operator :or
                                     :conditions     [[:ends-with "drawings"]
                                                      [:equals "Direct Entry Credit Item Ref: drawings Seaweed Software"]
                                                      [:starts-with "ATM Withdrawal - "]
                                                      [:starts-with "Purchase - Ideal Shoe Sto"]
                                                      [:equals "Outward Dishonour Fee - Electronic"]
                                                      [:starts-with "Dishonour of: Direct Entry Debit Item Ref:"]
                                                      [:equals "ATM Direct Charge"]
                                                      [:equals "Reversal of ATM Direct Charge"]
                                                      [:starts-with "Reversal of ATM Withdrawal"]
                                                      [:equals "Purchase - IGA HUTT ST              ADELAIDE     AU"]
                                                      [:equals "Purchase - LUNCH ON ANGAS      ADELAIDE          AU"]
                                                      ;; Putting money in, like a reverse drawings
                                                      [:equals "Internet banking scheduled external transfer 012030 198414945"]
                                                      [:equals "Purchase/Cash out - H.B. FOODS PTY LIMIT HAZELBROOK NSW"]
                                                      [:equals "Purchase/Cash out - COLES EXPRESS 1724       FAULCONBRIDGEAU"]]}
                                    {:field          :out/desc
                                     :logic-operator :and
                                     :conditions     [[:starts-with "Internet banking bill payment"]
                                                      [:ends-with "4509499246191003"]]}]
   [coy :income/mining-sales]      [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "TRANSFER FROM MINES RESCUE PTY CS"]]}]
   [coy :income/poker-parse-sales] [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "TRANSFER FROM R T WILSON"]]}]
   [coy :income/bank-interest]     [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:starts-with "CREDIT INTEREST PAID"]]}]
   [coy :exp/office-rent]          [{:field          :out/desc
                                     :logic-operator :and
                                     :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                      [:ends-with "313 TRU"]]}]
   [coy :exp/bank-fee]             [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:equals "ACCOUNT SERVICING FEE"]]}]
   [coy :exp/interest-expense]     [{:field          :out/desc
                                     :logic-operator :single
                                     :conditions     [[:equals "DEBIT INTEREST CHARGED"]]}]
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

