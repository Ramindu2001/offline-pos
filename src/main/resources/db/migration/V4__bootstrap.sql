-- ===========================================================================
-- V4  bootstrap
--
-- The minimum a fresh install needs to reach a login screen and get past it:
-- one company, one shop, one role, one administrator.
--
-- Phase 16 replaces this with a first-run wizard that asks for the real
-- details. Until then these values are what the app starts with, and the
-- Settings screens built in Phase 6 can edit every one of them.
--
-- The shop feature flags below are set to a plain retail configuration.
-- They are the 25 is_* switches the React app reads through SHOP_FLAG, and
-- they decide which parts of the UI exist at all - so this row is effectively
-- the app's default feature set.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Company. Licence runs for a year from install.
--
-- ComExpireDate is treated as the LAST VALID DAY, not the first invalid one.
-- The legacy check was `$today < ComExpireDate` on strings, which locked the
-- shop out a day early (defect D12).
-- ---------------------------------------------------------------------------
INSERT INTO `company`
  (`CMID`, `CompanyNo`, `ComName`, `CompanyLocation`, `LicenceNo`, `VersionNo`,
   `ComStartDate`, `ComExpireDate`, `ComStat`, `is_multicategory`, `is_commonStock`,
   `CompanyType_CTID`, `last_updateDate`)
VALUES
  (1, 'CM_000001', 'My Company', 'Sri Lanka', 'OFFLINE-0001', '1.0',
   CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 1 YEAR), 1, 0, 0,
   1, CURRENT_DATE);

-- ---------------------------------------------------------------------------
-- Shop, with a plain retail feature set.
-- ---------------------------------------------------------------------------
INSERT INTO `shop`
  (`SHID`, `ShopNo`, `ShopName`, `emailAddress`, `Company_CMID`, `StockTypes_STID`, `ShopStat`,
   `RetailShop`, `WholesaleShop`,
   `is_inventory`, `is_minus`, `is_category`, `is_expire`, `is_variation`,
   `is_suppliers`, `is_service`, `is_salesman`, `is_expenses`, `is_customers`,
   `is_fixedprice`, `is_carton`, `is_warranty`, `is_promotions`, `is_secondlan`,
   `is_labelprice`, `is_quotation`, `is_racks`, `is_credit`, `is_prescription`,
   `is_counter`, `is_excessAmount`, `is_BatchNo`, `is_under_cost`, `invoice_print`)
VALUES
  (1, 'SH_000001', 'Main Shop', 'shop@localhost', 1, 1, 1,
   1, 0,
   1, 0, 1, 0, 0,
   1, 0, 0, 1, 1,
   1, 0, 0, 0, 0,
   0, 0, 0, 1, 0,
   0, 0, 0, 0, 1);

-- ---------------------------------------------------------------------------
-- Administrator role and user.
--
-- UserType 1 is the super admin who skips every permission check, so this
-- account needs no userroleaccess rows to be able to set the system up.
-- Ordinary roles are created in the app and DO need them.
--
-- Password: admin123
-- The hash is bcrypt in PHP's $2y$ format, which is what the Cloud POS
-- password_hash() already produces, so hashes are interchangeable between the
-- two systems. CHANGE THIS ON FIRST LOGIN - Phase 3 will force it.
-- ---------------------------------------------------------------------------
INSERT INTO `userroles` (`URID`, `UserRoleName`, `ur_status`, `added_by`, `user_ip`)
VALUES (1, 'Administrator', 1, 1, '127.0.0.1');

INSERT INTO `user`
  (`USID`, `UserName`, `UserEmail`, `ContactNo`, `UserPwd`,
   `UserStat`, `UserRoles_URID`, `UserType`, `paylimit`)
VALUES
  (1, 'admin', 'admin@localhost', '', '$2y$10$N2EQvVR0j5gxkRSxM5AyVeOJppYs/cICIZTKWz2npGxf6RdNrlyiK',
   1, 1, 1, 0.00);

INSERT INTO `shopusers` (`shop_SHID`, `user_USID`) VALUES (1, 1);

-- ---------------------------------------------------------------------------
-- Document number counter for the shop.
--
-- One row per shop, now enforced by uq_docno_shop from V2. Phase 8 reads and
-- increments this inside the sale transaction with SELECT ... FOR UPDATE,
-- which is the other half of the fix for duplicate bill numbers (D02).
-- ---------------------------------------------------------------------------
INSERT INTO `docno` (`shop_id`, `tmp_no`, `org_no`, `ws_no`) VALUES (1, 0, 0, 0);
