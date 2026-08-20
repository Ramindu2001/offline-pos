package lk.com.synsoft.offlinepos.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser;

/**
 * The {@code user} table and the role it belongs to.
 *
 * The password hash is deliberately absent from every return type here. It is
 * read inside {@link #verifyPassword} and compared there, so it never reaches a
 * DTO, a log line or a stack trace.
 */
public interface UserDao {

    /**
     * The account with this user name, if there is one and only one.
     *
     * {@code UserName} has no unique constraint in the legacy schema, so two
     * accounts could in principle share one. queryOne refuses that rather than
     * signing in whichever came back first.
     */
    Optional<AuthenticatedUser> findByUserName(Connection connection, String userName) throws SQLException;

    Optional<AuthenticatedUser> findById(Connection connection, int userId) throws SQLException;

    /** Whether the account is switched on. Gate 3. */
    boolean isActive(Connection connection, int userId) throws SQLException;

    /** Whether the role this account belongs to is switched on. */
    boolean isRoleActive(Connection connection, int roleId) throws SQLException;

    /**
     * Compares a password against the stored hash.
     *
     * @param password cleared by the caller, not by this method
     */
    boolean verifyPassword(Connection connection, int userId, char[] password) throws SQLException;

    /** Whether the stored value is a bcrypt hash this build can read at all. */
    boolean hasUsableHash(Connection connection, int userId) throws SQLException;

    /** Replaces the hash. The caller has already checked who is allowed to do this. */
    void updatePassword(Connection connection, int userId, String hash) throws SQLException;
}
