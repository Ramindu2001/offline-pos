package lk.com.synsoft.offlinepos.dao.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import lk.com.synsoft.offlinepos.dao.UserDao;
import lk.com.synsoft.offlinepos.db.BaseDao;
import lk.com.synsoft.offlinepos.db.RowMapper;
import lk.com.synsoft.offlinepos.db.Rows;
import lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser;
import lk.com.synsoft.offlinepos.dto.auth.UserType;
import lk.com.synsoft.offlinepos.util.Passwords;

public final class UserDaoImpl extends BaseDao implements UserDao {

    /**
     * The role name is joined in rather than looked up later: the header shows
     * it on every screen, and it is one round trip at login instead of one per
     * screen.
     */
    private static final String SELECT_USER = """
            SELECT u.USID, u.UserName, u.UserEmail, u.ContactNo, u.UserType,
                   u.UserRoles_URID, u.paylimit, r.UserRoleName
              FROM user u
              LEFT JOIN userroles r ON r.URID = u.UserRoles_URID
            """;

    private static final RowMapper<AuthenticatedUser> MAPPER = UserDaoImpl::toUser;

    @Override
    public Optional<AuthenticatedUser> findByUserName(Connection connection, String userName)
            throws SQLException {

        return queryOne(connection, SELECT_USER + " WHERE u.UserName = ?", MAPPER, userName);
    }

    @Override
    public Optional<AuthenticatedUser> findById(Connection connection, int userId) throws SQLException {
        return queryOne(connection, SELECT_USER + " WHERE u.USID = ?", MAPPER, userId);
    }

    @Override
    public boolean isActive(Connection connection, int userId) throws SQLException {
        return exists(connection,
                "SELECT 1 FROM user WHERE USID = ? AND UserStat = 1 LIMIT 1",
                userId);
    }

    @Override
    public boolean isRoleActive(Connection connection, int roleId) throws SQLException {
        return exists(connection,
                "SELECT 1 FROM userroles WHERE URID = ? AND ur_status = 1 LIMIT 1",
                roleId);
    }

    /**
     * Reads the hash, compares, and lets it go out of scope.
     *
     * This is the only method in the application that selects {@code UserPwd}.
     * Keeping it here rather than returning the hash to a service is what stops
     * it appearing in a DTO, a toString or a log line.
     */
    @Override
    public boolean verifyPassword(Connection connection, int userId, char[] password)
            throws SQLException {

        Optional<String> hash = queryOne(connection,
                "SELECT UserPwd FROM user WHERE USID = ?",
                row -> row.getString("UserPwd"),
                userId);

        return hash.filter(stored -> Passwords.verify(password, stored)).isPresent();
    }

    @Override
    public boolean hasUsableHash(Connection connection, int userId) throws SQLException {
        return queryOne(connection,
                "SELECT UserPwd FROM user WHERE USID = ?",
                row -> row.getString("UserPwd"),
                userId)
                .filter(Passwords::isSupportedHash)
                .isPresent();
    }

    @Override
    public void updatePassword(Connection connection, int userId, String hash) throws SQLException {
        // PwdChange records when it last changed; the legacy column holds a
        // string, so the format it already contains is kept.
        updateOne(connection, "Changing a password",
                "UPDATE user SET UserPwd = ?, PwdChange = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') WHERE USID = ?",
                hash, userId);
    }

    private static AuthenticatedUser toUser(ResultSet row) throws SQLException {
        return new AuthenticatedUser(
                Rows.integer(row, "USID"),
                Rows.text(row, "UserName"),
                Rows.text(row, "UserEmail"),
                Rows.text(row, "ContactNo"),
                UserType.fromCode(Rows.integer(row, "UserType")),
                Rows.integer(row, "UserRoles_URID"),
                Rows.text(row, "UserRoleName"),
                Rows.money(row, "paylimit"));
    }
}
