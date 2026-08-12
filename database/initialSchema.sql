Drop Table RiskLimitHistory IF EXISTS;

Drop Table RiskLimits IF EXISTS;

Drop Sequence RISKLIMITHISTORY_SEQ IF EXISTS;

Drop Table Trades IF EXISTS;

Drop Table AccountUsers IF EXISTS; 

Drop Table Positions IF EXISTS; 

Drop Table Accounts IF EXISTS; 

Drop Sequence ACCOUNTS_SEQ IF EXISTS;

CREATE TABLE Accounts ( ID INTEGER PRIMARY KEY, DisplayName VARCHAR (50) ) ; 

CREATE TABLE AccountUsers ( AccountID INTEGER NOT NULL, Username VARCHAR(15) NOT NULL, PRIMARY KEY (AccountID,Username));  

ALTER TABLE AccountUsers ADD FOREIGN KEY (AccountID) References Accounts(ID); 

CREATE TABLE Positions ( AccountID INTEGER , Security VARCHAR(15) , Updated TIMESTAMP, Quantity INTEGER, Primary Key (AccountID, Security) );  

Alter Table Positions ADD FOREIGN KEY (AccountID) References Accounts(ID) ; 

CREATE TABLE Trades ( ID Varchar (50) Primary Key, AccountID INTEGER, Created TIMESTAMP, Updated TIMESTAMP, Security VARCHAR (15) ,  Side VARCHAR(10) check (Side in ('Buy','Sell')),  Quantity INTEGER check Quantity > 0 , State VARCHAR(20) check (State in ('New', 'Processing', 'Settled', 'Cancelled'))) ;  

Alter Table Trades Add Foreign Key (AccountID) references Accounts(ID); 

CREATE SEQUENCE ACCOUNTS_SEQ start with 65000 INCREMENT BY 1;

--- TRX-102: pre-trade risk limits. Set by the risk function, read by trade-service at
--- enforcement time. MiFID II RTS 6 Art. 15 requires the limit to be owned by a function
--- independent of the desk, so the limit lives here and never in trade-service.

CREATE TABLE RiskLimits ( AccountID INTEGER PRIMARY KEY, MaxOrderNotional DECIMAL(19,2) NOT NULL check MaxOrderNotional >= 0, Currency VARCHAR(3) NOT NULL, EffectiveFrom TIMESTAMP NOT NULL, SetBy VARCHAR(50) NOT NULL, Updated TIMESTAMP NOT NULL );

Alter Table RiskLimits Add Foreign Key (AccountID) references Accounts(ID);

--- Append-only trail of every limit value that has ever been in force. Each change appends
--- one row holding the newly effective value plus who changed it and why; the superseded
--- value survives as the preceding row. Rows are never updated or deleted.

CREATE TABLE RiskLimitHistory ( ID BIGINT PRIMARY KEY, AccountID INTEGER NOT NULL, MaxOrderNotional DECIMAL(19,2) NOT NULL, Currency VARCHAR(3) NOT NULL, EffectiveFrom TIMESTAMP NOT NULL, SetBy VARCHAR(50) NOT NULL, ChangeType VARCHAR(10) NOT NULL check (ChangeType in ('CREATE','AMEND')), ChangedBy VARCHAR(50) NOT NULL, ChangedAt TIMESTAMP NOT NULL, Reason VARCHAR(255) );

Alter Table RiskLimitHistory Add Foreign Key (AccountID) references Accounts(ID);

CREATE INDEX IDX_RISKLIMITHISTORY_ACCOUNT ON RiskLimitHistory (AccountID, ChangedAt);

CREATE SEQUENCE RISKLIMITHISTORY_SEQ start with 1000 INCREMENT BY 1;

--- SAMPLE DATA ---

INSERT into Accounts (ID, DisplayName) VALUES (22214, 'Test Account 20'); 
INSERT into Accounts (ID, DisplayName) VALUES (11413, 'Private Clients Fund TTXX'); 
INSERT into Accounts (ID, DisplayName) VALUES (42422, 'Algo Execution Partners'); 
INSERT into Accounts (ID, DisplayName) VALUES (52355, 'Big Corporate Fund'); 
INSERT into Accounts (ID, DisplayName) VALUES (62654, 'Hedge Fund TXY1'); 
INSERT into Accounts (ID, DisplayName) VALUES (10031, 'Internal Trading Book'); 
INSERT into Accounts (ID, DisplayName) VALUES (44044, 'Trading Account 1'); 

INSERT into AccountUsers (AccountID, Username) VALUES (22214, 'user01'); 
INSERT into AccountUsers (AccountID, Username) VALUES (22214, 'user03'); 
INSERT into AccountUsers (AccountID, Username) VALUES (22214, 'user09'); 
INSERT into AccountUsers (AccountID, Username) VALUES (22214, 'user05'); 
INSERT into AccountUsers (AccountID, Username) VALUES (22214, 'user07'); 

INSERT into AccountUsers (AccountID, Username) VALUES (62654, 'user09'); 
INSERT into AccountUsers (AccountID, Username) VALUES (62654, 'user05'); 
INSERT into AccountUsers (AccountID, Username) VALUES (62654, 'user07'); 
INSERT into AccountUsers (AccountID, Username) VALUES (62654, 'user01'); 

