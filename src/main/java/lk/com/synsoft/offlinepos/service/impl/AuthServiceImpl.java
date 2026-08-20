package lk.com.synsoft.offlinepos.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lk.com.synsoft.offlinepos.dao.PermissionDao;
import lk.com.synsoft.offlinepos.dao.ShopDao;
import lk.com.synsoft.offlinepos.dao.UserDao;
import lk.com.synsoft.offlinepos.dao.UserLogDao;
import lk.com.synsoft.offlinepos.db.TransactionManager;
import lk.com.synsoft.offlinepos.dto.auth.Action;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Authentication;
import lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser;
import lk.com.synsoft.offlinepos.dto.auth.CompanyProfile;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.LicenceStatus;
import lk.com.synsoft.offlinepos.dto.auth.Rights;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.error.LoginFailedException;
import lk.com.synsoft.offlinepos.error.LoginFailedException.Reason;
import lk.com.synsoft.offlinepos.error.NotFoundException;
import lk.com.synsoft.offlinepos.error.PermissionDeniedException;
import lk.com.synsoft.offlinepos.error.ValidationException;
import lk.com.synsoft.offlinepos.service.AuthService;
import lk.com.synsoft.offlinepos.service.PermissionService;
import lk.com.synsoft.offlinepos.util.Passwords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The three gates, in the legacy order, minus the flaws.
 *
 * The clock is injected rather than read from the machine, so the licence
 * boundary can be tested on both sides of an expiry date instead of only on the
 * day the test happens to run. That boundary is defect D12 and it is worth one
 * constructor parameter.
 */
