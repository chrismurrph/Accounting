(ns accounting.data.seaweed
  (:require [accounting.util :as u]
            [accounting.time :as t]
            [accounting.common :as c]))

(def amp :bank/amp)
(def coy :bank/anz-coy)
(def visa :bank/anz-visa)

(def splits {:agl-gas {:exp/light-power-heating 0.2M :personal/anz-visa 0.8M}})

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
                     [amp :exp/travel]           [{:field          :out/desc
                                                   :logic-operator :or
                                                   :conditions     [[:equals "Internet banking external transfer 062202 10481871 - car rental"]
                                                                    [:equals "ROAM TOLLING P/L          MELBOURNE"]]}]
                     [amp :exp/accomodation]     [{:field          :out/desc
                                                   :logic-operator :single
                                                   :conditions     [[:equals "Internet banking external transfer 062202 10481871 - renting caravan"]]}]
                     [visa :exp/travel]          [{:field          :out/desc
                                                   :logic-operator :or
                                                   :conditions     [[:equals "MILDURA INLANDER          MILDURA"]
                                                                    [:equals "TRAVELLERS REST MTL   WEE THALLE    NSW"]
                                                                    ;; Actually petrol - putting in here b/c I know it won't be removed
                                                                    ;; by my accountant
                                                                    [:equals "UNITED MEDLOW BATH        MEDLOW BATH"]]}]
                     [visa :exp/food]            [{:field          :out/desc
                                                   :logic-operator :or
                                                   :conditions     [[:equals '"M J Brown & S E Thomps    GOOLGOWI"]
                                                                    [:equals "BURONGA IGA X-PRESS       BURONGA"]
                                                                    [:equals "THE GOLDEN TRIANGLE       GRENFELL"]
                                                                    [:equals "SUBWAY KELSO              KELSO"]
                                                                    [:equals "THE FRUIT HOUSE           HAZELBROOK"]
                                                                    [:equals "Purchase/Cash out - H.B. FOODS PTY LIMIT HAZELBROOK NSW"]
                                                                    [:equals "Purchase/Cash out - COLES EXPRESS 1724       FAULCONBRIDGEAU"]
                                                                    [:equals "HAZELBROOK BUTCHERY       HAZELBROOK"]
                                                                    ]}]
                     [visa :exp/freight-courier] [{:field          :out/desc
                                                   :logic-operator :single
                                                   :conditions     [[:equals "POST   HAZELBROOK LP      HAZELBROOK"]]}]
                     [visa :personal/anz-visa]   [{:field          :out/desc
                                                   :on-dates       #{(t/short-date-str->date "26/07/2016")
                                                                     (t/short-date-str->date "04/07/2016")}
                                                   :logic-operator :single
                                                   :conditions     [[:equals "AMAZON AUST SERVICES      MELBOURNE"]]}]
                     })

(def q3-2017-rules (c/attach-period {:period/tax-year 2017
                                     :period/quarter  :q3} -q3-2017-rules))

(def q2-2017-rules (c/attach-period {:period/tax-year 2017
                                     :period/quarter  :q2} -q2-2017-rules))