INSERT into AccountUsers (AccountID, Username) VALUES (10031, 'user01'); 
INSERT into AccountUsers (AccountID, Username) VALUES (10031, 'user03'); 
INSERT into AccountUsers (AccountID, Username) VALUES (10031, 'user09'); 

INSERT into AccountUsers (AccountID, Username) VALUES (44044, 'user09'); 
INSERT into AccountUsers (AccountID, Username) VALUES (44044, 'user05'); 
INSERT into AccountUsers (AccountID, Username) VALUES (44044, 'user07'); 
INSERT into AccountUsers (AccountID, Username) VALUES (44044, 'user04'); 
INSERT into AccountUsers (AccountID, Username) VALUES (44044, 'user01'); 
INSERT into AccountUsers (AccountID, Username) VALUES (44044, 'user06'); 
 

INSERT into Trades(ID, Created, Updated, Security, Side, Quantity, State, AccountID) VALUES('TRADE-22214-AABBCC', NOW(), NOW(), 'IBM', 'Sell', 100, 'Settled', 22214); 
INSERT into Trades(ID, Created, Updated, Security, Side, Quantity, State, AccountID) VALUES('TRADE-22214-DDEEFF', NOW(), NOW(), 'MS', 'Buy', 1000, 'Settled', 22214); 
INSERT into Trades(ID, Created, Updated, Security, Side, Quantity, State, AccountID) VALUES('TRADE-22214-GGHHII', NOW(), NOW(), 'C', 'Sell', 2000, 'Settled', 22214); 

INSERT into Positions (AccountID, Security, Updated, Quantity) VALUES(22214, 'MS',NOW(), 1000); 
INSERT into Positions (AccountID, Security, Updated, Quantity) VALUES(22214, 'IBM',NOW(), -100); 
INSERT into Positions (AccountID, Security, Updated, Quantity) VALUES(22214, 'C',NOW(), -2000); 


INSERT into Trades(ID, Created, Updated, Security, Side, Quantity, State, AccountID) VALUES('TRADE-52355-AABBCC', NOW(), NOW(), 'BAC', 'Sell', 2400, 'Settled', 52355); 
INSERT into Positions (AccountID, Security, Updated, Quantity) VALUES(52355, 'BAC',NOW(), -2400); 

--- TRX-102 seed limits. 11413 is deliberately tight: a 1,000 share order in a ~USD 100
--- name breaches it, so the enforcement path (TRX-101) can be demonstrated live.

INSERT into RiskLimits (AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, Updated) VALUES (11413, 50000.00, 'USD', NOW(), 'risk.control@traderx', NOW());
INSERT into RiskLimits (AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, Updated) VALUES (22214, 2500000.00, 'USD', NOW(), 'risk.control@traderx', NOW());
INSERT into RiskLimits (AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, Updated) VALUES (42422, 5000000.00, 'USD', NOW(), 'risk.control@traderx', NOW());
INSERT into RiskLimits (AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, Updated) VALUES (52355, 10000000.00, 'USD', NOW(), 'risk.control@traderx', NOW());
INSERT into RiskLimits (AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, Updated) VALUES (62654, 7500000.00, 'USD', NOW(), 'risk.control@traderx', NOW());

--- Accounts 10031 (Internal Trading Book) and 44044 (Trading Account 1) are left without a
--- limit on purpose, so the missing-limit behaviour is visible in the demo.

INSERT into RiskLimitHistory (ID, AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, ChangeType, ChangedBy, ChangedAt, Reason) VALUES (1, 11413, 50000.00, 'USD', NOW(), 'risk.control@traderx', 'CREATE', 'risk.control@traderx', NOW(), 'Initial limit set by risk control');
INSERT into RiskLimitHistory (ID, AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, ChangeType, ChangedBy, ChangedAt, Reason) VALUES (2, 22214, 2500000.00, 'USD', NOW(), 'risk.control@traderx', 'CREATE', 'risk.control@traderx', NOW(), 'Initial limit set by risk control');
INSERT into RiskLimitHistory (ID, AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, ChangeType, ChangedBy, ChangedAt, Reason) VALUES (3, 42422, 5000000.00, 'USD', NOW(), 'risk.control@traderx', 'CREATE', 'risk.control@traderx', NOW(), 'Initial limit set by risk control');
INSERT into RiskLimitHistory (ID, AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, ChangeType, ChangedBy, ChangedAt, Reason) VALUES (4, 52355, 10000000.00, 'USD', NOW(), 'risk.control@traderx', 'CREATE', 'risk.control@traderx', NOW(), 'Initial limit set by risk control');
INSERT into RiskLimitHistory (ID, AccountID, MaxOrderNotional, Currency, EffectiveFrom, SetBy, ChangeType, ChangedBy, ChangedAt, Reason) VALUES (5, 62654, 7500000.00, 'USD', NOW(), 'risk.control@traderx', 'CREATE', 'risk.control@traderx', NOW(), 'Initial limit set by risk control');
