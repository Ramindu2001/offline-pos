package lk.com.synsoft.offlinepos.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import lk.com.synsoft.offlinepos.dto.auth.CompanyProfile;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;

/**
 * The {@code shop} and {@code company} rows behind the session, and the
 * {@code shopusers} links that decide which shops a user may open.
 */
public interface ShopDao {

    /**
     * The shops this user is linked to, active ones only, in name order.
     *
     * A super admin is not automatically linked to every shop: the legacy system
     * reads the same {@code shopusers} rows for both user types, so an admin
     * whose links were never created has no shop, and the login says so rather
     * than silently opening one.
     */
    List<ShopProfile> findForUser(Connection connection, int userId) throws SQLException;

    Optional<ShopProfile> findById(Connection connection, int shopId) throws SQLException;

    /** Whether this user is linked to this shop. The check behind shop scope. */
    boolean isLinkedToUser(Connection connection, int shopId, int userId) throws SQLException;

    Optional<CompanyProfile> findCompany(Connection connection, int companyId) throws SQLException;
}
