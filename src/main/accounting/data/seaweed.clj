(ns accounting.data.seaweed
  (:require [accounting.util :as u]
            [accounting.time :as t]
            [accounting.common :as c]
            [accounting.data.common :as dc]))

(def amp :bank/amp)
(def coy :bank/anz-coy)
(def visa :bank/anz-visa)

(def splits {:split/agl-gas {:exp/light-power-heating 0.2M :personal/anz-visa 0.8M}})

;;
;; For every transaction date that comes thru that is :office-expense we will require a
;; description TODO - there are more descriptions, need to take all from Xero
;; All the invented accounts will need to have description. So here :niim-trip
;;
;; Have introduce more specificity to these rules, that will be used if not nil:
;;   :on-date
;;   :time-slot
;;   :amount
;;   - already has :period
;; Here the PayPal can be :on-date, and the niim-trip between dates
;; Will test the PayPal by having on wrong date - works!
;; TODO
;; S/also have whether multiple matches are allowed. For permanent ones default will be true.
;; For temporary ones like here the user might have to specify
;;
(def -q3-2017-rules {
                     ;; Upon closer investigation, this one was personal spending
                     [amp :personal/amp]        [{:rule/rule-num  1
                                                  :field          :out/desc
                                                  :on-dates       #{(t/long-date-str->date "20 Feb 2017")}
                                                  :logic-operator :and
                                                  :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                                   [:ends-with "PAYPAL AUSTRALIA"]]}]
                     [visa :personal/anz-visa]  [{:rule/rule-num  2
                                                  :field          :out/desc
                                                  :logic-operator :single
                                                  :conditions     [[:starts-with "QANTAS AIRWAYS"]]}
                                                 ]
                     [visa :exp/niim-trip]      [{:rule/rule-num  3
                                                  :field                   :out/desc
                                                  :time-slot (t/inclusive-range "25 Jan 2017" "08 Feb 2017")
                                                  :logic-operator          :or
                                                  :conditions              [[:starts-with "SKYBUS COACH SERVICE"]
                                                                            [:equals "RE & TK WILSDON PTY       KEITH"]
                                                                            [:starts-with "TIGER AIRWAYS AUSTRALIA"]
                                                                            [:starts-with "AUSDRAGON PTY LTD"]]}]
                     [visa :exp/office-expense] [{:rule/rule-num  4
                                                  :field          :out/desc
                                                  :logic-operator :or
                                                  :conditions     [[:equals "TARGET 5009               ADELAIDE"]
                                                                   [:starts-with "DRAKE SUPERMARKETS"]
                                                                   [:starts-with "Z & Y BEYOND INTL PL"]]}]})

(def -q2-2017-rules {
                     [amp :personal/amp] [{:rule/rule-num  5
                                           :field          :out/desc
                                           :on-dates       #{(t/long-date-str->date "12 Dec 2016")
                                                             (t/long-date-str->date "8 Dec 2016")}
                                           :logic-operator :and
                                           :conditions     [[:starts-with "Direct Entry Debit Item Ref: "]
                                                            [:ends-with "PAYPAL AUSTRALIA"]]}]})

