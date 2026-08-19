-- ===========================================================================
-- V2  integrity constraints
--
-- The live schema declares 37 foreign keys across 87 tables. Every other
-- relationship exists only as an assumption inside PHP, which is how the live
-- database ended up holding 29 selldetail rows whose header no longer exists.
-- This migration declares the rest.
--
-- Delete policy, applied deliberately rather than uniformly:
--
--   CASCADE   document lines. A line has no life of its own, so removing the
--             header must remove them. This is the structural fix for orphan
--             detail rows - the database now refuses to leave them behind
--             whatever the application does.
--
--   RESTRICT  master data. A product that has been sold, a user who has rung
--             up a bill, or a shop with history must not be deletable. The
--             app is expected to deactivate instead.
--
--   SET NULL  optional cross-references, where losing the link is survivable
--             and the column already allows NULL.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 0. A type mismatch that blocks its own foreign key
--
-- transferheader.user_USID is smallint unsigned; user.USID is int. Besides
-- capping the table at 65,535 users, the mismatch makes the constraint
-- impossible to declare, which is presumably why it was never added.
-- ---------------------------------------------------------------------------
ALTER TABLE `transferheader`
  MODIFY COLUMN `user_USID` int NOT NULL;

-- ---------------------------------------------------------------------------
-- 0b. Magic zero instead of NULL
--
-- These columns are optional in real life and mandatory in the schema, so the
-- legacy app stores 0 to mean "none": no cash counter, no salesman, no named
-- customer. A 0 that points at no row is precisely why these foreign keys
-- could never be declared - every such row would violate them.
--
-- The React till already sends null for both (CashCounter_CCID: counter?.CCID
-- ?? null), so making the column match how the app actually behaves costs
-- nothing and lets the constraint exist.
-- ---------------------------------------------------------------------------
ALTER TABLE `invoiceheader`
  MODIFY COLUMN `CashCounter_CCID` int DEFAULT NULL,
  MODIFY COLUMN `Salesmans_SLID`   int DEFAULT NULL;

ALTER TABLE `return_invoice_header`
  MODIFY COLUMN `CashCounter_CCID` int DEFAULT NULL,
  MODIFY COLUMN `Customer_CTID`    int DEFAULT NULL;

ALTER TABLE `hold_invoice`
  MODIFY COLUMN `CashCounter_CCID` int DEFAULT NULL,
  MODIFY COLUMN `Salesmans_SLID`   int DEFAULT NULL;

-- ---------------------------------------------------------------------------
-- 1. Tenant and setup
-- ---------------------------------------------------------------------------
ALTER TABLE `shop`
  ADD CONSTRAINT `fk_shop_company`    FOREIGN KEY (`Company_CMID`)    REFERENCES `company` (`CMID`)    ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_shop_stocktypes` FOREIGN KEY (`StockTypes_STID`) REFERENCES `stocktypes` (`STID`) ON DELETE RESTRICT;

ALTER TABLE `shopusers`
  ADD CONSTRAINT `fk_shopusers_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_shopusers_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE CASCADE;