public final class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final TransactionManager transactions;
    private final UserDao userDao;
    private final ShopDao shopDao;
    private final PermissionDao permissionDao;
    private final UserLogDao userLogDao;
    private final PermissionService permissions;
    private final Clock clock;

    public AuthServiceImpl(TransactionManager transactions,
                           UserDao userDao,
                           ShopDao shopDao,
                           PermissionDao permissionDao,
                           UserLogDao userLogDao,
                           PermissionService permissions,
                           Clock clock) {

        this.transactions = transactions;
        this.userDao = userDao;
        this.shopDao = shopDao;
        this.permissionDao = permissionDao;
        this.userLogDao = userLogDao;
        this.permissions = permissions;
        this.clock = clock;
    }

    // ==================================================================
    // step one: who are you
    // ==================================================================

    @Override
    public Authentication authenticate(String userName, char[] password) throws LoginFailedException {
        if (userName == null || userName.isBlank() || password == null || password.length == 0) {
            throw refuse(Reason.BAD_CREDENTIALS, "Login attempted with an empty name or password.");
        }

        try {
            return transactions.<Authentication, LoginFailedException>inReadOnly(connection -> {

                Optional<AuthenticatedUser> found =
                        userDao.findByUserName(connection, userName.trim());

                if (found.isEmpty()) {
                    throw refuse(Reason.BAD_CREDENTIALS, "No account named '" + userName + "'.");
                }

                AuthenticatedUser user = found.get();

                // The password is checked before the account status, deliberately.
                // The legacy app checked status first, so anyone at the counter
                // could tell a real user name from an invented one by which
                // message came back. Both now look identical unless you already
                // know the password.
                if (!userDao.verifyPassword(connection, user.id(), password)) {
                    if (!userDao.hasUsableHash(connection, user.id())) {
                        // Same message to the user; a different line in the log,
                        // because this one is an administrator's problem.
                        log.error("Account {} has no readable password hash.", user.displayName());
                    }
                    throw refuse(Reason.BAD_CREDENTIALS, "Wrong password for '" + userName + "'.");
                }

                if (!userDao.isActive(connection, user.id())) {
                    throw refuse(Reason.ACCOUNT_DISABLED, "Account " + user.id() + " is switched off.");
                }

                // A super admin has no meaningful role row and must not be locked
                // out by one, which is how a fresh install stays recoverable.
                if (!user.isSuperAdmin() && !userDao.isRoleActive(connection, user.roleId())) {
                    throw refuse(Reason.ROLE_DISABLED,
                            "Role " + user.roleId() + " is switched off or missing.");
                }

                List<ShopProfile> shops = shopDao.findForUser(connection, user.id());

                if (shops.isEmpty()) {
                    throw refuse(Reason.NO_SHOP, "No active shop is linked to user " + user.id() + ".");
                }

                log.info("Authenticated {} ({}), {} shop(s) available.",
                        user.displayName(), user.roleName(), shops.size());

                return new Authentication(user, shops);
            });

        } finally {
            clear(password);
        }
    }

    // ==================================================================
    // step two: open a shop
    // ==================================================================

    @Override
    public AppContext openShop(Authentication authentication, int shopId) throws LoginFailedException {
        AuthenticatedUser user = authentication.user();

        return transactions.<AppContext, LoginFailedException>inTransaction(connection -> {

            // Read from shopusers rather than trusting the list carried in the
            // Authentication: that list came back over the same boundary the
            // caller controls, and this is the check that decides shop scope for
            // the whole session (defect D10).
            if (!shopDao.isLinkedToUser(connection, shopId, user.id())) {
                throw refuse(Reason.SHOP_NOT_ALLOWED,
                        "User " + user.id() + " is not linked to shop " + shopId + ".");
            }

            ShopProfile shop = shopDao.findById(connection, shopId)
                    .orElseThrow(() -> refuse(Reason.SHOP_NOT_ALLOWED, "No shop " + shopId + "."));

            // --- Gate 2: the shop ---
            if (!shop.active()) {
                throw refuse(Reason.SHOP_DISABLED, "Shop " + shopId + " is closed.");
            }

            CompanyProfile company = shopDao.findCompany(connection, shop.companyId())
                    .orElseThrow(() -> refuse(Reason.LICENCE_INACTIVE,
                            "Shop " + shopId + " points at company " + shop.companyId()
                            + ", which does not exist."));

            // --- Gate 1: the licence ---
            LicenceStatus licence = LicenceStatus.of(company, LocalDate.now(clock));

            if (licence.inactive()) {
                throw refuse(Reason.LICENCE_INACTIVE, "Company " + company.id() + " is switched off.");
            }
            if (licence.expired()) {
                throw refuse(Reason.LICENCE_EXPIRED,
                        "Licence expired on " + licence.expiryDate() + ".");
            }

            // A super admin skips every rights check, so loading a matrix for
            // them would only be a matrix nobody reads.
            Map<Integer, Rights> rights = user.isSuperAdmin()
                    ? Map.of()
                    : permissionDao.findRightsForRole(connection, user.roleId());

            LocalDateTime now = LocalDateTime.now(clock);

            int stale = userLogDao.closeOpenSessions(connection, user.id(), now);
            if (stale > 0) {
                log.info("Closed {} session(s) left open for {}.", stale, user.displayName());
            }

            int sessionId = userLogDao.openSession(connection, user.id(), now);

            log.info("{} opened shop {} ({}), {} feature right(s) loaded, session {}.",
                    user.displayName(), shop.name(), shop.id(), rights.size(), sessionId);

            if (licence.expiringSoon()) {
                log.warn("Licence expires in {} day(s), on {}.", licence.daysLeft(), licence.expiryDate());
            }

            return new AppContext(user, shop, company, licence, rights, sessionId);
        });
    }

    @Override
    public void signOut(AppContext context) {
        if (context == null) {
            return;
        }

        try {
            transactions.runInTransaction(connection ->
                    userLogDao.closeSession(connection, context.sessionId(), LocalDateTime.now(clock)));

            log.info("{} signed out of shop {}.",
                    context.user().displayName(), context.shop().name());

        } catch (RuntimeException e) {
            // Never let a bookkeeping failure trap someone in the application.
            log.error("Could not close session {}.", context.sessionId(), e);
        }
    }

    // ==================================================================
    // passwords
    // ==================================================================

    @Override
    public void changeOwnPassword(AppContext context, char[] currentPassword, char[] newPassword)
            throws ValidationException {

        try {
            validateNewPassword(newPassword);

            if (Arrays.equals(currentPassword, newPassword)) {
                throw new ValidationException("The new password must be different from the old one.");
            }

            transactions.<ValidationException>runInTransaction(connection -> {
                if (!userDao.verifyPassword(connection, context.userId(), currentPassword)) {
                    throw new ValidationException("Your current password is not correct.");
                }
                userDao.updatePassword(connection, context.userId(), Passwords.hash(newPassword));
            });

            log.info("{} changed their own password.", context.user().displayName());

        } finally {
            clear(currentPassword);
            clear(newPassword);
        }
    }

    @Override
    public void resetPassword(int targetUserId, char[] newPassword)
            throws PermissionDeniedException, NotFoundException, ValidationException {

        // Checked before the transaction opens: neither of these needs the
        // database, and a unit of work can only carry one kind of checked
        // failure out of it.
        permissions.require(Feature.ADD_USERS, Action.EDIT);
        validateNewPassword(newPassword);

        try {
            transactions.<NotFoundException>runInTransaction(connection -> {
                if (userDao.findById(connection, targetUserId).isEmpty()) {
                    throw new NotFoundException("User", targetUserId);
                }
                userDao.updatePassword(connection, targetUserId, Passwords.hash(newPassword));
            });

            log.info("User {} reset the password of user {}.", permissions.userId(), targetUserId);

        } finally {
            clear(newPassword);
        }
    }

    // ==================================================================

    private void validateNewPassword(char[] password) throws ValidationException {
        ValidationException.Check check = new ValidationException.Check();

        check.require(password != null && password.length >= MINIMUM_PASSWORD_LENGTH,
                "Use at least " + MINIMUM_PASSWORD_LENGTH + " characters.");

        // bcrypt reads 72 bytes and no more. PHP silently ignored the rest,
        // which means part of what someone typed was never protecting anything.
        check.require(password == null || Passwords.byteLength(password) <= Passwords.MAX_BYTES,
                "That password is too long. Use at most " + Passwords.MAX_BYTES + " characters.");

        check.throwIfFailed();
    }

    private LoginFailedException refuse(Reason reason, String detail) {
        // The detail goes to the log only. What the user sees is the reason's
        // own sentence, which never distinguishes a wrong name from a wrong
        // password.
        log.warn("Login refused ({}): {}", reason, detail);
        return new LoginFailedException(reason, detail);
    }

    /** Overwrites a password in memory so it is not left in the heap. */
    private static void clear(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