(def q1-2017-rules (c/attach-period {:period/tax-year 2017
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
;; TODO
;; When coding the UI <or>s can be different {} rules, and <and> assumed. Thus the user doesn't need to
;; enter much. Just a logical mixture of :starts-with, :ends-with and :equals. For instance after :equals
;; nothing else to be allowed.
;; (Still good to have <or>s working - too much typing for entering data here otherwise)
;; Hmm - there is a place for <or>s - where really is the same rule - for instance with different wording
;; for same thing coming from bank statement.
;;
(def permanent-rules
  {[visa :personal/anz-visa]         [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "CITY EAST IGA"]
                                                        [:starts-with "DAN MURPHY'S"]
                                                        [:starts-with "STRATH CORNER BAKERY"]
                                                        [:starts-with "PAYMENT THANKYOU"]
                                                        [:equals "PAYMENT - THANKYOU"]
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
                                                        [:equals "PARADE SUPERMARKET PTY    NORWOOD"]
                                                        [:equals "SURREY CLUB HOTEL         REDFERN"]
                                                        [:equals "HAZELBROOK CELLARS        HAZELBROOK"]
                                                        [:equals "COLES EXPRESS 1546        ST HELENS PK"]
                                                        [:equals "CELLARBRATIONS AT APPIN   APPIN"]
                                                        [:equals "LIQUORLAND 3472           INGLEBURN"]]}]
   [visa :split/agl-gas]             [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:equals "AGL SALES P/L 0307        MELBOURNE"]
                                                        [:equals "AGL SALES P/L 0308        MELBOURNE"]]}]
   [visa :exp/office-expense]        [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "OFFICEWORKS"]
                                                        [:equals "POST   APPIN LPO          APPIN"]]}]
   [visa :exp/petrol]                [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "CALTEX"]
                                                        [:starts-with "BP"]
                                                        [:contains "PETROL"]
                                                        [:starts-with "X CONVENIENCE MT BARKE"]
                                                        [:equals "OTR KENT TOWN 7217        KENT TOWN"]
                                                        [:equals "FUEL-MART                 FAULCONBRIDGE"]]}]
   [visa :exp/motor-vehicle]         [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "PETER STEVENS MOTORC"]
                                                        [:starts-with "DPTI - EZYREG"]
                                                        [:equals "SYD CITY M CYC LN CV  LAN E COVE    NSW"]
                                                        [:equals "RMS ETOLL PH 131865       PARRAMATTA"]]}]
   [visa :exp/interest-expense]      [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "INTEREST CHARGED ON PURCHASES"]]}]
   [visa :exp/travel]                [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "TFNSW RAIL                GLENFIELD"]]}]
   [visa :exp/accounting-software]   [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "XERO AUSTRALIA PTY LTD"]]}]
   [visa :exp/advertising]           [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "OOH EDGE PTY LIMITED  NOR TH SYDNEY NSW"]]}]
   [visa :exp/meeting-entertainmant] [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "BREWRISTAS PTY. LTD.      GLEBE"]]}]
   [visa :exp/cloud-expense]         [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "GOOGLE*SVCSAPPS SEASOF"]
                                                        [:equals "GOOGLE*SVCSAPPSSTRANDZ-OR SINGAPORE"]
                                                        [:starts-with "GOOGLE*SVCSAPPS STRAND"]
                                                        [:starts-with "LINODE.COM"]
                                                        [:starts-with "FastMail Pty Ltd"]]}]
   [visa :exp/computer-expense]      [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "ALLNEEDS COMPUTERS"]
                                                        [:equals "ASMARINA E CONSULTIN      LANE COVE"]]}]
   [visa :exp/mobile-expense]        [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "TELSTRA                   MELBOURNE"]]}]
   [visa :exp/isp]                   [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "IINET LIMITED             PERTH"]]}]
   [visa :non-exp/private-health]    [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "HCF"]]}]
   [visa :exp/bank-fee]              [{:field          :out/desc
                                       :dominates      #{:personal/anz-visa}
                                       :logic-operator :or
                                       :conditions     [[:equals "INTEREST CHARGED ON CASH"]
                                                        [:equals "REWARD PROGRAM FEE"]
                                                        [:equals "ANNUAL FEE"]
                                                        [:equals "CASH ADVANCE FEE - INTERNET"]
                                                        [:equals "LATE PAYMENT FEE"]]}]
   [visa :exp/accomodation]          [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "AIRBNB                    AUSTRALIA"]]}]
   [visa :exp/donations]             [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "THE SOCIETY OF JESUS      CHIPPENDALE"]]}]
   [visa :exp/storage]               [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:equals "KENNARDS SELF STORAGE     CAMPBELLTOWN"]
                                                        [:equals "KENNARDS SELF STORAGE     LEUMEAH"]]}]
   [amp :personal/amp]               [{:field          :out/desc
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
                                                        [:equals "Internet banking scheduled external transfer 012030 198414945"]]}
                                      {:field          :out/desc
                                       :logic-operator :and
                                       :conditions     [[:starts-with "Internet banking bill payment"]
                                                        [:ends-with "4509499246191003"]]}]
   [coy :income/mining-sales]        [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "TRANSFER FROM MINES RESCUE PTY CS"]]}]
   [coy :income/poker-parse-sales]   [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "TRANSFER FROM R T WILSON"]]}]
   [coy :income/bank-interest]       [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "CREDIT INTEREST PAID"]]}]
   [coy :exp/office-rent]            [{:field          :out/desc
                                       :logic-operator :and
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                        [:ends-with "313 TRU"]]}]
   [coy :exp/bank-fee]               [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:equals "ACCOUNT SERVICING FEE"]
                                                        [:equals "HONOUR/OVERDRAWN FEE"]]}]
   [coy :exp/interest-expense]       [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "DEBIT INTEREST CHARGED"]]}]
   [coy :exp/asic-payment]           [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING BPAY ASIC"]]}]
   [coy :non-exp/ato-payment]        [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING BPAY TAX OFFICE PAYMENT"]
                                                        [:starts-with "PAYMENT TO ATO"]]}]
   [coy :exp/accounting-expense]     [{:field          :out/desc
                                       :logic-operator :and
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                        [:ends-with "ALEXANDER FAMILY TRU"]]}]
   [coy :capital/drawings]           [{:field          :out/desc
                                       :logic-operator :and
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                        [:ends-with "4509499246191003"]]}
                                      {:field          :out/desc
                                       :logic-operator :and
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                        [:ends-with "CHRISTOPHER MURP"]]}
                                      ;; Could be funds introduced, but what's the point?
                                      {:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:equals "TRANSFER FROM MURPHY  CHRISTOP REVERSE DRAWINGS"]
                                                        [:equals "TRANSFER FROM MURPHY  CHRISTOP"]]}
                                      {:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "ANZ ATM CASULA BP EXPRESS        CASULA       NS"]]}
                                      ]})

;; Bank amounts are as at 30/06/2016, so s/be able to run quarters 1, 2 and 3 and get
;; balances as at 31/03/2017, which are:
;; :bank/anz-coy    2138.16
;; :bank/anz-visa    250.74
;; :bank/amp         431.76
;; They were indeed these amounts!
(def general-ledger
  {:bank/anz-coy              96.15M
   :bank/anz-visa             -1024.48M
   :bank/amp                  3010.59M
   :personal/amp              0M
   :personal/anz-visa         0M
   :income/mining-sales       0M
   :income/poker-parse-sales  0M
   :income/bank-interest      0M
   :capital/drawings          0M
   :exp/office-expense        0M
   :exp/motor-vehicle         0M
   :exp/cloud-expense         0M
   :exp/niim-trip             0M
   :exp/accounting-software   0M
   :exp/mobile-expense        0M
   :exp/bank-fee              0M
   :exp/interest-expense      0M
   :exp/petrol                0M
   :exp/computer-expense      0M
   :exp/office-rent           0M
   :exp/travel                0M
   :exp/donations             0M
   :exp/isp                   0M
   :exp/storage               0M
   :exp/light-power-heating   0M
   :exp/accomodation          0M
   :exp/food                  0M
   :exp/advertising           0M
   :exp/meeting-entertainmant 0M
   :exp/asic-payment          0M
   :exp/freight-courier       0M
   :exp/accounting-expense    0M
   :non-exp/ato-payment       0M
   :non-exp/private-health    0M
   })