(def -q1-2017-rules {
                     [amp :exp/national-travel]  [{:rule/rule-num  6
                                                   :field          :out/desc
                                                   :logic-operator :or
                                                   :conditions     [[:equals "Internet banking external transfer 062202 10481871 - car rental"]
                                                                    [:equals "ROAM TOLLING P/L          MELBOURNE"]]}]
                     [amp :exp/rent]             [{:rule/rule-num  7
                                                   :field          :out/desc
                                                   :logic-operator :single
                                                   :conditions     [[:equals "Internet banking external transfer 062202 10481871 - renting caravan"]]}]
                     [visa :exp/national-travel] [{:rule/rule-num  8
                                                   :field          :out/desc
                                                   :logic-operator :or
                                                   :conditions     [[:equals "MILDURA INLANDER          MILDURA"]
                                                                    [:equals "TRAVELLERS REST MTL   WEE THALLE    NSW"]
                                                                    ;; Actually petrol - putting in here b/c I know it won't be removed
                                                                    ;; by my accountant
                                                                    [:equals "UNITED MEDLOW BATH        MEDLOW BATH"]]}]
                     [visa :exp/food]            [{:rule/rule-num  9
                                                   :field          :out/desc
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
                     [visa :exp/freight-courier] [{:rule/rule-num  10
                                                   :field          :out/desc
                                                   :logic-operator :single
                                                   :conditions     [[:equals "POST   HAZELBROOK LP      HAZELBROOK"]]}]
                     [visa :personal/anz-visa]   [{:rule/rule-num  11
                                                   :field          :out/desc
                                                   :on-dates       #{(t/short-date-str->date "26/07/2016")
                                                                     (t/short-date-str->date "04/07/2016")}
                                                   :logic-operator :single
                                                   :conditions     [[:equals "AMAZON AUST SERVICES      MELBOURNE"]]}]
                     })

(def q3-2017-rules (c/attach-period {:actual-period/tax-year 2017
                                     :actual-period/quarter  :q3} -q3-2017-rules))

(def q2-2017-rules (c/attach-period {:actual-period/tax-year 2017
                                     :actual-period/quarter  :q2} -q2-2017-rules))

(def q1-2017-rules (c/attach-period {:actual-period/tax-year 2017
                                     :actual-period/quarter  :q1} -q1-2017-rules))

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
;; So good definition is decrease in personal wealth over that quarter, in the bank account
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
   [visa :exp/bank-interest]         [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "INTEREST CHARGED ON PURCHASES"]]}]
   [visa :exp/national-travel]       [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "TFNSW RAIL                GLENFIELD"]]}]
   [visa :exp/accounting-software]   [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:starts-with "XERO AUSTRALIA PTY LTD"]]}]
   [visa :exp/advertising]           [{:field          :out/desc
                                       :logic-operator :single
                                       :conditions     [[:equals "OOH EDGE PTY LIMITED  NOR TH SYDNEY NSW"]]}]
   [visa :exp/meeting-entertainment] [{:field          :out/desc
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
   [visa :exp/rent]                  [{:field          :out/desc
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
   [coy :exp/rent]                   [{:field          :out/desc
                                       :logic-operator :and
                                       :conditions     [[:starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"]
                                                        [:ends-with "313 TRU"]]}]
   [coy :exp/bank-fee]               [{:field          :out/desc
                                       :logic-operator :or
                                       :conditions     [[:equals "ACCOUNT SERVICING FEE"]
                                                        [:equals "HONOUR/OVERDRAWN FEE"]]}]
   [coy :exp/bank-interest]          [{:field          :out/desc
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
   [coy :liab/drawings]              [{:field          :out/desc
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

(def quarter->rules
  {:q1 q1-2017-rules
   :q2 q2-2017-rules
   :q3 q3-2017-rules})

(def all-rules
  (let [initial-rules (merge-with (comp vec concat)
                                  permanent-rules
                                  (apply concat (map quarter->rules [:q1 :q2 :q3])))]
    (->> initial-rules
         dc/canonicalise-rules
         (mapv t/civilize-joda))))

(def rules-keys (->> all-rules
                     (map keys)
                     (apply concat)
                     distinct))

(defn example-rule-field [field]
  (->> all-rules
       (apply concat)
       (filter (fn [[k v]] (and (some? v) (= k field))))
       first
       val))

;;
;; Bank balances at beginning of periods are factual, can never be different once the time has passed.
;; Hence we will keeping them in the database. Here we manually calculated them from ye-2016 by
;; generating trial balances. But they will be the same as the real thing if you look.
;; Theoretically assets, liabs and equity are just as solid. Later we will include them.
;; One difference is that bank balances cannot be affected by rules.
;; Here 2017 :q1 has the balances at the start of :q1
;; , equivalent to balances at the end of 2016 :q4
;;
(def start-of-period-bank-balances
  {{:period/tax-year 2017
    :period/quarter  :q1} {:bank/amp 3010.59M :bank/anz-coy 96.15M :bank/anz-visa -1024.48M}
   {:period/tax-year 2017
    :period/quarter  :q2} {:bank/amp 51.60M :bank/anz-coy -600.49M :bank/anz-visa -1556.15M}
   {:period/tax-year 2017
    :period/quarter  :q3} {:bank/amp 103.21M :bank/anz-coy 345.26M :bank/anz-visa -1144.08M}
   {:period/tax-year 2017
    :period/quarter  :q4} {:bank/amp 431.76M :bank/anz-coy 2138.16M :bank/anz-visa 250.74M}})

;; Bank amounts are as at 30/06/2016, so s/be able to run quarters 1, 2 and 3 and get
;; balances as at 31/03/2017, which are:
;; :bank/anz-coy    2138.16
;; :bank/anz-visa    250.74
;; :bank/amp         431.76
;; They were indeed these amounts!
(def ye-2016 {:gl {:bank/anz-coy              96.15M
                   :bank/anz-visa             -1024.48M
                   :bank/amp                  3010.59M
                   :personal/amp              0M
                   :personal/anz-visa         0M
                   :income/mining-sales       0M
                   :income/poker-parse-sales  0M
                   :income/bank-interest      0M
                   :liab/drawings             0M
                   :exp/office-expense        0M
                   :exp/motor-vehicle         0M
                   :exp/cloud-expense         0M
                   :exp/niim-trip             0M
                   :exp/accounting-software   0M
                   :exp/mobile-expense        0M
                   :exp/bank-fee              0M
                   :exp/bank-interest         0M
                   :exp/petrol                0M
                   :exp/computer-expense      0M
                   :exp/national-travel       0M
                   :exp/donations             0M
                   :exp/isp                   0M
                   :exp/storage               0M
                   :exp/light-power-heating   0M
                   :exp/rent                  0M
                   :exp/food                  0M
                   :exp/advertising           0M
                   :exp/meeting-entertainment 0M
                   :exp/asic-payment          0M
                   :exp/freight-courier       0M
                   :exp/accounting-expense    0M
                   :non-exp/ato-payment       0M
                   :non-exp/private-health    0M
                   }})

;;
;; Only goes back one period
;;
(defn starting-gl [make-period-fn year quarter]
  (let [req-period (make-period-fn year quarter)
        bank-balances (get start-of-period-bank-balances req-period)]
    (assert bank-balances (str "No bank balances at " req-period ", prior for " year ", " quarter))
    (u/deep-merge ye-2016 {:gl bank-balances})))

(def xero-account-numbers
  {:income/bank-interest                270
   :income/mining-sales                 200
   :exp/bank-fee                        404
   :exp/books-periodicals               1050
   :exp/computer-expense                1900
   :exp/accounting-expense              412
   :exp/formation-costs                 304
   :exp/freight-courier                 425
   :exp/income-tax-expense              505
   :exp/light-power-heating             445
   :exp/motor-vehicle                   449
   :exp/rent                            469
   :exp/subscriptions                   485
   ;; Will be superseded by a few: cloud-expense, accounting-software, mobile-expense, isp
   :exp/telephone-internet              489
   :exp/national-travel                 493
   :asset/accounts-receivable           610
   :bank/amp                            1400
   :asset/banking-correction            1301
   :bank/anz-visa                       1300
   :asset/cash-on-hand                  640
   :bank/anz-coy                        1200
   :asset/computer-equipment            720
   :asset/director-loans                693
   :asset/low-value-pool                770
   :asset/office-equipment              710
   :asset/office-equipment-accum-deprec 711
   :asset/petty-cash-adjustment         641
   :liab/gst                            820
   :liab/income-tax-payable             830
   :liab/integrated-client-account      894
   :liab/drawings                       880
   :liab/funds-introduced               881
   :liab/trade-creditors                883
   :equity/share-capital                970
   :equity/retained-earnings            960
   })

;Revenue
;Interest Income (270)				$0.66
;Sales (200)				$24,600.00
;
;Expenses
;Bank Fees (404)			$129.14
;Books and Periodicals (1050)			$200.34
;Computer Expenses (1900)			$968.53
;Consulting & Accounting (412)			$1,235.40
;Formation Costs (304)			$318.00
;Freight & Courier (425)			$127.50
;Income Tax Expense (505)			$9,308.00
;Light, Power, Heating (445)			$433.78
;Motor Vehicle Expenses (449)			$2,026.43
;Rent (469)			$1,999.35
;Subscriptions (485)			$13.64
;Telephone & Internet (489)			$1,174.81
;Travel - National (493)			$228.64
;
;Assets
;Accounts Receivable (610)			$4,950.00
;AMP Current (1400)			$607.97
;ANZ card, AMP Current, Cash Mgt accounts correction (1301)				$2,625.31
;ANZ Credit Card (1300)				$11,152.14
;Cash on Hand (640)			$2.00
;Company Cash Management (1200)			$7,650.80
;Computer Equipment (720)			$168.13
;Director Loans (693)			$120,938.39
;Low Value and STS Pool Assets (770)			$929.00
;Office Equipment (710)			$4,101.95
;Less Accumulated Depreciation on Office Equipment (711)				$2,958.00
;Petty Cash adjustment (641)			$314.99
;PettyCash				$374.99
;
;Liabilities
;GST (820)				$7,568.08
;Income Tax Payable (830)				$7,538.00
;Integrated Client Account (894)				$24,089.51
;Owner A Drawings (880)			$57,931.00
;Owner A Funds Introduced (881)				$1,470.00
;Trade Creditors (883)				$414.00
;Trash (TSH)				$32,536.14
;
;Equity
;Owner A Share Capital (970)				$2.00
;Retained Earnings (960)				$100,428.96

;;
;; tb asset and liability amounts s/be always be 'as at'.
;; Whereas Y and expenses are flows over the whole year here
;; The asset amounts (like bank balances) are actually wrong in Xero
;; We will override them with the real amounts where we have better data (like from actual bank statements)
;; DEBIT  +ive
;; CREDIT -ive
;;
(def -xero-tb-ye-2016
  {
   :income/mining-sales                 -24600M
   :income/bank-interest                -0.66M

   :exp/bank-fee                        129.14M
   :exp/books-periodicals               200.34M
   :exp/computer-expense                968.53M
   :exp/accounting-expense              1235.40M
   :exp/formation-costs                 318.00M
   :exp/freight-courier                 127.50M
   :exp/income-tax-expense              9308.00M
   :exp/light-power-heating             433.78M
   :exp/motor-vehicle                   2026.43M
   :exp/rent                            1999.35M
   :exp/subscriptions                   13.64M
   :exp/telephone-internet              1174.81M
   :exp/national-travel                 228.64M

   :equity/share-capital                -2.00M
   :equity/retained-earnings            -100428.96M

   :liab/gst                            -7568.08M
   :liab/income-tax-payable             -7538.00M
   :liab/integrated-client-account      -24089.51M
   :liab/drawings                       57931.00M
   :liab/funds-introduced               -1470.00M
   :liab/trade-creditors                -414.00M
   :liab/trash                          -32536.14M

   :asset/accounts-receivable           4950.00M
   :bank/amp                            607.97M
   :asset/banking-correction            -2625.31M
   :bank/anz-visa                       -11152.14M
   :asset/cash-on-hand                  2.00M
   :bank/anz-coy                        7650.80M
   :asset/computer-equipment            168.13M
   :asset/director-loans                120938.39M
   :asset/low-value-pool                929.00M
   :asset/office-equipment              4101.95M
   :asset/office-equipment-accum-deprec -2958.00M
   :asset/petty-cash-adjustment         314.99M
   :asset/petty-cash                    -374.99M
   })

(def notes-explained-tb-ye-2016
  {
   :income/div-7a-interest    -6583.00M
   :income/ato-interest       -541.00M
   :exp/plant-equip-deprec    168.00M
   :exp/low-value-pool-deprec 105.00M
   :exp/ato-interest          1453.00M
   })

(def unexplained-tb-ye-2016
  {
   :exp/directors-bonus 22000.00M
   :exp/filing-fees     243.00M
   :exp/fines-penalties 75.00M
   })

;;
;; Got by manually going into Xero.
;; Is it a problem with James' software that Telephone must be on its own, or
;; is that a requirement of ATO the Xero doesn't know about?
;; In future keep :exp/mobile-expense so no need to dig it out
;;
(def telstra-2016-recharges (bigdec (+ (* 10 27.27) 18.18 (* 2 36.36))))

(def xero-tb-ye-2016
  (let [{:keys [exp/computer-expense exp/telephone-internet]} -xero-tb-ye-2016
        internet (- telephone-internet telstra-2016-recharges)]
    (-> -xero-tb-ye-2016
        (assoc :exp/mobile-expense telstra-2016-recharges)
        (assoc :exp/computer-expenses (+ internet computer-expense))
        (merge notes-explained-tb-ye-2016)
        (merge unexplained-tb-ye-2016))))

(def bank-balances-ye-2016
  {:bank/anz-coy  96.15M
   :bank/anz-visa -1024.48M
   :bank/amp      3010.59M
   })
