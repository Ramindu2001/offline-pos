package lk.com.synsoft.offlinepos.dao.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lk.com.synsoft.offlinepos.dao.ShopDao;
import lk.com.synsoft.offlinepos.db.BaseDao;
import lk.com.synsoft.offlinepos.db.RowMapper;
import lk.com.synsoft.offlinepos.db.Rows;
import lk.com.synsoft.offlinepos.dto.auth.CompanyProfile;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;

public final class ShopDaoImpl extends BaseDao implements ShopDao {

    /**
     * Every column the session needs, written out rather than SELECT *.
     *
     * The 25 flag columns are named explicitly so adding one to the schema is a
     * compile-time change here rather than a silently missing switch.
     */
    private static final String SELECT_SHOP = """
            SELECT s.SHID, s.ShopNo, s.ShopName, s.AddressLineOne, s.AddressLineTwo, s.City,
                   s.PhoneNumber, s.emailAddress, s.Company_CMID, s.RetailShop, s.WholesaleShop,
                   s.ShopStat,
                   s.is_inventory, s.is_minus, s.is_category, s.is_expire, s.is_variation,
                   s.is_suppliers, s.is_service, s.is_salesman, s.is_expenses, s.is_customers,
                   s.is_fixedprice, s.is_carton, s.is_warranty, s.is_promotions, s.is_secondlan,
                   s.is_labelprice, s.is_quotation, s.is_racks, s.is_credit, s.is_prescription,
                   s.is_counter, s.is_excessAmount, s.is_BatchNo, s.is_under_cost, s.invoice_print
              FROM shop s
            """;

    private static final RowMapper<ShopProfile> SHOP = ShopDaoImpl::toShop;

    @Override
    public List<ShopProfile> findForUser(Connection connection, int userId) throws SQLException {
        return queryList(connection,
                SELECT_SHOP + """
                          JOIN shopusers su ON su.shop_SHID = s.SHID
                         WHERE su.user_USID = ?
                           AND s.ShopStat = 1
                         ORDER BY s.ShopName
                        """,
                SHOP, userId);
    }

    @Override
    public Optional<ShopProfile> findById(Connection connection, int shopId) throws SQLException {
        return queryOne(connection, SELECT_SHOP + " WHERE s.SHID = ?", SHOP, shopId);
    }

    @Override
    public boolean isLinkedToUser(Connection connection, int shopId, int userId) throws SQLException {
        return exists(connection,
                "SELECT 1 FROM shopusers WHERE shop_SHID = ? AND user_USID = ? LIMIT 1",
                shopId, userId);
    }

    @Override
    public Optional<CompanyProfile> findCompany(Connection connection, int companyId) throws SQLException {
        return queryOne(connection, """
                SELECT CMID, CompanyNo, ComName, CompanyLocation, LicenceNo, VersionNo,
                       ComStartDate, ComExpireDate, ComStat, is_multicategory, is_commonStock
                  FROM company
                 WHERE CMID = ?
                """, ShopDaoImpl::toCompany, companyId);
    }

    private static ShopProfile toShop(ResultSet row) throws SQLException {
        Set<ShopFlag> flags = EnumSet.noneOf(ShopFlag.class);

        for (ShopFlag flag : ShopFlag.values()) {
            if (Rows.flag(row, flag.column())) {
                flags.add(flag);
            }
        }

        return new ShopProfile(
                Rows.integer(row, "SHID"),
                Rows.text(row, "ShopNo"),
                Rows.text(row, "ShopName"),
                Rows.text(row, "AddressLineOne"),
                Rows.text(row, "AddressLineTwo"),
                Rows.text(row, "City"),
                Rows.text(row, "PhoneNumber"),
                Rows.text(row, "emailAddress"),
                Rows.integer(row, "Company_CMID"),
                Rows.flag(row, "RetailShop"),
                Rows.flag(row, "WholesaleShop"),
                Rows.flag(row, "ShopStat"),
                flags);
    }

    private static CompanyProfile toCompany(ResultSet row) throws SQLException {
        return new CompanyProfile(
                Rows.integer(row, "CMID"),
                Rows.text(row, "CompanyNo"),
                Rows.text(row, "ComName"),
                Rows.text(row, "CompanyLocation"),
                Rows.text(row, "LicenceNo"),
                Rows.text(row, "VersionNo"),
                Rows.date(row, "ComStartDate"),
                Rows.date(row, "ComExpireDate"),
                Rows.flag(row, "ComStat"),
                Rows.flag(row, "is_multicategory"),
                Rows.flag(row, "is_commonStock"));
    }
}
