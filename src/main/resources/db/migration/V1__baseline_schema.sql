-- ===========================================================================
-- V1  baseline schema
--
-- The 87 tables of Cloud POS, ported from the live democloud database
-- (MariaDB 10.4) to MySQL 8, with four corrections applied mechanically:
--
--   1. utf8mb4 throughout. The source mixed utf8mb3 (55 tables) with utf8mb4
--      (32), which makes a JOIN between two of them fail on collation and
--      silently truncates anything outside the BMP.
--
--   2. Money is DECIMAL, never FLOAT. 27 money columns across 9 tables were
--      float(10,2) while their neighbours in the same row were decimal(12,2).
--      Float cannot hold 0.01 exactly, so SUM() over a day's sales drifts and
--      a bill never quite reconciles against its own lines.
--
--   3. retrun_invoice_header is spelled return_invoice_header. Nothing syncs
--      to the cloud, so this is the moment to fix it or never.
--
--   4. Integer display widths dropped (deprecated in MySQL 8). tinyint(1) is
--      kept, because there it means boolean.
--
--   5. A `date` column may not default to current_timestamp() in MySQL 8.
--      Six of them did; they now default to (CURRENT_DATE). MariaDB accepted
--      the original and silently threw the time away.
--
-- Foreign keys, unique constraints and indexes are NOT here. They are in V2,
-- so this file stays a faithful port and the corrections stay reviewable.
-- ===========================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `adjustheader`;
CREATE TABLE `adjustheader` (
  `AHID` int NOT NULL AUTO_INCREMENT,
  `AdjustNo` varchar(12) DEFAULT NULL,
  `EffectiveDate` date DEFAULT NULL,
  `AdjustCount` int DEFAULT NULL,
  `AdjustAmount` decimal(12,2) DEFAULT NULL,
  `AdjustStat` tinyint DEFAULT NULL,
  `AdjustmentType_ITID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `user_USID` int NOT NULL,
  PRIMARY KEY (`AHID`),
  KEY `fk_AdjustHeader_AdjustmentType1_idx` (`AdjustmentType_ITID`),
  KEY `fk_AdjustHeader_shop1_idx` (`shop_SHID`),
  KEY `fk_AdjustHeader_user1_idx` (`user_USID`),
  CONSTRAINT `fk_AdjustHeader_AdjustmentType1` FOREIGN KEY (`AdjustmentType_ITID`) REFERENCES `adjustmenttype` (`ITID`),
  CONSTRAINT `fk_AdjustHeader_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`),
  CONSTRAINT `fk_AdjustHeader_user1` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `adjustmenttype`;
CREATE TABLE `adjustmenttype` (
  `ITID` int NOT NULL AUTO_INCREMENT,
  `AdjustmentTypeName` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`ITID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `adjustproddetails`;
CREATE TABLE `adjustproddetails` (
  `APID` int NOT NULL AUTO_INCREMENT,
  `AdjustProdQty` decimal(12,3) DEFAULT NULL,
  `UnitPurchasePrice` decimal(12,2) DEFAULT NULL,
  `UnitSellingPrice` decimal(12,2) DEFAULT NULL,
  `MnfDate` date DEFAULT NULL,
  `ExpDate` date DEFAULT NULL,
  `InventoryID` int DEFAULT NULL,
  `VariationID` int DEFAULT NULL,
  `RackID` int DEFAULT NULL,
  `AdjustStat` int DEFAULT NULL,
  `AdjustProdAmount` decimal(12,2) DEFAULT NULL,
  `products_PDID` int NOT NULL,
  `AdjustHeader_AHID` int NOT NULL,
  `batch_id` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`APID`),
  KEY `fk_AdjustProdDetails_products1_idx` (`products_PDID`),
  KEY `fk_AdjustProdDetails_AdjustHeader1_idx` (`AdjustHeader_AHID`),
  CONSTRAINT `fk_AdjustProdDetails_AdjustHeader1` FOREIGN KEY (`AdjustHeader_AHID`) REFERENCES `adjustheader` (`AHID`),
  CONSTRAINT `fk_AdjustProdDetails_products1` FOREIGN KEY (`products_PDID`) REFERENCES `products` (`PDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `batch`;
CREATE TABLE `batch` (
  `BTID` int NOT NULL AUTO_INCREMENT,
  `BatchNo` varchar(12) DEFAULT NULL,
  `BatchStat` tinyint DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`BTID`),
  KEY `fk_Batch_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_Batch_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `cashcounter`;
CREATE TABLE `cashcounter` (
  `CCID` int NOT NULL AUTO_INCREMENT,
  `CounterDate` date DEFAULT NULL,
  `StartBalance` decimal(12,2) DEFAULT NULL,
  `EndBalance` decimal(12,2) DEFAULT NULL,
  `CounterStartTime` datetime DEFAULT NULL,
  `CounterEndTime` datetime DEFAULT NULL,
  `CounterStat` tinyint DEFAULT NULL,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`CCID`),
  KEY `fk_CashCounter_user1_idx` (`user_USID`),
  KEY `fk_CashCounter_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_CashCounter_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`),
  CONSTRAINT `fk_CashCounter_user1` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `CTID` int NOT NULL AUTO_INCREMENT,
  `CategoryNo` varchar(12) DEFAULT NULL,
  `CategoryName` varchar(60) DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`CTID`),
  KEY `fk_categories_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_categories_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `cities`;
CREATE TABLE `cities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `district_id` int NOT NULL,
  `name_en` varchar(45) DEFAULT NULL,
  `name_si` varchar(45) DEFAULT NULL,
  `name_ta` varchar(45) DEFAULT NULL,
  `sub_name_en` varchar(45) DEFAULT NULL,
  `sub_name_si` varchar(45) DEFAULT NULL,
  `sub_name_ta` varchar(45) DEFAULT NULL,
  `postcode` varchar(15) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cities_districts1_idx` (`district_id`),
  CONSTRAINT `fk_cities_districts1` FOREIGN KEY (`district_id`) REFERENCES `districts` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `company`;
CREATE TABLE `company` (
  `CMID` int NOT NULL AUTO_INCREMENT,
  `CompanyNo` varchar(12) DEFAULT NULL,
  `ComName` varchar(60) DEFAULT NULL,
  `CompanyLocation` varchar(60) DEFAULT NULL,
  `LicenceNo` varchar(60) DEFAULT NULL,
  `VersionNo` varchar(60) DEFAULT NULL,
  `ComLogo` varchar(255) DEFAULT NULL,
  `ComStartDate` date DEFAULT NULL,
  `ComExpireDate` date DEFAULT NULL,
  `ComStat` tinyint DEFAULT NULL,
  `is_multicategory` tinyint DEFAULT NULL,
  `is_commonStock` int NOT NULL DEFAULT 0,
  `CompanyType_CTID` int NOT NULL,
  `last_updateDate` date NOT NULL DEFAULT (CURRENT_DATE),
  PRIMARY KEY (`CMID`),
  KEY `fk_Company_CompanyType1_idx` (`CompanyType_CTID`),
  CONSTRAINT `fk_Company_CompanyType1` FOREIGN KEY (`CompanyType_CTID`) REFERENCES `companytype` (`CTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `companytype`;
