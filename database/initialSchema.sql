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

CREATE TABLE Trades ( ID Varchar (50) Primary Key, AccountID INTEGER, Created TIMESTAMP, Updated TIMESTAMP, Security VARCHAR (15) ,  Side VARCHAR(10) check (Side in ('Buy','Sell')),  Quantity INTEGER check Quantity > 0 , State VARCHAR(20) check (State in ('New', 'Processing', 'Settled', 'Cancelled')), CorrelationID VARCHAR(50)) ;  

Alter Table Trades Add Foreign Key (AccountID) references Accounts(ID); 

-- MiFID II Art. 16(6) / RTS 27-28 best-execution record. Append-only: written once by
-- trade-service for every submission, accepted or rejected, and never updated or deleted.
-- Deliberately has no foreign key to Accounts, because a rejected order may name an account
-- that does not exist - that rejection still has to be reconstructable.
--
-- Note the absence of a DROP above, and IF NOT EXISTS below: this script re-runs on every
-- database start, and every other table here is rebuilt from seed data. This one accumulates.
-- A retained record that a restart erases is not a retained record. Note the limit of that
-- claim in the demo stack: the H2 files live inside the container, so records survive a
-- database restart but not a rebuild. Durable retention is an infrastructure follow-up.
CREATE TABLE IF NOT EXISTS OrderDecisionAudit (
  ID VARCHAR(50) PRIMARY KEY,
  CorrelationID VARCHAR(50) NOT NULL,
  OrderID VARCHAR(50),
  AccountID INTEGER,
  Security VARCHAR(50),
  Side VARCHAR(10),
  Quantity INTEGER,
  Price DECIMAL(19,4),
  PriceSource VARCHAR(40),
  Notional DECIMAL(23,4),
  Decision VARCHAR(10) NOT NULL check (Decision in ('ACCEPTED','REJECTED')),
  ReasonCode VARCHAR(40) NOT NULL,
  LimitID VARCHAR(40),
  LimitType VARCHAR(40),
  LimitValue DECIMAL(23,4),
  LimitEffectiveFrom TIMESTAMP(3) WITH TIME ZONE,
  SubmittedBy VARCHAR(50),
  -- WITH TIME ZONE so external readers (TRX-105, exports) see UTC rather than the
  -- session wall clock; Hibernate maps java.time.Instant to TIMESTAMP_UTC.
  DecisionTimestamp TIMESTAMP(3) WITH TIME ZONE NOT NULL,
  -- Full precision write time. One order can produce two records - an acceptance and then a
  -- failure to reach the trade feed - inside the same millisecond, and the pair is only
  -- readable if their order is unambiguous. This is the tie-break, not a regulatory field.
  RecordedAt TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS IDX_OrderDecisionAudit_Correlation ON OrderDecisionAudit (CorrelationID);

CREATE INDEX IF NOT EXISTS IDX_OrderDecisionAudit_Account_Time ON OrderDecisionAudit (AccountID, DecisionTimestamp);

CREATE SEQUENCE ACCOUNTS_SEQ start with 65000 INCREMENT BY 1;

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