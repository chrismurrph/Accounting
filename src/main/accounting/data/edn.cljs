(ns accounting.data.edn)

;;
;; Just scratch/experiments
;; What the .edn file looks like
;; Made as cljs so hidden from Clojure
;;
(def on-file
  [{:logic-operator :or, :conditions [[:out/desc :starts-with "ANZ INTERNET BANKING BPAY TAX OFFICE PAYMENT"] [:out/desc :starts-with "PAYMENT TO ATO"]], :rule/source-bank :bank/anz-coy, :rule/target-account :non-exp/ato-payment, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :and, :conditions [[:out/desc :starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"] [:out/desc :ends-with "4509499246191003"]], :rule/source-bank :bank/anz-coy, :rule/target-account :liab/drawings, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :and, :conditions [[:out/desc :starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"] [:out/desc :ends-with "CHRISTOPHER MURP"]], :rule/source-bank :bank/anz-coy, :rule/target-account :liab/drawings, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "TRANSFER FROM MURPHY  CHRISTOP REVERSE DRAWINGS"] [:out/desc :equals "TRANSFER FROM MURPHY  CHRISTOP"]], :rule/source-bank :bank/anz-coy, :rule/target-account :liab/drawings, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "ANZ ATM CASULA BP EXPRESS        CASULA       NS"]], :rule/source-bank :bank/anz-coy, :rule/target-account :liab/drawings, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "HCF"]], :rule/source-bank :bank/anz-visa, :rule/target-account :non-exp/private-health, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "TELSTRA                   MELBOURNE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/mobile-expense, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "THE SOCIETY OF JESUS      CHIPPENDALE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/donations, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "Internet banking external transfer 062202 10481871 - car rental"] [:out/desc :equals "ROAM TOLLING P/L          MELBOURNE"]], :period #:period{:tax-year 2017, :quarter :q1}, :rule/source-bank :bank/amp, :rule/target-account :exp/national-travel, :between-dates-inclusive nil, :on-dates nil}
   {:dominates #{:personal/anz-visa}, :logic-operator :or, :conditions [[:out/desc :equals "INTEREST CHARGED ON CASH"] [:out/desc :equals "REWARD PROGRAM FEE"] [:out/desc :equals "ANNUAL FEE"] [:out/desc :equals "CASH ADVANCE FEE - INTERNET"] [:out/desc :equals "LATE PAYMENT FEE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/bank-fee, :between-dates-inclusive nil, :on-dates nil}
   {:between-dates-inclusive [#inst "2017-01-25T00:00:00.000-00:00" #inst "2017-02-09T00:00:00.000-00:00"], :logic-operator :or, :conditions [[:out/desc :starts-with "SKYBUS COACH SERVICE"] [:out/desc :equals "RE & TK WILSDON PTY       KEITH"] [:out/desc :starts-with "TIGER AIRWAYS AUSTRALIA"] [:out/desc :starts-with "AUSDRAGON PTY LTD"]], :period #:period{:tax-year 2017, :quarter :q3}, :rule/source-bank :bank/anz-visa, :rule/target-account :exp/niim-trip, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "ACCOUNT SERVICING FEE"] [:out/desc :equals "HONOUR/OVERDRAWN FEE"]], :rule/source-bank :bank/anz-coy, :rule/target-account :exp/bank-fee, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "POST   HAZELBROOK LP      HAZELBROOK"]], :period #:period{:tax-year 2017, :quarter :q1}, :rule/source-bank :bank/anz-visa, :rule/target-account :exp/freight-courier, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :and, :conditions [[:out/desc :starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"] [:out/desc :ends-with "ALEXANDER FAMILY TRU"]], :rule/source-bank :bank/anz-coy, :rule/target-account :exp/accounting-expense, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "OOH EDGE PTY LIMITED  NOR TH SYDNEY NSW"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/advertising, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [#_[:out/desc :starts-with "OFFICEWORKS"] [:out/desc :equals "POST   APPIN LPO          APPIN"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/office-expense, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "TARGET 5009               ADELAIDE"] [:out/desc :starts-with "DRAKE SUPERMARKETS"] [:out/desc :starts-with "Z & Y BEYOND INTL PL"]], :period #:period{:tax-year 2017, :quarter :q3}, :rule/source-bank :bank/anz-visa, :rule/target-account :exp/office-expense, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "DEBIT INTEREST CHARGED"]], :rule/source-bank :bank/anz-coy, :rule/target-account :exp/bank-interest, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "AGL SALES P/L 0307        MELBOURNE"] [:out/desc :equals "AGL SALES P/L 0308        MELBOURNE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :split/agl-gas, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :starts-with "PETER STEVENS MOTORC"] [:out/desc :starts-with "DPTI - EZYREG"] [:out/desc :equals "SYD CITY M CYC LN CV  LAN E COVE    NSW"] [:out/desc :equals "RMS ETOLL PH 131865       PARRAMATTA"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/motor-vehicle, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "Internet banking external transfer 062202 10481871 - renting caravan"]], :period #:period{:tax-year 2017, :quarter :q1}, :rule/source-bank :bank/amp, :rule/target-account :exp/rent, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "IINET LIMITED             PERTH"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/isp, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "BREWRISTAS PTY. LTD.      GLEBE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/meeting-entertainment, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :ends-with "drawings"] [:out/desc :equals "Direct Entry Credit Item Ref: drawings Seaweed Software"]
                                      [:out/desc :starts-with "ATM Withdrawal - "] [:out/desc :starts-with "Purchase - Ideal Shoe Sto"]
                                      [:out/desc :equals "Outward Dishonour Fee - Electronic"]
                                      [:out/desc :starts-with "Dishonour of: Direct Entry Debit Item Ref:"] [:out/desc :equals "ATM Direct Charge"]
                                      [:out/desc :equals "Reversal of ATM Direct Charge"] [:out/desc :starts-with "Reversal of ATM Withdrawal"]
                                      [:out/desc :equals "Purchase - IGA HUTT ST              ADELAIDE     AU"]
                                      [:out/desc :equals "Purchase - LUNCH ON ANGAS      ADELAIDE          AU"]
                                      [:out/desc :equals "Internet banking scheduled external transfer 012030 198414945"]],
    :rule/source-bank :bank/amp, :rule/target-account :personal/amp, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :and, :conditions [[:out/desc :starts-with "Internet banking bill payment"] [:out/desc :ends-with "4509499246191003"]], :rule/source-bank :bank/amp, :rule/target-account :personal/amp, :between-dates-inclusive nil, :on-dates nil}
   {:on-dates #{#inst "2016-12-12T00:00:00.000-00:00" #inst "2016-12-08T00:00:00.000-00:00"}, :logic-operator :and, :conditions [[:out/desc :starts-with "Direct Entry Debit Item Ref: "] [:out/desc :ends-with "PAYPAL AUSTRALIA"]], :period #:period{:tax-year 2017, :quarter :q2}, :rule/source-bank :bank/amp, :rule/target-account :personal/amp, :between-dates-inclusive nil}
   {:on-dates #{#inst "2017-02-20T00:00:00.000-00:00"}, :logic-operator :and, :conditions [[:out/desc :starts-with "Direct Entry Debit Item Ref: "] [:out/desc :ends-with "PAYPAL AUSTRALIA"]], :period #:period{:tax-year 2017, :quarter :q3}, :rule/source-bank :bank/amp, :rule/target-account :personal/amp, :between-dates-inclusive nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "M J Brown & S E Thomps    GOOLGOWI"] [:out/desc :equals "BURONGA IGA X-PRESS       BURONGA"] [:out/desc :equals "THE GOLDEN TRIANGLE       GRENFELL"] [:out/desc :equals "SUBWAY KELSO              KELSO"] [:out/desc :equals "THE FRUIT HOUSE           HAZELBROOK"] [:out/desc :equals "Purchase/Cash out - H.B. FOODS PTY LIMIT HAZELBROOK NSW"] [:out/desc :equals "Purchase/Cash out - COLES EXPRESS 1724       FAULCONBRIDGEAU"] [:out/desc :equals "HAZELBROOK BUTCHERY       HAZELBROOK"]], :period #:period{:tax-year 2017, :quarter :q1}, :rule/source-bank :bank/anz-visa, :rule/target-account :exp/food, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :starts-with "CITY EAST IGA"] [:out/desc :starts-with "DAN MURPHY'S"] [:out/desc :starts-with "STRATH CORNER BAKERY"] [:out/desc :starts-with "PAYMENT THANKYOU"] [:out/desc :equals "PAYMENT - THANKYOU"] [:out/desc :starts-with "WOOLWORTHS"] [:out/desc :starts-with "FEATHERS HOTEL"] [:out/desc :starts-with "BWS LIQUOR"] [:out/desc :starts-with "UNLEY SWIMMING CNTR"] [:out/desc :starts-with "NORWOOD SWIM SCHOOL"] [:out/desc :starts-with "SQ *OUT OF ZULULAND"] [:out/desc :starts-with "PANCAKE HOUSE"] [:out/desc :starts-with "FREWVILLE FOODLAND"] [:out/desc :starts-with "GREAT DREAM PTY LTD       HAYBOROUGH"] [:out/desc :starts-with "JETTY SURF"] [:out/desc :starts-with "COCKLES ON NORTH"] [:out/desc :starts-with "CORIOLE VINEYARDS"] [:out/desc :equals "HAZELS POOLSIDE CAFE      HAZELWOOD PAR"] [:out/desc :equals "J TSIMBINOS & PARTNERS    ADELAIDE"] [:out/desc :equals "NRMA LIMITED - 2          NORTH STRATHF"] [:out/desc :equals "COLES EXPRESS 1943        ROSE PARK"] [:out/desc :equals "HOTEL ROYAL               TORRENSVILLE"] [:out/desc :equals "NATIONAL PHARMACIES       VICTOR HARBOR"] [:out/desc :equals "RECOVERIES CORP           MELBOURNE"] [:out/desc :starts-with "CASH ADVANCE"] [:out/desc :equals "LUNCH ON ANGAS            ADELAIDE"] [:out/desc :equals "IGA HUTT ST               ADELAIDE"] [:out/desc :equals "THE YIROS HUTT BY YA      ADELAIDE"] [:out/desc :equals "GENERAL HAVELOCK PTY L    ADELAIDE"] [:out/desc :equals "PARADE SUPERMARKET PTY    NORWOOD"] [:out/desc :equals "SURREY CLUB HOTEL         REDFERN"] [:out/desc :equals "HAZELBROOK CELLARS        HAZELBROOK"] [:out/desc :equals "COLES EXPRESS 1546        ST HELENS PK"] [:out/desc :equals "CELLARBRATIONS AT APPIN   APPIN"] [:out/desc :equals "LIQUORLAND 3472           INGLEBURN"]], :rule/source-bank :bank/anz-visa, :rule/target-account :personal/anz-visa, :between-dates-inclusive nil, :on-dates nil}
   {:on-dates #{#inst "2016-07-04T00:00:00.000-00:00" #inst "2016-07-26T00:00:00.000-00:00"}, :logic-operator :single,
    :conditions [[:out/desc :equals "AMAZON AUST SERVICES      MELBOURNE"]],
    :period #:period{:tax-year 2017, :quarter :q1},
    :rule/source-bank :bank/anz-visa, :rule/target-account :personal/anz-visa, :between-dates-inclusive nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "QANTAS AIRWAYS"]], :period #:period{:tax-year 2017, :quarter :q3}, :rule/source-bank :bank/anz-visa, :rule/target-account :personal/anz-visa, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "ANZ INTERNET BANKING BPAY ASIC"]], :rule/source-bank :bank/anz-coy, :rule/target-account :exp/asic-payment, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "INTEREST CHARGED ON PURCHASES"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/bank-interest, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "CREDIT INTEREST PAID"]], :rule/source-bank :bank/anz-coy, :rule/target-account :income/bank-interest, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :starts-with "ALLNEEDS COMPUTERS"] [:out/desc :equals "ASMARINA E CONSULTIN      LANE COVE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/computer-expense, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "KENNARDS SELF STORAGE     CAMPBELLTOWN"] [:out/desc :equals "KENNARDS SELF STORAGE     LEUMEAH"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/storage, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "XERO AUSTRALIA PTY LTD"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/accounting-software, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :and, :conditions [[:out/desc :starts-with "ANZ INTERNET BANKING FUNDS TFER TRANSFER"] [:out/desc :ends-with "313 TRU"]], :rule/source-bank :bank/anz-coy, :rule/target-account :exp/rent, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "TFNSW RAIL                GLENFIELD"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/national-travel, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :equals "MILDURA INLANDER          MILDURA"] [:out/desc :equals "TRAVELLERS REST MTL   WEE THALLE    NSW"] [:out/desc :equals "UNITED MEDLOW BATH        MEDLOW BATH"]], :period #:period{:tax-year 2017, :quarter :q1}, :rule/source-bank :bank/anz-visa, :rule/target-account :exp/national-travel, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :equals "AIRBNB                    AUSTRALIA"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/rent, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "TRANSFER FROM MINES RESCUE PTY CS"]], :rule/source-bank :bank/anz-coy, :rule/target-account :income/mining-sales, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :starts-with "CALTEX"] [:out/desc :starts-with "BP"] [:out/desc :contains "PETROL"] [:out/desc :starts-with "X CONVENIENCE MT BARKE"] [:out/desc :equals "OTR KENT TOWN 7217        KENT TOWN"] [:out/desc :equals "FUEL-MART                 FAULCONBRIDGE"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/petrol, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :single, :conditions [[:out/desc :starts-with "TRANSFER FROM R T WILSON"]], :rule/source-bank :bank/anz-coy, :rule/target-account :income/poker-parse-sales, :between-dates-inclusive nil, :on-dates nil}
   {:logic-operator :or, :conditions [[:out/desc :starts-with "GOOGLE*SVCSAPPS SEASOF"] [:out/desc :equals "GOOGLE*SVCSAPPSSTRANDZ-OR SINGAPORE"] [:out/desc :starts-with "GOOGLE*SVCSAPPS STRAND"] [:out/desc :starts-with "LINODE.COM"] [:out/desc :starts-with "FastMail Pty Ltd"]], :rule/source-bank :bank/anz-visa, :rule/target-account :exp/cloud-expense, :between-dates-inclusive nil, :on-dates nil}])