CREATE TABLE `companytype` (
  `CTID` int NOT NULL AUTO_INCREMENT,
  `CompanyTypeName` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`CTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `creditcustomer`;
CREATE TABLE `creditcustomer` (
  `CCID` int NOT NULL AUTO_INCREMENT,
  `EffectiveDate` date DEFAULT (CURRENT_DATE),
  `CreditAmount` decimal(12,2) DEFAULT 0.00,
  `DebitAmount` decimal(12,2) DEFAULT 0.00,
  `Balance` decimal(12,2) DEFAULT NULL,
  `SubmitDate` date DEFAULT (CURRENT_DATE),
  `DueDate` date DEFAULT NULL,
  `invoice_header_id` int DEFAULT NULL,
  `pay_m_id` int DEFAULT NULL COMMENT 'Payment Method ID',
  `CreditStat` int DEFAULT 1,
  `Customers_CTID` int NOT NULL,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `paymentMode` int NOT NULL DEFAULT 1 COMMENT '1=payment\r\n2=sales return credit reduction',
  PRIMARY KEY (`CCID`),
  KEY `fk_CreditCustomer_Customers1_idx` (`Customers_CTID`),
  KEY `fk_CreditCustomer_user1_idx` (`user_USID`),
  CONSTRAINT `fk_CreditCustomer_Customers1` FOREIGN KEY (`Customers_CTID`) REFERENCES `customers` (`CTID`),
  CONSTRAINT `fk_CreditCustomer_user1` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `creditsupplier`;
CREATE TABLE `creditsupplier` (
  `SCID` int NOT NULL AUTO_INCREMENT,
  `EffectiveDate` date DEFAULT NULL,
  `CreditAmount` decimal(12,2) DEFAULT 0.00,
  `DebitAmount` decimal(12,2) DEFAULT 0.00,
  `Balance` decimal(12,2) DEFAULT NULL,
  `SubmitDate` date DEFAULT NULL,
  `DueDate` date DEFAULT NULL,
  `invoice_header_id` int DEFAULT NULL,
  `remarks` text DEFAULT NULL,
  `pay_m_id` int NOT NULL COMMENT 'Payment Method ID',
  `CreditStat` int DEFAULT 1,
  `Supplier_ID` int NOT NULL,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`SCID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
DROP TABLE IF EXISTS `cuscredittransactions`;
CREATE TABLE `cuscredittransactions` (
  `CCTID` int NOT NULL AUTO_INCREMENT,
  `cuscreditTransactionAmount` decimal(10,2) NOT NULL,
  `cuscreditTransactionStat` tinyint NOT NULL DEFAULT 1,
  `invoice_id` int NOT NULL,
  `paymethod_id` int NOT NULL,
  `createDate` date NOT NULL DEFAULT (CURRENT_DATE),
  `created_dateTime` datetime NOT NULL DEFAULT current_timestamp(),
  `CreditCustomer_CCID` int NOT NULL,
  PRIMARY KEY (`CCTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `custcheq`;
CREATE TABLE `custcheq` (
  `CCQID` int NOT NULL AUTO_INCREMENT,
  `type` int NOT NULL DEFAULT 2 COMMENT '1=issued cheque\r\n2=received cheque',
  `chq_stat` int NOT NULL DEFAULT 1 COMMENT '0=inactive\r\n1=active\r\n2=transferred\r\n3=bounced cheque\r\n4=realized cheque',
  `chq_no` varchar(250) NOT NULL,
  `cust_CTID` int NOT NULL,
  `effectiveDate` date NOT NULL,
  `invoiceID` int NOT NULL,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `createdDate` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`CCQID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `custchqdetail`;
CREATE TABLE `custchqdetail` (
  `CCDID` int NOT NULL AUTO_INCREMENT,
  `bank` varchar(250) NOT NULL,
  `chqAmount` decimal(12,2) NOT NULL,
  `chqNo` varchar(25) NOT NULL,
  `chqDate` date NOT NULL,
  `invoiceID` int NOT NULL,
  `CCQID` int NOT NULL,
  PRIMARY KEY (`CCDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
  `CTID` int NOT NULL AUTO_INCREMENT,
  `CustomerNo` varchar(12) DEFAULT NULL,
  `CustName` varchar(120) DEFAULT NULL,
  `CustGender` int NOT NULL DEFAULT 1 COMMENT '0=other\r\n1=Male\r\n2=Female',
  `CustDOB` date DEFAULT NULL,
  `CustAddress` varchar(255) DEFAULT NULL,
  `CustContact` varchar(12) DEFAULT NULL,
  `MaxCreditAmount` decimal(12,2) DEFAULT 100000.00 COMMENT 'maximum  credit amount 100000',
  `PaymentTerm` int DEFAULT NULL,
  `CustStat` int DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`CTID`),
  KEY `fk_Customers_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_Customers_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `dayendsummary`;
CREATE TABLE `dayendsummary` (
  `DSID` int NOT NULL AUTO_INCREMENT,
  `ActiveDate` date DEFAULT NULL,
  `TotalGrossSale` decimal(12,2) DEFAULT NULL,
  `TotalNetSale` decimal(12,2) DEFAULT NULL,
  `TotalBillCount` int DEFAULT NULL,
  `TotalCounters` int DEFAULT NULL,
  `UsersCount` int DEFAULT NULL,
  `TotalReturnCount` int DEFAULT NULL,
  `TotalReturnAmount` decimal(12,2) DEFAULT NULL,
  `TotalCounterStartBalance` decimal(12,2) DEFAULT NULL,
  `TotalCounterEndBalance` decimal(12,2) DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`DSID`),
  KEY `fk_DayEndSummary_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_DayEndSummary_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `districts`;
CREATE TABLE `districts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `province_id` int NOT NULL,
  `name_en` varchar(45) DEFAULT NULL,
  `name_si` varchar(45) DEFAULT NULL,
  `name_ta` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `provinces_id` (`province_id`),
  CONSTRAINT `fk_districts_provinces1` FOREIGN KEY (`province_id`) REFERENCES `provinces` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `docno`;
CREATE TABLE `docno` (
  `DNID` int NOT NULL AUTO_INCREMENT,
  `tmp_no` int DEFAULT NULL,
  `org_no` int DEFAULT NULL,
  `ws_no` int NOT NULL DEFAULT 0,
  `shop_id` int DEFAULT NULL,
  PRIMARY KEY (`DNID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `expensecategory`;
CREATE TABLE `expensecategory` (
  `ECID` int NOT NULL AUTO_INCREMENT,
  `expense_ctg` text NOT NULL,
  `expense_ETID` int NOT NULL,
  PRIMARY KEY (`ECID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `expensereason`;
CREATE TABLE `expensereason` (
  `ERID` int NOT NULL AUTO_INCREMENT,
  `ExpenseReasonName` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`ERID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `expenses`;
CREATE TABLE `expenses` (
  `EPID` int NOT NULL AUTO_INCREMENT,
  `EffectiveDate` date DEFAULT NULL,
  `ExpenseAmount` decimal(12,2) DEFAULT NULL,
  `ExpenseReason` varchar(60) DEFAULT NULL,
  `expensecategory_id` varchar(255) NOT NULL,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `counter_id` int DEFAULT 1,
  PRIMARY KEY (`EPID`),
  KEY `fk_Expenses_user1_idx` (`user_USID`),
  KEY `fk_Expenses_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_Expenses_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`),
  CONSTRAINT `fk_Expenses_user1` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `expensetransactions`;
CREATE TABLE `expensetransactions` (
  `ETID` int NOT NULL AUTO_INCREMENT,
  `expTransactionAmount` decimal(10,2) NOT NULL,
  `expTransactionStat` tinyint NOT NULL,
  `expense_id` int NOT NULL,
  `paymethod_id` int NOT NULL,
  PRIMARY KEY (`ETID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `expensetype`;
CREATE TABLE `expensetype` (
  `ETID` int NOT NULL AUTO_INCREMENT,
  `expense_type` text NOT NULL,
  PRIMARY KEY (`ETID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `grndetails`;
CREATE TABLE `grndetails` (
  `GDID` int NOT NULL AUTO_INCREMENT,
  `InitQty` decimal(12,3) DEFAULT NULL,
  `CurrentQty` decimal(12,3) DEFAULT NULL,
  `UnitPurchasePrice` decimal(12,2) DEFAULT NULL,
  `UnitLabelPrice` decimal(12,2) DEFAULT NULL,
  `UnitSellPrice` decimal(12,2) DEFAULT NULL,
  `TotalPurchasePrice` decimal(12,2) DEFAULT NULL,
  `TotalSellPrice` decimal(12,2) DEFAULT NULL,
  `MnfDate` date DEFAULT NULL,
  `ExpDate` date DEFAULT NULL,
  `GRNStat` int DEFAULT NULL,
  `VariationID` int DEFAULT NULL,
  `products_PDID` int NOT NULL,
  `GRNHeader_GHID` int NOT NULL,
  `Rack_RKID` int NOT NULL,
  PRIMARY KEY (`GDID`),
  KEY `fk_GRNDetails_products1_idx` (`products_PDID`),
  KEY `fk_GRNDetails_GRNHeader1_idx` (`GRNHeader_GHID`),
  KEY `fk_GRNDetails_Rack1_idx` (`Rack_RKID`),
  CONSTRAINT `fk_GRNDetails_GRNHeader1` FOREIGN KEY (`GRNHeader_GHID`) REFERENCES `grnheader` (`GHID`),
  CONSTRAINT `fk_GRNDetails_Rack1` FOREIGN KEY (`Rack_RKID`) REFERENCES `rack` (`RKID`),
  CONSTRAINT `fk_GRNDetails_products1` FOREIGN KEY (`products_PDID`) REFERENCES `products` (`PDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `grnheader`;
CREATE TABLE `grnheader` (
  `GHID` int NOT NULL AUTO_INCREMENT,
  `GRNHeaderNo` varchar(12) DEFAULT NULL,
  `EffectiveDate` date DEFAULT NULL,
  `InvoiceNo` varchar(45) DEFAULT NULL,
  `ItemCount` int DEFAULT NULL,
  `TotalPurchasePrice` decimal(12,2) DEFAULT NULL,
  `TotalSellPrice` decimal(12,2) DEFAULT NULL,
  `GRNStartTime` datetime DEFAULT NULL,
  `GRNEndTime` datetime DEFAULT NULL,
  `GRNStat` int DEFAULT NULL,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `Suppliers_SPID` int NOT NULL,
  `SuppPayment` decimal(12,2) NOT NULL,
  `SuppBalance` decimal(12,2) NOT NULL,
  `excessAmount` decimal(12,2) NOT NULL,
  `refference` text NOT NULL,
  `PurchDiscType` int DEFAULT 0,
  `PurchDisc` decimal(18,2) DEFAULT 0.00,
  `TotalDisc` decimal(18,2) NOT NULL DEFAULT 0.00,
  `TotalOriginalPurchase` decimal(18,2) DEFAULT 0.00,
  PRIMARY KEY (`GHID`),
  KEY `fk_GRNHeader_user1_idx` (`user_USID`),
  KEY `fk_GRNHeader_shop1_idx` (`shop_SHID`),
  KEY `fk_GRNHeader_Suppliers1_idx` (`Suppliers_SPID`),
  CONSTRAINT `fk_GRNHeader_Suppliers1` FOREIGN KEY (`Suppliers_SPID`) REFERENCES `suppliers` (`SPID`),
  CONSTRAINT `fk_GRNHeader_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`),
  CONSTRAINT `fk_GRNHeader_user1` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `hold_invoice`;
CREATE TABLE `hold_invoice` (
  `HIID` int NOT NULL AUTO_INCREMENT,
  `Temp_No` varchar(12) DEFAULT NULL,
  `Inv_Type` int NOT NULL DEFAULT 1 COMMENT '1= Wholesale Invoice\r\n2= gui pos',
  `EffectiveDate` date DEFAULT NULL,
  `BillNo` varchar(12) DEFAULT NULL,
  `InvStartTime` datetime DEFAULT NULL,
  `InvEndTime` datetime DEFAULT NULL,
  `InvItemCount` int DEFAULT NULL,
  `GrossAmount` decimal(12,2) DEFAULT NULL,
  `lineDiscount` decimal(12,2) DEFAULT 0.00,
  `PercentDiscount` decimal(12,2) DEFAULT 0.00,
  `FixedDiscount` decimal(12,2) DEFAULT 0.00,
  `DiscountAmount` decimal(12,2) DEFAULT NULL,
  `deliveryCharge` decimal(12,2) NOT NULL DEFAULT 0.00,
  `otherCharge` decimal(12,2) NOT NULL DEFAULT 0.00,
  `excessAmount` decimal(12,2) DEFAULT 0.00,
  `returnAmount` decimal(12,2) DEFAULT 0.00,
  `NetAmount` decimal(12,2) DEFAULT NULL,
  `CustPayment` decimal(12,2) DEFAULT NULL,
  `CustBalance` decimal(12,2) DEFAULT NULL,
  `InvStat` tinyint DEFAULT NULL COMMENT '0=cancel\r\n1=active \r\n5=claimbill',
  `remarks` text DEFAULT NULL,
  `user_USID` int NOT NULL,
  `customers_CTID` int DEFAULT NULL,
  `Salesmans_SLID` int NOT NULL,
  `ReturnHeader_RHID` int DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  `CashCounter_CCID` int NOT NULL,
  `print_count` int NOT NULL DEFAULT 1,
  `is_delivery` int NOT NULL DEFAULT 0,
  `deliveryPartner` int NOT NULL DEFAULT 0,
  `sales_source` int DEFAULT NULL,
  PRIMARY KEY (`HIID`),
  KEY `fk_InvoiceHeader_user1_idx` (`user_USID`),
  KEY `fk_InvoiceHeader_Salesmans1_idx` (`Salesmans_SLID`),
  KEY `fk_InvoiceHeader_shop1_idx` (`shop_SHID`),
  KEY `fk_InvoiceHeader_CashCounter1_idx` (`CashCounter_CCID`),
  KEY `customers_CTID` (`customers_CTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory` (
  `INID` int NOT NULL AUTO_INCREMENT,
  `CurrentQty` decimal(12,3) DEFAULT NULL,
  `BillQty` decimal(12,3) DEFAULT NULL,
  `ReturnQty` decimal(12,3) DEFAULT NULL,
  `TransferInQty` decimal(12,3) DEFAULT NULL,
  `TransferOutQty` decimal(12,3) DEFAULT NULL,
  `Sup_Rtn` decimal(12,3) NOT NULL DEFAULT 0.000,
  `products_PDID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `RackID` int DEFAULT NULL,
  `is_default` int NOT NULL DEFAULT 0,
  `BatchID` varchar(11) DEFAULT NULL,
  PRIMARY KEY (`INID`),
  KEY `fk_Inventory_products1_idx` (`products_PDID`),
  KEY `fk_Inventory_shop1_idx` (`shop_SHID`),
  CONSTRAINT `fk_Inventory_products1` FOREIGN KEY (`products_PDID`) REFERENCES `products` (`PDID`),
  CONSTRAINT `fk_Inventory_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `inventory_consumption`;
CREATE TABLE `inventory_consumption` (
  `ICID` int NOT NULL,
  `invoice_headerID` int NOT NULL COMMENT 'INID',
  `status` int NOT NULL DEFAULT 1 COMMENT '0= inactive\r\n1=active',
  `inventory_INID` int NOT NULL COMMENT 'INID Inventory ID',
  `price` decimal(12,2) NOT NULL COMMENT 'original product price',
  `sold_price` decimal(12,2) NOT NULL,
  `batch No` int NOT NULL,
  `product_PDID` int NOT NULL COMMENT 'PDID Product ID',
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `quantity` decimal(12,2) NOT NULL,
  `shop_SHID` int NOT NULL COMMENT 'SHID shop ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `invoice_remarks`;
CREATE TABLE `invoice_remarks` (
  `IRID` int NOT NULL AUTO_INCREMENT,
  `remarks` text NOT NULL,
  `from_invoice` int NOT NULL DEFAULT 0 COMMENT '0=not from invoice\r\n1=from invoice',
  `user_USID` int NOT NULL,
  `invoiceheader_IHID` int NOT NULL,
  `date_time` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`IRID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `invoicedetails`;
CREATE TABLE `invoicedetails` (
  `IDID` int NOT NULL AUTO_INCREMENT,
  `Item_Name` text NOT NULL,
  `SellQty` decimal(12,3) DEFAULT NULL,
  `UnitPrice` decimal(12,2) DEFAULT NULL,
  `SellAmount` decimal(12,2) DEFAULT NULL,
  `PercentDiscount` decimal(12,2) DEFAULT NULL,
  `DirectDiscount` decimal(12,2) DEFAULT NULL,
  `SellDiscount` decimal(12,2) DEFAULT NULL,
  `disc_type` int DEFAULT 0 COMMENT '1=percentage\r\n2= flat discount',
  `ItemType` int NOT NULL DEFAULT 1 COMMENT '1=product\r\n2=service',
  `SoldAmount` decimal(12,2) DEFAULT NULL,
  `WarrantyStart` date DEFAULT NULL,
  `WarrantyEnd` date DEFAULT NULL,
  `ReferenceNo` varchar(45) DEFAULT NULL,
  `InvoiceHeader_IHID` int NOT NULL,
  `products_PDID` int NOT NULL,
  `item_des` varchar(250) NOT NULL,
  `batch_no` varchar(12) DEFAULT NULL,
  `Inventory_INID` int NOT NULL,
  `shop_id` int NOT NULL,
  PRIMARY KEY (`IDID`),
  KEY `fk_InvoiceDetails_InvoiceHeader1_idx` (`InvoiceHeader_IHID`),
  KEY `fk_InvoiceDetails_products1_idx` (`products_PDID`),
  CONSTRAINT `fk_InvoiceDetails_products1` FOREIGN KEY (`products_PDID`) REFERENCES `products` (`PDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `invoiceheader`;
CREATE TABLE `invoiceheader` (
  `IHID` int NOT NULL AUTO_INCREMENT,
  `InvoiceNo` varchar(12) DEFAULT NULL,
  `Inv_Type` int NOT NULL DEFAULT 1 COMMENT '1= Wholesale Invoice\r\n2= gui pos',
  `EffectiveDate` date DEFAULT NULL,
  `BillNo` varchar(12) DEFAULT NULL,
  `InvStartTime` datetime DEFAULT NULL,
  `InvEndTime` datetime DEFAULT NULL,
  `InvItemCount` int DEFAULT NULL,
  `GrossAmount` decimal(12,2) DEFAULT NULL,
  `lineDiscount` decimal(12,2) DEFAULT 0.00 COMMENT 'Item Wise Total Discount',
  `PercentDiscount` decimal(12,2) DEFAULT 0.00 COMMENT 'if the invoice discount type percentage',
  `FixedDiscount` decimal(12,2) DEFAULT 0.00 COMMENT 'if the invoice discount type flat rate',
  `DiscountAmount` decimal(12,2) DEFAULT NULL COMMENT 'Total Discount',
  `discountType` int NOT NULL DEFAULT 1 COMMENT '1=percentage\r\n2=flat',
  `deliveryCharge` decimal(12,2) NOT NULL DEFAULT 0.00,
  `otherCharge` decimal(12,2) NOT NULL DEFAULT 0.00,
  `excessAmount` decimal(12,2) DEFAULT 0.00,
  `returnAmount` decimal(12,2) DEFAULT 0.00,
  `NetAmount` decimal(12,2) DEFAULT NULL,
  `CustPayment` decimal(12,2) DEFAULT NULL,
  `CustBalance` decimal(12,2) DEFAULT NULL,
  `InvStat` tinyint DEFAULT NULL COMMENT '0=cancel\r\n1=active \r\n5=claimbill',
  `remarks` text DEFAULT NULL,
  `user_USID` int NOT NULL,
  `customers_CTID` int DEFAULT NULL,
  `Salesmans_SLID` int NOT NULL,
  `ReturnHeader_RHID` int DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  `CashCounter_CCID` int NOT NULL DEFAULT 0,
  `print_count` int NOT NULL DEFAULT 1,
  `is_delivery` int NOT NULL DEFAULT 0,
  `deliveryPartner` varchar(120) NOT NULL,
  `sales_source` int DEFAULT NULL,
  `HIID` int DEFAULT NULL COMMENT 'hold invoice id',
  PRIMARY KEY (`IHID`),
  KEY `fk_InvoiceHeader_user1_idx` (`user_USID`),
  KEY `fk_InvoiceHeader_Salesmans1_idx` (`Salesmans_SLID`),
  KEY `customers_CTID` (`customers_CTID`),
  KEY `fk_InvoiceHeader_CashCounter1_id` (`CashCounter_CCID`) USING BTREE,
  KEY `fk_InvoiceHeader_shop1` (`shop_SHID`),
  CONSTRAINT `fk_InvoiceHeader_Salesmans1` FOREIGN KEY (`Salesmans_SLID`) REFERENCES `salesmans` (`SLID`),
  CONSTRAINT `fk_InvoiceHeader_shop1` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`),
  CONSTRAINT `fk_InvoiceHeader_user1` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`),
  CONSTRAINT `invoiceheader_ibfk_1` FOREIGN KEY (`customers_CTID`) REFERENCES `customers` (`CTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `label`;
CREATE TABLE `label` (
  `LBID` int NOT NULL AUTO_INCREMENT,
  `LabelName` varchar(120) DEFAULT NULL,
  `LabelPath` varchar(255) DEFAULT NULL,
  `dpi` decimal(10,2) NOT NULL,
  `numRow` decimal(10,2) NOT NULL,
  `numCol` decimal(10,2) NOT NULL,
  `lblWidth` decimal(10,2) NOT NULL,
  `lblHeight` decimal(10,2) NOT NULL,
  `stkWidth` decimal(10,2) NOT NULL,
  `stkHeight` decimal(10,2) NOT NULL,
  `stkMarginLeft` decimal(10,2) NOT NULL,
  `stkMarginRight` decimal(10,2) NOT NULL,
  `stkMarginTop` decimal(10,2) NOT NULL,
  `stkMarginBottom` decimal(10,2) NOT NULL,
  `lblStat` tinyint NOT NULL,
  `shop_id` int DEFAULT NULL,
  PRIMARY KEY (`LBID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `multipay`;
CREATE TABLE `multipay` (
  `MPID` int NOT NULL AUTO_INCREMENT,
  `paidAmount` decimal(12,2) DEFAULT NULL,
  `payStat` smallint DEFAULT NULL,
  `paymethod_id` int DEFAULT NULL,
  `sellheader_id` int DEFAULT NULL,
  `returnheader_id` int DEFAULT NULL,
  PRIMARY KEY (`MPID`),
  KEY `sellheader_id` (`sellheader_id`),
  KEY `paymethod_id` (`paymethod_id`),
  CONSTRAINT `multipay_ibfk_1` FOREIGN KEY (`sellheader_id`) REFERENCES `sellheader` (`SHID`),
  CONSTRAINT `multipay_ibfk_2` FOREIGN KEY (`paymethod_id`) REFERENCES `paymethod` (`PMID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `paymethod`;
CREATE TABLE `paymethod` (
  `PMID` int NOT NULL AUTO_INCREMENT,
  `PaymethodName` varchar(45) DEFAULT NULL,
  `image_path` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PMID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `prescription_va`;
CREATE TABLE `prescription_va` (
  `PVA_ID` int NOT NULL AUTO_INCREMENT,
  `eye` int NOT NULL COMMENT '1=right\r\n2=left',
  `uva` varchar(255) NOT NULL,
  `ph` varchar(255) NOT NULL,
  `pres_id` int NOT NULL,
  PRIMARY KEY (`PVA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `prescriptiondetails`;
CREATE TABLE `prescriptiondetails` (
  `PRDID` int NOT NULL AUTO_INCREMENT,
  `prescription_PRHID` int NOT NULL,
  `side` int NOT NULL COMMENT '1=right\r\n2=left',
  `prescription_type` int NOT NULL COMMENT '1=Subjective Refraction\r\n2=Present Prescription',
  `add` int NOT NULL DEFAULT 0 COMMENT '1=add\r\n0= non add',
  `sph` text NOT NULL,
  `cyl` text NOT NULL,
  `axis` text NOT NULL,
  `none` text DEFAULT NULL,
  PRIMARY KEY (`PRDID`),
  KEY `prescription_PRHID` (`prescription_PRHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `prescriptionheader`;
CREATE TABLE `prescriptionheader` (
  `PRHID` int NOT NULL AUTO_INCREMENT,
  `pr_no` varchar(255) NOT NULL,
  `customer_CTID` int NOT NULL,
  `date` datetime NOT NULL DEFAULT current_timestamp(),
  `pr_subjective_ref` text NOT NULL,
  `pr_hb` text NOT NULL,
  `pr_refraction` text NOT NULL,
  `pr_remarks` text NOT NULL,
  `pr_va` varchar(255) NOT NULL,
  `invoice_IHID` int DEFAULT NULL,
  `user_USID` int NOT NULL,
  `shop_ID` int NOT NULL,
  `staus` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`PRHID`),
  KEY `customer_CTID` (`customer_CTID`),
  KEY `user_USID` (`user_USID`),
  KEY `shop_ID` (`shop_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `pricechangelog`;
CREATE TABLE `pricechangelog` (
  `PPCID` int NOT NULL AUTO_INCREMENT,
  `product_PDID` int DEFAULT NULL COMMENT 'Product ID',
  `varriation_id` int DEFAULT NULL,
  `batch_id` varchar(25) NOT NULL,
  `old_selling_price` decimal(12,2) NOT NULL,
  `old_label_price` decimal(12,2) NOT NULL,
  `new_selling_price` decimal(12,2) NOT NULL,
  `new_label_price` decimal(12,2) NOT NULL,
  `user_USID` int NOT NULL,
  `date` datetime NOT NULL DEFAULT current_timestamp(),
  `priceHistory_PHID` int NOT NULL,
  `status` int NOT NULL DEFAULT 1,
  `shop_id` int NOT NULL,
  PRIMARY KEY (`PPCID`),
  KEY `product_PDID` (`product_PDID`),
  KEY `user_USID` (`user_USID`),
  KEY `priceHistory_PHID` (`priceHistory_PHID`),
  KEY `shop_id` (`shop_id`),
  CONSTRAINT `pricechangelog_ibfk_1` FOREIGN KEY (`product_PDID`) REFERENCES `products` (`PDID`),
  CONSTRAINT `pricechangelog_ibfk_2` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`),
  CONSTRAINT `pricechangelog_ibfk_3` FOREIGN KEY (`priceHistory_PHID`) REFERENCES `pricehistory` (`PHID`),
  CONSTRAINT `pricechangelog_ibfk_4` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `pricehistory`;
CREATE TABLE `pricehistory` (
  `PHID` int NOT NULL AUTO_INCREMENT,
  `ProductID` int DEFAULT NULL COMMENT 'Without a foreign key we pass the product_id.',
  `VariationID` int DEFAULT NULL COMMENT 'without a foreign key variation ID can be null, if there is  no variations',
  `EffectiveDate` date DEFAULT NULL,
  `PurchasePrice` decimal(12,2) DEFAULT NULL,
  `SellingPrice` decimal(12,2) DEFAULT NULL,
  `labelPrice` decimal(10,2) DEFAULT NULL,
  `MnfDate` date DEFAULT NULL,
  `ExpDate` date DEFAULT NULL,
  `BatchID` varchar(45) DEFAULT NULL,
  `Inventory_INID` int NOT NULL,
  `GrnDetailID` int DEFAULT NULL,
  PRIMARY KEY (`PHID`),
  KEY `fk_PriceHistory_Inventory1_idx` (`Inventory_INID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
  `PDID` int NOT NULL AUTO_INCREMENT,
  `ProductNo` varchar(12) DEFAULT NULL,
  `ProdImage` varchar(255) DEFAULT NULL,
  `Barcode` varchar(45) DEFAULT NULL,
  `ItemName` varchar(120) DEFAULT NULL,
  `ProdDescription` mediumtext DEFAULT NULL,
  `SecondName` varchar(120) DEFAULT NULL,
  `ProdPurchasePrice` decimal(12,2) DEFAULT NULL,
  `ProdSellPrice` decimal(12,2) DEFAULT NULL,
  `CartonQty` int DEFAULT 1,
  `ProductStat` tinyint DEFAULT NULL,
  `AddedDate` date DEFAULT NULL,
  `UpdatedDate` date DEFAULT NULL,
  `ItemType` varchar(1) DEFAULT NULL,
  `user_USID` int NOT NULL,
  `UpdateUserID` int DEFAULT NULL,
  `Subcategories_SCID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `PurchaseUnit` int DEFAULT NULL,
  `UnitConversion` decimal(12,3) DEFAULT NULL,
  `SellingUnit` int DEFAULT NULL,
  `prodDiscount` decimal(18,2) DEFAULT 0.00,
  `prodFlatDiscount` decimal(12,2) NOT NULL,
  `is_fixedPrice` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`PDID`),
  KEY `fk_products_user1_idx` (`user_USID`),
  KEY `fk_products_Subcategories1_idx` (`Subcategories_SCID`),
  KEY `fk_products_shop1_idx` (`shop_SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `provinces`;
CREATE TABLE `provinces` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name_en` varchar(45) NOT NULL,
  `name_si` varchar(45) DEFAULT NULL,
  `name_ta` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `rack`;
CREATE TABLE `rack` (
  `RKID` int NOT NULL AUTO_INCREMENT,
  `RackNo` varchar(10) DEFAULT NULL,
  `RackName` varchar(45) DEFAULT NULL,
  `Sections_SEID` int NOT NULL,
  PRIMARY KEY (`RKID`),
  KEY `fk_Rack_Sections1_idx` (`Sections_SEID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `return_invoice_header`;
CREATE TABLE `return_invoice_header` (
  `RIHID` int NOT NULL AUTO_INCREMENT,
  `reason` varchar(255) NOT NULL,
  `return_type` int NOT NULL COMMENT '1=cash refund\r\n2=exchange',
  `usability` int NOT NULL DEFAULT 1 COMMENT '1=Adjust Stock\r\n0=Damage Stock',
  `InvoiceNo` varchar(255) DEFAULT NULL,
  `EffectiveDate` date NOT NULL DEFAULT (CURRENT_DATE),
  `InvStartTime` datetime NOT NULL DEFAULT current_timestamp(),
  `IHID` int DEFAULT NULL,
  `Customer_CTID` int NOT NULL,
  `returnby` int NOT NULL,
  `shopID` int NOT NULL,
  `return_no` varchar(255) NOT NULL,
  `return_amount` decimal(12,2) NOT NULL,
  `return_discount` decimal(12,2) NOT NULL,
  `return_gross_amount` decimal(12,2) NOT NULL,
  `return_count` int NOT NULL,
  `return_header_stat` smallint DEFAULT 0,
  `CashCounter_CCID` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`RIHID`),
  KEY `fk_return_invoice_header_shop1` (`shopID`),
  KEY `fk_return_invoice_header_user1` (`returnby`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `returndetails`;
CREATE TABLE `returndetails` (
  `RDID` int NOT NULL AUTO_INCREMENT,
  `ReturnQty` decimal(12,3) DEFAULT NULL,
  `ReturnAmount` decimal(12,2) DEFAULT NULL,
  `return_unit_price` decimal(12,2) NOT NULL,
  `return_discount_type` int NOT NULL DEFAULT 2 COMMENT '1=percentage\r\n2= flate',
  `return_discount` decimal(12,2) NOT NULL,
  `ReturnHeader_RHID` int DEFAULT NULL,
  `InvoiceDetails_IDID` int DEFAULT NULL,
  `inventory_INID` int NOT NULL,
  `products_PDID` int NOT NULL,
  `batch_id` varchar(255) NOT NULL,
  PRIMARY KEY (`RDID`),
  KEY `fk_ReturnDetails_products1_id` (`products_PDID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `sales_order_details`;
CREATE TABLE `sales_order_details` (
  `id` int NOT NULL AUTO_INCREMENT,
  `sales_order_id` int NOT NULL,
  `product_id` int NOT NULL,
  `Inv_id` int NOT NULL,
  `PriceHis_id` int NOT NULL,
  `product_name` varchar(255) NOT NULL,
  `quantity` int NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `total` decimal(10,2) NOT NULL,
  `Add_date` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
DROP TABLE IF EXISTS `sales_orders`;
CREATE TABLE `sales_orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `SalesOrderNo` varchar(255) NOT NULL,
  `SalesOrderDate` date DEFAULT NULL,
  `customer_id` int NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `order_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
DROP TABLE IF EXISTS `salesettings`;
CREATE TABLE `salesettings` (
  `SSID` int NOT NULL AUTO_INCREMENT,
  `billAddOption` varchar(45) DEFAULT NULL,
  `qtyAddDuration` int DEFAULT NULL,
  `billNoHeader` varchar(4) DEFAULT NULL,
  `WbillNoHeader` varchar(10) DEFAULT NULL,
  `settingStat` smallint DEFAULT NULL,
  `shop_id` int DEFAULT NULL,
  `countertype_id` int DEFAULT NULL,
  PRIMARY KEY (`SSID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `salesmans`;
CREATE TABLE `salesmans` (
  `SLID` int NOT NULL AUTO_INCREMENT,
  `SalesmanNo` varchar(12) DEFAULT NULL,
  `SalesmansName` varchar(60) DEFAULT NULL,
  `SalesmansContact` varchar(12) DEFAULT NULL,
  `commision_rate` decimal(10,2) NOT NULL DEFAULT 0.00,
  `SalesmanStat` int DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`SLID`),
  KEY `fk_Salesmans_shop1_idx` (`shop_SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `salesreturntype`;
CREATE TABLE `salesreturntype` (
  `SRTID` int NOT NULL AUTO_INCREMENT,
  `SRT_Name` varchar(255) NOT NULL,
  `SRT_Des` text NOT NULL,
  PRIMARY KEY (`SRTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `salessources`;
CREATE TABLE `salessources` (
  `SSUID` int NOT NULL AUTO_INCREMENT,
  `source_name` text NOT NULL,
  PRIMARY KEY (`SSUID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `sections`;
CREATE TABLE `sections` (
  `SEID` int NOT NULL AUTO_INCREMENT,
  `SectionNo` varchar(10) DEFAULT NULL,
  `SectionName` varchar(45) DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`SEID`),
  KEY `fk_Sections_shop1_idx` (`shop_SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `selldetail`;
CREATE TABLE `selldetail` (
  `IDID` int NOT NULL AUTO_INCREMENT,
  `Item_Name` text NOT NULL,
  `SellQty` decimal(12,3) DEFAULT NULL,
  `UnitPrice` decimal(12,2) DEFAULT NULL,
  `origi_UnitPrice` decimal(12,2) NOT NULL,
  `SellAmount` decimal(12,2) DEFAULT NULL,
  `PercentDiscount` decimal(12,2) DEFAULT NULL,
  `DirectDiscount` decimal(12,2) DEFAULT NULL,
  `SellDiscount` decimal(12,2) DEFAULT NULL,
  `disc_type` int DEFAULT 0 COMMENT '1=percentage\r\n2= flat discount',
  `SoldAmount` decimal(12,2) DEFAULT NULL,
  `WarrantyStart` date DEFAULT NULL,
  `WarrantyEnd` date DEFAULT NULL,
  `ReferenceNo` varchar(45) DEFAULT NULL,
  `products_PDID` int NOT NULL,
  `item_des` varchar(250) NOT NULL,
  `batch_no` varchar(12) DEFAULT NULL,
  `shop_id` int NOT NULL,
  `sellHeader_SHID` int NOT NULL,
  PRIMARY KEY (`IDID`),
  KEY `fk_InvoiceDetails_products1_idx` (`products_PDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `sellheader`;
CREATE TABLE `sellheader` (
  `SHID` int NOT NULL AUTO_INCREMENT,
  `tmp_bill_no` varchar(12) DEFAULT NULL,
  `ItemCount` int DEFAULT NULL,
  `GrossAmount` decimal(12,2) DEFAULT NULL,
  `PercentDiscount` decimal(12,2) DEFAULT NULL,
  `FixedDiscount` decimal(12,2) DEFAULT NULL,
  `lineDiscount` decimal(12,2) DEFAULT NULL,
  `DiscountAmount` decimal(12,2) DEFAULT NULL,
  `NetAmount` decimal(12,2) DEFAULT NULL,
  `SellStat` smallint DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `shop_id` int DEFAULT NULL,
  `cashcounter_id` int DEFAULT NULL,
  `EffectiveDate` date DEFAULT NULL,
  PRIMARY KEY (`SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop` (
  `SHID` int NOT NULL AUTO_INCREMENT,
  `ShopNo` varchar(12) DEFAULT NULL,
  `ShopName` varchar(45) DEFAULT NULL,
  `ShopLogo` varchar(255) DEFAULT NULL,
  `ReceiptLogo` varchar(255) DEFAULT NULL,
  `WholesaleShop` tinyint(1) NOT NULL COMMENT 'Wholesale shop',
  `RetailShop` tinyint(1) NOT NULL COMMENT 'Retail shop',
  `is_inventory` tinyint DEFAULT NULL,
  `is_minus` tinyint DEFAULT NULL,
  `is_category` tinyint DEFAULT NULL,
  `is_expire` tinyint DEFAULT NULL,
  `is_variation` tinyint DEFAULT NULL,
  `is_suppliers` tinyint DEFAULT NULL,
  `is_service` tinyint DEFAULT NULL,
  `is_salesman` tinyint DEFAULT NULL,
  `is_expenses` tinyint DEFAULT NULL,
  `is_customers` tinyint DEFAULT NULL,
  `is_fixedprice` tinyint DEFAULT NULL,
  `is_carton` tinyint DEFAULT NULL,
  `is_warranty` tinyint DEFAULT NULL,
  `is_promotions` tinyint DEFAULT NULL,
  `is_secondlan` tinyint DEFAULT NULL,
  `is_labelprice` tinyint DEFAULT NULL,
  `is_quotation` tinyint DEFAULT NULL,
  `is_racks` tinyint DEFAULT NULL,
  `is_credit` tinyint DEFAULT NULL,
  `invoice_print` tinyint NOT NULL DEFAULT 1,
  `is_prescription` tinyint(1) NOT NULL,
  `is_counter` int NOT NULL DEFAULT 1,
  `is_excessAmount` int NOT NULL DEFAULT 0,
  `is_BatchNo` int NOT NULL DEFAULT 0,
  `is_under_cost` int NOT NULL DEFAULT 0,
  `ShopStat` tinyint DEFAULT NULL,
  `Company_CMID` int NOT NULL,
  `StockTypes_STID` int NOT NULL,
  `AddressLineOne` varchar(255) DEFAULT NULL,
  `AddressLineTwo` varchar(255) DEFAULT NULL,
  `City` varchar(120) DEFAULT NULL,
  `emailAddress` varchar(255) NOT NULL,
  `PhoneNumber` varchar(25) DEFAULT NULL,
  PRIMARY KEY (`SHID`),
  KEY `fk_shop_Company1_idx` (`Company_CMID`),
  KEY `fk_shop_StockTypes1_idx` (`StockTypes_STID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shopfeatures`;
CREATE TABLE `shopfeatures` (
  `SPFID` int NOT NULL AUTO_INCREMENT,
  `FeatureName` varchar(250) NOT NULL,
  PRIMARY KEY (`SPFID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shoppaymethod`;
CREATE TABLE `shoppaymethod` (
  `SPID` int NOT NULL AUTO_INCREMENT,
  `shop_SHID` int NOT NULL,
  `paymethod_PMID` int NOT NULL,
  PRIMARY KEY (`SPID`),
  KEY `fk_ShopPaymethod_shop1_idx` (`shop_SHID`),
  KEY `fk_ShopPaymethod_paymethod1_idx` (`paymethod_PMID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shoppermissions`;
CREATE TABLE `shoppermissions` (
  `SPPID` int NOT NULL AUTO_INCREMENT,
  `ShopFeature_SPFID` int NOT NULL,
  `Shop_SHID` int NOT NULL,
  `is_active` int NOT NULL,
  PRIMARY KEY (`SPPID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shopreceipts`;
CREATE TABLE `shopreceipts` (
  `SRID` int NOT NULL AUTO_INCREMENT,
  `receiptName` varchar(45) DEFAULT NULL,
  `shop_id` int DEFAULT NULL,
  `is_default` tinyint DEFAULT NULL,
  `ReceiptStat` tinyint DEFAULT NULL,
  `ReceiptPath` varchar(255) DEFAULT NULL,
  `RecieptType` int NOT NULL DEFAULT 1 COMMENT '1 = GUI Invoice\r\n2 = WHolesale Invoice',
  PRIMARY KEY (`SRID`),
  KEY `shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shopusers`;
CREATE TABLE `shopusers` (
  `SUID` int NOT NULL AUTO_INCREMENT,
  `shop_SHID` int NOT NULL,
  `user_USID` int NOT NULL,
  PRIMARY KEY (`SUID`),
  KEY `fk_ShopUsers_shop1_idx` (`shop_SHID`),
  KEY `fk_ShopUsers_user1_idx` (`user_USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `shortcutkeys`;
CREATE TABLE `shortcutkeys` (
  `SKID` int NOT NULL AUTO_INCREMENT,
  `keys` varchar(255) NOT NULL,
  `description` varchar(255) NOT NULL,
  PRIMARY KEY (`SKID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `sms_details`;
CREATE TABLE `sms_details` (
  `id` int NOT NULL,
  `is_enable` tinyint(1) DEFAULT 0,
  `UserName` varchar(255) DEFAULT NULL,
  `Password` varchar(255) DEFAULT NULL,
  `Mask` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
DROP TABLE IF EXISTS `sms_log`;
CREATE TABLE `sms_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `phone_number` varchar(20) NOT NULL,
  `message` text NOT NULL,
  `sent_at` datetime DEFAULT current_timestamp(),
  `status` varchar(50) NOT NULL,
  `error_message` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `stocktypes`;
CREATE TABLE `stocktypes` (
  `STID` int NOT NULL AUTO_INCREMENT,
  `StockTypeName` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`STID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `subcategories`;
CREATE TABLE `subcategories` (
  `SCID` int NOT NULL AUTO_INCREMENT,
  `SubCatNo` varchar(12) DEFAULT NULL,
  `SubCatName` varchar(60) DEFAULT NULL,
  `categories_CTID` int NOT NULL,
  PRIMARY KEY (`SCID`),
  KEY `fk_Subcategories_categories1_idx` (`categories_CTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `supcheq`;
CREATE TABLE `supcheq` (
  `SCQID` int NOT NULL AUTO_INCREMENT,
  `type` int NOT NULL DEFAULT 2 COMMENT '1=issued cheque\r\n2=received cheque',
  `chq_stat` int NOT NULL DEFAULT 1 COMMENT '0=inactive\r\n1=active\r\n2=transferred\r\n3=bounced cheque\r\n4=realized cheque',
  `chq_no` varchar(250) NOT NULL,
  `sup_SPID` int NOT NULL,
  `effectiveDate` date NOT NULL,
  `GRNHeader_GHID` int NOT NULL,
  `transferedFrom` int NOT NULL DEFAULT 0,
  `user_USID` int NOT NULL,
  `shop_SHID` int NOT NULL,
  `createdDate` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`SCQID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `supchqdetail`;
CREATE TABLE `supchqdetail` (
  `SCDID` int NOT NULL AUTO_INCREMENT,
  `bank` varchar(250) NOT NULL,
  `chqAmount` decimal(12,2) NOT NULL,
  `chqNo` varchar(25) NOT NULL,
  `chqDate` date NOT NULL,
  `GRNHeader_GHID` int NOT NULL,
  `SCQID` int NOT NULL,
  PRIMARY KEY (`SCDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `supcredittransactions`;
CREATE TABLE `supcredittransactions` (
  `SCTID` int NOT NULL AUTO_INCREMENT,
  `supcreditTransactionAmount` decimal(10,2) NOT NULL,
  `supcreditTransactionStat` tinyint NOT NULL DEFAULT 1,
  `grn_GHI` int NOT NULL,
  `paymethod_id` int NOT NULL,
  `createDate` date NOT NULL DEFAULT (CURRENT_DATE),
  `created_dateTime` datetime NOT NULL DEFAULT current_timestamp(),
  `CreditSupplier_SCID` int DEFAULT NULL,
  `supplier_id` int NOT NULL,
  PRIMARY KEY (`SCTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `supplierreturn`;
CREATE TABLE `supplierreturn` (
  `SRID` int NOT NULL AUTO_INCREMENT,
  `ReturnNo` varchar(12) DEFAULT NULL,
  `EffectiveDate` date DEFAULT NULL,
  `ReturnAmount` decimal(12,2) DEFAULT NULL,
  `ReturnStat` tinyint DEFAULT NULL,
  `Supplier_SPID` int DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  `user_USID` int NOT NULL,
  PRIMARY KEY (`SRID`) USING BTREE,
  KEY `fk_SupplierReturn_user1_idx` (`user_USID`),
  KEY `fk_SupplierReturn_shop1_idx` (`shop_SHID`),
  KEY `fk_SupplierID_idx` (`Supplier_SPID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `supplierreturndetails`;
CREATE TABLE `supplierreturndetails` (
  `SRDID` int NOT NULL AUTO_INCREMENT,
  `ReturnQty` int DEFAULT NULL,
  `UnitPurchasePrice` decimal(12,2) DEFAULT NULL,
  `InventoryID` int DEFAULT NULL,
  `ProductID` varchar(20) DEFAULT NULL,
  `Batch` varchar(50) DEFAULT NULL,
  `VariationID` int DEFAULT NULL,
  `ReturnStat` int DEFAULT NULL,
  `ReturnAmount` decimal(12,2) DEFAULT NULL,
  `supplierreturn_SRID` int DEFAULT NULL,
  PRIMARY KEY (`SRDID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `suppliers`;
CREATE TABLE `suppliers` (
  `SPID` int NOT NULL AUTO_INCREMENT,
  `SupplierNo` varchar(12) DEFAULT NULL,
  `Distributer` varchar(45) DEFAULT NULL,
  `SupplierName` varchar(90) DEFAULT NULL,
  `Contact` varchar(12) DEFAULT NULL,
  `SupplierStat` tinyint DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`SPID`),
  KEY `fk_Suppliers_shop2_idx` (`shop_SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `suppliertransactions`;
CREATE TABLE `suppliertransactions` (
  `TRID` int NOT NULL AUTO_INCREMENT,
  `TransferAmount` decimal(12,2) DEFAULT NULL,
  `TransactionStat` tinyint DEFAULT 1,
  `paymethod_PMID` int NOT NULL,
  `GRNHeader_GHID` int NOT NULL,
  `returnheader_id` int DEFAULT NULL,
  PRIMARY KEY (`TRID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
DROP TABLE IF EXISTS `sysfeatures`;
CREATE TABLE `sysfeatures` (
  `SFID` int NOT NULL AUTO_INCREMENT,
  `FeatureName` varchar(45) DEFAULT NULL,
  `SystemModules_SMID` int NOT NULL,
  PRIMARY KEY (`SFID`),
  KEY `fk_SysFeatures_SystemModules1_idx` (`SystemModules_SMID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `sysmodules`;
CREATE TABLE `sysmodules` (
  `SMID` int NOT NULL AUTO_INCREMENT,
  `ModuleName` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`SMID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `tbl_denomination`;
CREATE TABLE `tbl_denomination` (
  `id` int NOT NULL AUTO_INCREMENT,
  `CountDate` datetime DEFAULT NULL,
  `CounterType` varchar(10) DEFAULT NULL,
  `RS5000` int DEFAULT NULL,
  `RS1000` int DEFAULT NULL,
  `RS500` int DEFAULT NULL,
  `RS100` int DEFAULT NULL,
  `RS50` int DEFAULT NULL,
  `RS20` int DEFAULT NULL,
  `RS10` int DEFAULT NULL,
  `RS5` int DEFAULT NULL,
  `RS2` int DEFAULT NULL,
  `RS1` int DEFAULT NULL,
  `user_USID` int DEFAULT NULL,
  `counter_id` int DEFAULT NULL,
  `shop_SHID` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
DROP TABLE IF EXISTS `temp_grnupload`;
CREATE TABLE `temp_grnupload` (
  `upload_id` int NOT NULL AUTO_INCREMENT,
  `barcode` varchar(45) DEFAULT NULL,
  `itemname` varchar(45) DEFAULT NULL,
  `qty` decimal(12,3) DEFAULT NULL,
  `purchaseprice` decimal(12,2) DEFAULT NULL,
  `labelprice` decimal(12,2) DEFAULT NULL,
  `sellingprice` decimal(12,2) DEFAULT NULL,
  `mnfdate` date DEFAULT NULL,
  `expdate` date DEFAULT NULL,
  `section_id` int DEFAULT NULL,
  `rack_id` int DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `prod_stat` int DEFAULT NULL,
  `shop_id` int DEFAULT NULL,
  PRIMARY KEY (`upload_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
  `TRID` int NOT NULL AUTO_INCREMENT,
  `TransferAmount` decimal(12,2) DEFAULT NULL,
  `TransactionStat` tinyint DEFAULT 1,
  `paymethod_PMID` int NOT NULL,
  `InvoiceHeader_IHID` int NOT NULL,
  `returnheader_id` int DEFAULT NULL,
  PRIMARY KEY (`TRID`),
  KEY `fk_transactions_paymethod1_idx` (`paymethod_PMID`),
  KEY `fk_transactions_InvoiceHeader1_idx` (`InvoiceHeader_IHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `transferdetails`;
CREATE TABLE `transferdetails` (
  `TDID` int NOT NULL AUTO_INCREMENT,
  `TransferQty` decimal(12,3) DEFAULT NULL,
  `ReceivedQty` decimal(12,3) DEFAULT NULL,
  `UnitPurchasePrice` decimal(12,2) DEFAULT NULL,
  `UnitSellingPrice` decimal(12,2) DEFAULT NULL,
  `MnfDate` date DEFAULT NULL,
  `ExpDate` date DEFAULT NULL,
  `TransferTotalAmount` decimal(12,2) DEFAULT NULL,
  `InventoryID` int DEFAULT NULL,
  `products_PDID` int NOT NULL,
  `VariationID` int DEFAULT NULL,
  `RackID` int DEFAULT NULL,
  `TransferStat` int DEFAULT NULL,
  `TransferHeader_THID` int NOT NULL,
  `Batch_ID` varchar(12) DEFAULT NULL,
  PRIMARY KEY (`TDID`),
  KEY `fk_TransferDetails_products1_idx` (`products_PDID`),
  KEY `fk_TransferDetails_TransferHeader1_idx` (`TransferHeader_THID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `transferheader`;
CREATE TABLE `transferheader` (
  `THID` int NOT NULL AUTO_INCREMENT,
  `TransferNo` varchar(12) DEFAULT NULL,
  `EffectiveDate` date DEFAULT NULL,
  `TransferFrom` int DEFAULT NULL COMMENT 'Transfer from shop id',
  `TransferTo` int DEFAULT NULL COMMENT 'Transfer to shop id',
  `TransferTotalCount` int DEFAULT NULL,
  `TransferTotalAmount` decimal(12,2) DEFAULT NULL,
  `TransferStat` int DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  `user_USID` smallint unsigned NOT NULL DEFAULT 0,
  PRIMARY KEY (`THID`),
  KEY `fk_TransferHeader_shop1_idx` (`shop_SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `transfertransactions`;
CREATE TABLE `transfertransactions` (
  `TTID` int NOT NULL AUTO_INCREMENT,
  `TrnTransactionAmount` decimal(10,2) NOT NULL,
  `TrnTransactionStat` tinyint NOT NULL,
  `transfer_header_id` int DEFAULT NULL,
  `paymethod_id` int DEFAULT NULL,
  PRIMARY KEY (`TTID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `units`;
CREATE TABLE `units` (
  `UNID` int NOT NULL AUTO_INCREMENT,
  `UnitName` varchar(60) DEFAULT NULL,
  `ShortName` varchar(10) DEFAULT NULL,
  `shop_SHID` int NOT NULL,
  PRIMARY KEY (`UNID`),
  KEY `fk_units_shop1_idx` (`shop_SHID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `USID` int NOT NULL AUTO_INCREMENT,
  `UserProfile` varchar(255) DEFAULT NULL,
  `UserName` varchar(45) DEFAULT NULL,
  `UserEmail` varchar(150) DEFAULT NULL,
  `ContactNo` varchar(12) DEFAULT NULL,
  `UserPwd` varchar(255) DEFAULT NULL,
  `PwdChange` varchar(255) DEFAULT NULL,
  `UserStat` tinyint DEFAULT 1,
  `UserRoles_URID` int NOT NULL,
  `UserType` int NOT NULL DEFAULT 0,
  `paylimit` decimal(12,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`USID`),
  KEY `fk_user_UserRoles1_idx` (`UserRoles_URID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `userlog`;
CREATE TABLE `userlog` (
  `ULID` int NOT NULL AUTO_INCREMENT,
  `logStart` datetime DEFAULT NULL,
  `logEnd` datetime DEFAULT NULL,
  `logStat` tinyint DEFAULT NULL,
  `user_USID` int NOT NULL,
  PRIMARY KEY (`ULID`),
  KEY `fk_userlog_user1_idx` (`user_USID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `usermoduleaccess`;
CREATE TABLE `usermoduleaccess` (
  `MAID` int NOT NULL AUTO_INCREMENT,
  `SysModules_SMID` int NOT NULL,
  `UserRoles_URID` int DEFAULT NULL,
  PRIMARY KEY (`MAID`),
  KEY `fk_UserModuleAccess_SysModules1_idx` (`SysModules_SMID`),
  KEY `UserRoles_URID` (`UserRoles_URID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `userremarks`;
CREATE TABLE `userremarks` (
  `URSID` int NOT NULL AUTO_INCREMENT,
  `user_UID` int NOT NULL,
  `Remarks` text NOT NULL,
  `Addedby` int NOT NULL,
  `date` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`URSID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `userroleaccess`;
CREATE TABLE `userroleaccess` (
  `RAID` int NOT NULL AUTO_INCREMENT,
  `is_create` tinyint DEFAULT NULL,
  `is_edit` tinyint DEFAULT NULL,
  `is_view` tinyint DEFAULT NULL,
  `is_delete` tinyint DEFAULT NULL,
  `is_verify` tinyint DEFAULT NULL,
  `is_print` tinyint DEFAULT NULL,
  `UserRolls_URID` int NOT NULL,
  `SysFeatures_SFID` int NOT NULL,
  PRIMARY KEY (`RAID`),
  KEY `fk_UserRoleAccess_UserRolls1_idx` (`UserRolls_URID`),
  KEY `fk_UserRoleAccess_SysFeatures1_idx` (`SysFeatures_SFID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `userroles`;
CREATE TABLE `userroles` (
  `URID` int NOT NULL AUTO_INCREMENT,
  `UserRoleName` varchar(60) DEFAULT NULL,
  `ur_status` tinyint(1) NOT NULL DEFAULT 1,
  `added_by` int NOT NULL,
  `user_ip` varchar(25) DEFAULT NULL,
  PRIMARY KEY (`URID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS `variations`;
CREATE TABLE `variations` (
  `VRID` int NOT NULL AUTO_INCREMENT,
  `VariationName` varchar(45) DEFAULT NULL,
  `products_PDID` int NOT NULL,
  PRIMARY KEY (`VRID`),
  KEY `fk_variations_products1_idx` (`products_PDID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



-- inventory_consumption.quantity is a quantity, not money: it needs the same
-- three decimal places the rest of the schema uses for quantities.
ALTER TABLE `inventory_consumption`
  MODIFY COLUMN `quantity` decimal(12,3) DEFAULT NULL;

SET FOREIGN_KEY_CHECKS = 1;
