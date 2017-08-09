Accounting
==========
Parses in all the bank statements to build up the company accounts, applying rules to turn statements into general ledger entries. Has become more general so will work with months as well as quarters. There is the concept of many bank accounts, but only one is for the main entity. No reason why it wouldn't work for many bank accounts being of the main entity being accounted for. Soon enough will be working for many entities and many bank accounts.

Ledgers
-------
Ledgers are used when what comes in from the bank statements is not enough. For example cash is collected from members, and it is written into a book who has paid for what. This writing part should become electronic entry. Then as the bank statement is parsed in the rules that are picked up then refer to ledgers. The ledgers are of course referring to accounts. So the ledgers effectively become part of the rules.

Here we are going to address concerns about what happens when mistakes are made in the ledgers and need to be corrected.

1. Whenever a change is made to a ledger a recalc date for that ledger becomes the date of this entry.
2. The change of this recalc date means that the rules associated with the ledger will need to be applied again for all bank statement lines after that date.
3. Thus we may end up with new trial balances for quarters or months (whatever the organisation uses). This is okay as using Datomic for storage - the old and incorrect but possibly relied upon trial balances will still be there if choose to time-travel. Notice two concepts of time here. We are correcting the past - but not altering the past. These trial balances are what we are going to be storing. No logical need to store the bank statements themselves. Will also need the ledgers and opening balances of asset and liability accounts.
4. As apply rules to ledgers the ledger recalc date will move forward.
5. The ledger is hoovered up until exactly the right amount is reached. Otherwise there will be an error. (For now - it will go to the UI later).
6. When the right amount is reached the rules completed by the ledger are applied.
7. Recalc date is inclusive in the past, so we start from the beginning of the next day. Well dates are moments at beginning of the day. So if we've done up to 31st then recalc date is 1st of next month. 
