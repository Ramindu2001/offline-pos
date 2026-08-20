package lk.com.synsoft.offlinepos.dto.auth;

import java.math.BigDecimal;

/**
 * The signed-in user, as the rest of the application sees them.
 *
 * There is no password field and there never should be. The hash is read inside
 * {@code UserDaoImpl}, compared there, and does not leave the DAO - so it cannot
 * end up in a log line, a DTO dump or a crash report.
 *
 * @param payLimit the largest discount or refund this user may authorise;
 *                 0 means no limit is configured, not a limit of nothing
 */
public record AuthenticatedUser(
        int id,
        String userName,
        String email,
        String contactNo,
        UserType type,
        int roleId,
        String roleName,
        BigDecimal payLimit) {

    public boolean isSuperAdmin() {
        return type == UserType.SUPER_ADMIN;
    }

    /** For the header, the receipt footer and the user log. */
    public String displayName() {
        return userName == null || userName.isBlank() ? "user " + id : userName;
    }
}