ALTER TABLE `shoppermissions`
  ADD CONSTRAINT `fk_shoppermissions_shop` FOREIGN KEY (`Shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE CASCADE;

ALTER TABLE `shoppaymethod`
  ADD CONSTRAINT `fk_shoppaymethod_shop`      FOREIGN KEY (`shop_SHID`)      REFERENCES `shop` (`SHID`)          ON DELETE CASCADE,
  ADD CONSTRAINT `fk_shoppaymethod_paymethod` FOREIGN KEY (`paymethod_PMID`) REFERENCES `paymethod` (`PMID`)     ON DELETE RESTRICT;

-- ---------------------------------------------------------------------------
-- 2. Security
--
-- userroleaccess.UserRolls_URID is misspelled in the source schema. It is left
-- as-is: this migration is a faithful port plus the corrections listed in V1,
-- and renaming columns is a larger decision than renaming one table was.
-- ---------------------------------------------------------------------------
ALTER TABLE `user`
  ADD CONSTRAINT `fk_user_userroles` FOREIGN KEY (`UserRoles_URID`) REFERENCES `userroles` (`URID`) ON DELETE RESTRICT;

ALTER TABLE `userroleaccess`
  ADD CONSTRAINT `fk_userroleaccess_userroles`   FOREIGN KEY (`UserRolls_URID`)   REFERENCES `userroles` (`URID`)    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_userroleaccess_sysfeatures` FOREIGN KEY (`SysFeatures_SFID`) REFERENCES `sysfeatures` (`SFID`)  ON DELETE CASCADE;

-- Without this, sysfeatures rows can point at a module that does not exist -
-- which is exactly the state the live data is in: SFID 70 "Test Feature"
-- belongs to a module 6 that was deleted.
ALTER TABLE `sysfeatures`
  ADD CONSTRAINT `fk_sysfeatures_sysmodules` FOREIGN KEY (`SystemModules_SMID`) REFERENCES `sysmodules` (`SMID`) ON DELETE RESTRICT;

ALTER TABLE `usermoduleaccess`
  ADD CONSTRAINT `fk_usermoduleaccess_userroles`  FOREIGN KEY (`UserRoles_URID`)  REFERENCES `userroles` (`URID`)   ON DELETE CASCADE,
  ADD CONSTRAINT `fk_usermoduleaccess_sysmodules` FOREIGN KEY (`SysModules_SMID`) REFERENCES `sysmodules` (`SMID`)  ON DELETE CASCADE;

ALTER TABLE `userlog`
  ADD CONSTRAINT `fk_userlog_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 3. Catalogue
-- ---------------------------------------------------------------------------
ALTER TABLE `subcategories`
  ADD CONSTRAINT `fk_subcategories_categories` FOREIGN KEY (`categories_CTID`) REFERENCES `categories` (`CTID`) ON DELETE RESTRICT;

ALTER TABLE `products`
  ADD CONSTRAINT `fk_products_shop`          FOREIGN KEY (`shop_SHID`)          REFERENCES `shop` (`SHID`)             ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_products_subcategories` FOREIGN KEY (`Subcategories_SCID`) REFERENCES `subcategories` (`SCID`)    ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_products_user`          FOREIGN KEY (`user_USID`)          REFERENCES `user` (`USID`)             ON DELETE RESTRICT;

ALTER TABLE `variations`
  ADD CONSTRAINT `fk_variations_products` FOREIGN KEY (`products_PDID`) REFERENCES `products` (`PDID`) ON DELETE CASCADE;

ALTER TABLE `units`
  ADD CONSTRAINT `fk_units_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT;

ALTER TABLE `sections`
  ADD CONSTRAINT `fk_sections_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT;

ALTER TABLE `rack`
  ADD CONSTRAINT `fk_rack_sections` FOREIGN KEY (`Sections_SEID`) REFERENCES `sections` (`SEID`) ON DELETE RESTRICT;

-- ---------------------------------------------------------------------------
-- 4. Stock
-- ---------------------------------------------------------------------------
ALTER TABLE `pricehistory`
  ADD CONSTRAINT `fk_pricehistory_inventory` FOREIGN KEY (`Inventory_INID`) REFERENCES `inventory` (`INID`) ON DELETE CASCADE;

ALTER TABLE `inventory_consumption`
  ADD CONSTRAINT `fk_invconsumption_inventory` FOREIGN KEY (`inventory_INID`) REFERENCES `inventory` (`INID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_invconsumption_shop`      FOREIGN KEY (`shop_SHID`)      REFERENCES `shop` (`SHID`)      ON DELETE RESTRICT;

-- ---------------------------------------------------------------------------
-- 5. Purchasing
-- ---------------------------------------------------------------------------
ALTER TABLE `suppliers`
  ADD CONSTRAINT `fk_suppliers_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT;

ALTER TABLE `creditsupplier`
  ADD CONSTRAINT `fk_creditsupplier_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_creditsupplier_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE RESTRICT;

ALTER TABLE `supcredittransactions`
  ADD CONSTRAINT `fk_supcredittrans_creditsupplier` FOREIGN KEY (`CreditSupplier_SCID`) REFERENCES `creditsupplier` (`SCID`) ON DELETE CASCADE;

ALTER TABLE `suppliertransactions`
  ADD CONSTRAINT `fk_suppliertrans_grnheader` FOREIGN KEY (`GRNHeader_GHID`) REFERENCES `grnheader` (`GHID`)  ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_suppliertrans_paymethod` FOREIGN KEY (`paymethod_PMID`) REFERENCES `paymethod` (`PMID`)  ON DELETE RESTRICT;

ALTER TABLE `supcheq`
  ADD CONSTRAINT `fk_supcheq_grnheader` FOREIGN KEY (`GRNHeader_GHID`) REFERENCES `grnheader` (`GHID`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_supcheq_shop`      FOREIGN KEY (`shop_SHID`)      REFERENCES `shop` (`SHID`)      ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_supcheq_user`      FOREIGN KEY (`user_USID`)      REFERENCES `user` (`USID`)      ON DELETE RESTRICT;

ALTER TABLE `supchqdetail`
  ADD CONSTRAINT `fk_supchqdetail_supcheq`   FOREIGN KEY (`SCQID`)          REFERENCES `supcheq` (`SCQID`)   ON DELETE CASCADE,
  ADD CONSTRAINT `fk_supchqdetail_grnheader` FOREIGN KEY (`GRNHeader_GHID`) REFERENCES `grnheader` (`GHID`)  ON DELETE RESTRICT;

ALTER TABLE `supplierreturn`
  ADD CONSTRAINT `fk_supplierreturn_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_supplierreturn_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE RESTRICT;

ALTER TABLE `supplierreturndetails`
  ADD CONSTRAINT `fk_supplierreturndetails_header` FOREIGN KEY (`supplierreturn_SRID`) REFERENCES `supplierreturn` (`SRID`) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 6. Sales
--
-- invoicedetails -> invoiceheader is the single most important line in this
-- file. Without it a bill can lose its header and leave its lines behind,
-- which is exactly what happened to selldetail in the live data.
-- ---------------------------------------------------------------------------
ALTER TABLE `invoiceheader`
  ADD CONSTRAINT `fk_invoiceheader_cashcounter` FOREIGN KEY (`CashCounter_CCID`) REFERENCES `cashcounter` (`CCID`)   ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_invoiceheader_holdinvoice` FOREIGN KEY (`HIID`)             REFERENCES `hold_invoice` (`HIID`)  ON DELETE SET NULL;

ALTER TABLE `invoicedetails`
  ADD CONSTRAINT `fk_invoicedetails_invoiceheader` FOREIGN KEY (`InvoiceHeader_IHID`) REFERENCES `invoiceheader` (`IHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_invoicedetails_inventory`     FOREIGN KEY (`Inventory_INID`)     REFERENCES `inventory` (`INID`)     ON DELETE RESTRICT;

ALTER TABLE `invoice_remarks`
  ADD CONSTRAINT `fk_invoiceremarks_invoiceheader` FOREIGN KEY (`invoiceheader_IHID`) REFERENCES `invoiceheader` (`IHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_invoiceremarks_user`          FOREIGN KEY (`user_USID`)          REFERENCES `user` (`USID`)          ON DELETE RESTRICT;

ALTER TABLE `transactions`
  ADD CONSTRAINT `fk_transactions_invoiceheader` FOREIGN KEY (`InvoiceHeader_IHID`) REFERENCES `invoiceheader` (`IHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_transactions_paymethod`     FOREIGN KEY (`paymethod_PMID`)     REFERENCES `paymethod` (`PMID`)     ON DELETE RESTRICT;

ALTER TABLE `sellheader`
  ADD CONSTRAINT `fk_sellheader_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`SHID`) ON DELETE CASCADE;

ALTER TABLE `selldetail`
  ADD CONSTRAINT `fk_selldetail_sellheader` FOREIGN KEY (`sellHeader_SHID`) REFERENCES `sellheader` (`SHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_selldetail_products`   FOREIGN KEY (`products_PDID`)   REFERENCES `products` (`PDID`)   ON DELETE RESTRICT;

ALTER TABLE `hold_invoice`
  ADD CONSTRAINT `fk_holdinvoice_shop`        FOREIGN KEY (`shop_SHID`)        REFERENCES `shop` (`SHID`)         ON DELETE CASCADE,
  ADD CONSTRAINT `fk_holdinvoice_user`        FOREIGN KEY (`user_USID`)        REFERENCES `user` (`USID`)         ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_holdinvoice_customers`   FOREIGN KEY (`customers_CTID`)   REFERENCES `customers` (`CTID`)    ON DELETE SET NULL,
  ADD CONSTRAINT `fk_holdinvoice_salesmans`   FOREIGN KEY (`Salesmans_SLID`)   REFERENCES `salesmans` (`SLID`)    ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_holdinvoice_cashcounter` FOREIGN KEY (`CashCounter_CCID`) REFERENCES `cashcounter` (`CCID`)  ON DELETE RESTRICT;

ALTER TABLE `salesmans`
  ADD CONSTRAINT `fk_salesmans_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT;

-- ---------------------------------------------------------------------------
-- 7. Returns and customer credit
-- ---------------------------------------------------------------------------
ALTER TABLE `return_invoice_header`
  ADD CONSTRAINT `fk_returnheader_shop`        FOREIGN KEY (`shopID`)           REFERENCES `shop` (`SHID`)          ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_returnheader_invoice`     FOREIGN KEY (`IHID`)             REFERENCES `invoiceheader` (`IHID`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_returnheader_customer`    FOREIGN KEY (`Customer_CTID`)    REFERENCES `customers` (`CTID`)     ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_returnheader_user`        FOREIGN KEY (`returnby`)         REFERENCES `user` (`USID`)          ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_returnheader_cashcounter` FOREIGN KEY (`CashCounter_CCID`) REFERENCES `cashcounter` (`CCID`)   ON DELETE RESTRICT;

ALTER TABLE `returndetails`
  ADD CONSTRAINT `fk_returndetails_header`         FOREIGN KEY (`ReturnHeader_RHID`)  REFERENCES `return_invoice_header` (`RIHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_returndetails_products`       FOREIGN KEY (`products_PDID`)      REFERENCES `products` (`PDID`)               ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_returndetails_inventory`      FOREIGN KEY (`inventory_INID`)     REFERENCES `inventory` (`INID`)              ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_returndetails_invoicedetails` FOREIGN KEY (`InvoiceDetails_IDID`) REFERENCES `invoicedetails` (`IDID`)        ON DELETE SET NULL;

ALTER TABLE `creditcustomer`
  ADD CONSTRAINT `fk_creditcustomer_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT;

ALTER TABLE `cuscredittransactions`
  ADD CONSTRAINT `fk_cuscredittrans_creditcustomer` FOREIGN KEY (`CreditCustomer_CCID`) REFERENCES `creditcustomer` (`CCID`) ON DELETE CASCADE;

ALTER TABLE `custcheq`
  ADD CONSTRAINT `fk_custcheq_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_custcheq_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE RESTRICT;

ALTER TABLE `custchqdetail`
  ADD CONSTRAINT `fk_custchqdetail_custcheq` FOREIGN KEY (`CCQID`) REFERENCES `custcheq` (`CCQID`) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 8. Stock movement
-- ---------------------------------------------------------------------------
ALTER TABLE `transferheader`
  ADD CONSTRAINT `fk_transferheader_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE RESTRICT,
  ADD CONSTRAINT `fk_transferheader_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE RESTRICT;

ALTER TABLE `transferdetails`
  ADD CONSTRAINT `fk_transferdetails_header`   FOREIGN KEY (`TransferHeader_THID`) REFERENCES `transferheader` (`THID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_transferdetails_products` FOREIGN KEY (`products_PDID`)       REFERENCES `products` (`PDID`)       ON DELETE RESTRICT;

-- ---------------------------------------------------------------------------
-- 9. Misc
-- ---------------------------------------------------------------------------
ALTER TABLE `prescriptionheader`
  ADD CONSTRAINT `fk_prescriptionheader_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE RESTRICT;

ALTER TABLE `tbl_denomination`
  ADD CONSTRAINT `fk_denomination_shop` FOREIGN KEY (`shop_SHID`) REFERENCES `shop` (`SHID`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_denomination_user` FOREIGN KEY (`user_USID`) REFERENCES `user` (`USID`) ON DELETE RESTRICT;


-- ===========================================================================
-- 10. Uniqueness on document numbers
--
-- This is the half of defect D02 the application cannot get wrong. The legacy
-- till reads the next number, then writes it back, with no lock and no
-- transaction, so two cashiers billing at the same moment get the same number.
-- Phase 8 allocates inside the sale transaction with SELECT ... FOR UPDATE;
-- these constraints mean that if that ever regresses, the second INSERT fails
-- loudly instead of quietly duplicating a bill number.
--
-- docno gets a unique shop_id because the legacy read is
-- "ORDER BY DNID DESC LIMIT 1" over a table that permits several rows per
-- shop - which is a second, quieter way to hand out the same number twice.
-- ===========================================================================
ALTER TABLE `docno`
  ADD CONSTRAINT `uq_docno_shop` UNIQUE (`shop_id`);

ALTER TABLE `invoiceheader`
  ADD CONSTRAINT `uq_invoiceheader_shop_billno`  UNIQUE (`shop_SHID`, `BillNo`),
  ADD CONSTRAINT `uq_invoiceheader_shop_invno`   UNIQUE (`shop_SHID`, `InvoiceNo`);

ALTER TABLE `grnheader`
  ADD CONSTRAINT `uq_grnheader_shop_no` UNIQUE (`shop_SHID`, `GRNHeaderNo`);

ALTER TABLE `transferheader`
  ADD CONSTRAINT `uq_transferheader_shop_no` UNIQUE (`shop_SHID`, `TransferNo`);

ALTER TABLE `adjustheader`
  ADD CONSTRAINT `uq_adjustheader_shop_no` UNIQUE (`shop_SHID`, `AdjustNo`);

ALTER TABLE `return_invoice_header`
  ADD CONSTRAINT `uq_returnheader_shop_no` UNIQUE (`shopID`, `return_no`);

ALTER TABLE `supplierreturn`
  ADD CONSTRAINT `uq_supplierreturn_shop_no` UNIQUE (`shop_SHID`, `ReturnNo`);


-- ===========================================================================
-- 11. Indexes for the paths reports and the till actually take
--
-- Barcode is indexed but NOT unique: shops legitimately carry unbarcoded
-- items, and an empty string would collide where NULL would not. Uniqueness
-- there is a business rule, enforced in the service where it can produce a
-- sentence a cashier understands.
-- ===========================================================================
CREATE INDEX `ix_products_barcode`        ON `products` (`Barcode`);
CREATE INDEX `ix_products_shop_name`      ON `products` (`shop_SHID`, `ItemName`(64));

CREATE INDEX `ix_inventory_shop_product`  ON `inventory` (`shop_SHID`, `products_PDID`);

CREATE INDEX `ix_invoiceheader_shop_date` ON `invoiceheader` (`shop_SHID`, `EffectiveDate`);
CREATE INDEX `ix_invoiceheader_shop_stat` ON `invoiceheader` (`shop_SHID`, `InvStat`, `EffectiveDate`);
CREATE INDEX `ix_invoicedetails_product`  ON `invoicedetails` (`products_PDID`);

CREATE INDEX `ix_grnheader_shop_date`     ON `grnheader` (`shop_SHID`, `EffectiveDate`);
CREATE INDEX `ix_returnheader_shop_date`  ON `return_invoice_header` (`shopID`, `EffectiveDate`);

CREATE INDEX `ix_creditcustomer_customer` ON `creditcustomer` (`Customers_CTID`, `EffectiveDate`);
CREATE INDEX `ix_creditsupplier_supplier` ON `creditsupplier` (`Supplier_ID`, `EffectiveDate`);

CREATE INDEX `ix_customers_shop_name`     ON `customers` (`shop_SHID`, `CustName`);
CREATE INDEX `ix_suppliers_shop_name`     ON `suppliers` (`shop_SHID`, `SupplierName`);
