package lk.com.synsoft.offlinepos.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import lk.com.synsoft.offlinepos.config.AppConfig;
import lk.com.synsoft.offlinepos.config.DataSourceProvider;
import lk.com.synsoft.offlinepos.dao.impl.PermissionDaoImpl;
import lk.com.synsoft.offlinepos.dao.impl.ShopDaoImpl;
import lk.com.synsoft.offlinepos.dao.impl.UserDaoImpl;
import lk.com.synsoft.offlinepos.dao.impl.UserLogDaoImpl;
import lk.com.synsoft.offlinepos.db.MigrationRunner;
import lk.com.synsoft.offlinepos.db.TransactionManager;
import lk.com.synsoft.offlinepos.dto.auth.Action;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Authentication;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.error.LoginFailedException;
import lk.com.synsoft.offlinepos.error.PermissionDeniedException;
import lk.com.synsoft.offlinepos.error.ValidationException;
import lk.com.synsoft.offlinepos.service.AuthService;
import lk.com.synsoft.offlinepos.service.PermissionService;
import lk.com.synsoft.offlinepos.util.Passwords;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phase 3 gate, end to end against the real database.
 *
 * A cashier role that holds VIEW on products and nothing else is created, signed
 * in for real, and then asked to do things it may not. The point being proved is
 * the one defect D03 got wrong: the refusal happens in the service, so it holds
 * whether or not any screen thought to hide the button.
 *
 * Everything this test creates is prefixed {@code zz_} and removed afterwards.
 * The rows it touches outside its own are the seeded shop and company, which it
 * only reads.
 *
 * Skipped in full when MySQL is not reachable.
 */
class AuthServiceImplTest {

    private static final String CASHIER_NAME = "zz_test_cashier";
    private static final String CASHIER_PASSWORD = "cashier123";
    private static final String ROLE_NAME = "ZZ Test Cashier";

    private static final int SEEDED_SHOP = 1;

    /**
     * Watermark sentinel. Not zero: an empty userlog gives a watermark of 0, and
     * everything above it still has to be cleaned up.
     */
    private static final int NOT_CAPTURED = -1;

    private static DataSource dataSource;
    private static TransactionManager transactions;

    private static int roleId;
    private static int cashierId;
    private static LocalDate licenceExpiry;

    /**
     * The highest userlog id before this class ran.
     *
     * The cashier's own log rows go when the user does, but signing in as the
     * seeded administrator writes rows that nothing cascades. userlog is an audit
     * table, so the test removes exactly the rows it added rather than leaving
     * them for someone to puzzle over later.
     */
    private static int userLogWatermark = NOT_CAPTURED;

    /** What the service under test reads as "the signed-in user". */
    private static final AtomicReference<AppContext> session = new AtomicReference<>();
    private static PermissionService permissions;

    // ==================================================================
    // fixture
    // ==================================================================

    @BeforeAll
    static void createTestRoleAndUser() throws SQLException {
        dataSource = DataSourceProvider.get();

        try (Connection connection = dataSource.getConnection()) {
            connection.rollback();
        } catch (SQLException e) {
            Assumptions.abort("Skipped: no MySQL at " + AppConfig.get().describe()
                    + " (" + e.getMessage() + ")");
        }

        // Brings V5 in on a database that was left at V4.
        new MigrationRunner(dataSource).migrate();

        transactions = new TransactionManager(dataSource);
        permissions = new PermissionServiceImpl(session::get);

        removeTestRows();

        try (Connection connection = dataSource.getConnection()) {
            userLogWatermark = queryInt(connection, "SELECT COALESCE(MAX(ULID), 0) FROM userlog");

            roleId = insertReturningKey(connection, """
                    INSERT INTO userroles (UserRoleName, ur_status, added_by, user_ip)
                    VALUES (?, 1, 1, '127.0.0.1')
                    """, ROLE_NAME);

            // The whole of this role's authority: look at products and at the
            // invoice list, print an invoice. Nothing else, anywhere.
            grant(connection, Feature.PRODUCTS, true, false, false, false, false, true);
            grant(connection, Feature.INVOICE_LIST, true, false, false, false, false, false);

            cashierId = insertReturningKey(connection, """
                    INSERT INTO user (UserName, UserEmail, ContactNo, UserPwd, UserStat,
                                      UserRoles_URID, UserType, paylimit)
                    VALUES (?, 'zz_cashier@localhost', '', ?, 1, ?, 0, 0.00)
                    """, CASHIER_NAME, Passwords.hash(CASHIER_PASSWORD.toCharArray()), roleId);

            execute(connection,
                    "INSERT INTO shopusers (shop_SHID, user_USID) VALUES (?, ?)",
                    SEEDED_SHOP, cashierId);

            licenceExpiry = queryDate(connection, """
                    SELECT c.ComExpireDate
                      FROM company c
                      JOIN shop s ON s.Company_CMID = c.CMID
                     WHERE s.SHID = ?
                    """, SEEDED_SHOP);

            connection.commit();
        }
    }

    @AfterAll
    static void removeTestRowsAfterwards() throws SQLException {
        if (dataSource == null) {
            return;
        }
        removeTestRows();
        DataSourceProvider.close();
    }

    /**
     * Puts the fixture back exactly as it started, so no test can depend on
     * another one having run first.
     */
    @BeforeEach
    void resetFixture() throws SQLException {
        session.set(null);

        try (Connection connection = dataSource.getConnection()) {
            execute(connection, "UPDATE user SET UserPwd = ?, UserStat = 1 WHERE USID = ?",
                    Passwords.hash(CASHIER_PASSWORD.toCharArray()), cashierId);
            execute(connection, "UPDATE userroles SET ur_status = 1 WHERE URID = ?", roleId);
            execute(connection, "DELETE FROM userlog WHERE user_USID = ?", cashierId);
            connection.commit();
        }
    }

    // ==================================================================
    // the gate
    // ==================================================================

    @Test
    @DisplayName("GATE: a cashier is refused at the service, whatever the screen shows")
    void cashierIsRefusedAtTheService() throws Exception {
        AppContext context = signInAsCashier();

        // What the UI reads to decide whether to draw the button at all.
        assertTrue(context.can(Feature.PRODUCTS, Action.VIEW), "The cashier should see products.");
        assertFalse(context.can(Feature.PRODUCTS, Action.CREATE), "The Add button must be hidden.");
        assertFalse(context.canSee(Feature.ADD_USERS), "The Users menu entry must be hidden.");

        // And the same answers again with the screen out of the picture: this is
        // a direct call to the service, which is what a bypassed UI amounts to.
        assertDoesNotThrow(() -> permissions.require(Feature.PRODUCTS, Action.VIEW));

        assertThrows(PermissionDeniedException.class,
                () -> permissions.require(Feature.PRODUCTS, Action.CREATE));
        assertThrows(PermissionDeniedException.class,
                () -> permissions.require(Feature.PRODUCTS, Action.DELETE));
        assertThrows(PermissionDeniedException.class,
                () -> permissions.requireView(Feature.ADD_USERS));

        // A real service method, refused before it touches the database.
        AuthService service = service(Clock.systemDefaultZone());

        PermissionDeniedException refused = assertThrows(PermissionDeniedException.class,
                () -> service.resetPassword(cashierId, "hijacked1".toCharArray()));

        assertEquals("You do not have permission to change add users.", refused.userMessage());

        // The refusal was not cosmetic: the password is untouched.
        assertTrue(canSignIn(CASHIER_PASSWORD), "The refused call changed the password anyway.");
    }

    @Test
    @DisplayName("an administrator may do what the cashier may not")
    void administratorIsAllowedTheSameCall() throws Exception {
        AppContext admin = signInAsAdministrator();
        assertTrue(admin.isSuperAdmin());

        AuthService service = service(Clock.systemDefaultZone());
        assertDoesNotThrow(() -> service.resetPassword(cashierId, "reset-by-admin".toCharArray()));

        assertTrue(canSignIn("reset-by-admin"));
        assertFalse(canSignIn(CASHIER_PASSWORD));
    }

    // ==================================================================
    // signing in
    // ==================================================================

    @Test
    @DisplayName("a correct name and password opens the shop and loads the rights")
    void signsInAndOpensTheShop() throws Exception {
        AppContext context = signInAsCashier();

        assertEquals(CASHIER_NAME, context.user().userName());
        assertEquals(ROLE_NAME, context.user().roleName());
        assertFalse(context.isSuperAdmin());

        assertEquals(SEEDED_SHOP, context.shopId());
        assertTrue(context.shop().active());
        assertTrue(context.licence().valid());

        // Exactly the two features granted, and no more.
        assertEquals(2, context.rights().size());

        // The seeded shop is a plain retail configuration.
        assertTrue(context.has(ShopFlag.INVENTORY));
        assertFalse(context.has(ShopFlag.PRESCRIPTION));

        assertTrue(context.sessionId() > 0, "The session should have been recorded in userlog.");
        assertEquals(1, openSessionCount(cashierId));
    }

    @Test
    @DisplayName("a wrong password and an unknown user name are indistinguishable")
    void wrongPasswordLooksLikeAnUnknownUser() {
        AuthService service = service(Clock.systemDefaultZone());

        LoginFailedException wrongPassword = assertThrows(LoginFailedException.class,
                () -> service.authenticate(CASHIER_NAME, "not-the-password".toCharArray()));

        LoginFailedException unknownUser = assertThrows(LoginFailedException.class,
                () -> service.authenticate("zz_no_such_person", "anything".toCharArray()));

        assertEquals(LoginFailedException.Reason.BAD_CREDENTIALS, wrongPassword.reason());
        assertEquals(LoginFailedException.Reason.BAD_CREDENTIALS, unknownUser.reason());
        assertEquals(wrongPassword.userMessage(), unknownUser.userMessage());

        assertEquals(0, openSessionCount(cashierId), "A failed login must not open a session.");
    }

    @Test
    @DisplayName("a switched-off account is refused, and told so")
    void disabledAccountIsRefused() throws SQLException {
        setUserStat(0);

        LoginFailedException refused = assertThrows(LoginFailedException.class,
                () -> service(Clock.systemDefaultZone())
                        .authenticate(CASHIER_NAME, CASHIER_PASSWORD.toCharArray()));

        assertEquals(LoginFailedException.Reason.ACCOUNT_DISABLED, refused.reason());
    }

    @Test
    @DisplayName("a switched-off role is refused")
    void disabledRoleIsRefused() throws SQLException {
        setRoleStatus(0);

        LoginFailedException refused = assertThrows(LoginFailedException.class,
                () -> service(Clock.systemDefaultZone())
                        .authenticate(CASHIER_NAME, CASHIER_PASSWORD.toCharArray()));

        assertEquals(LoginFailedException.Reason.ROLE_DISABLED, refused.reason());
    }

    @Test
    @DisplayName("a shop the user is not linked to cannot be opened")
    void unlinkedShopIsRefused() throws Exception {
        AuthService service = service(Clock.systemDefaultZone());
        Authentication who = service.authenticate(CASHIER_NAME, CASHIER_PASSWORD.toCharArray());

        LoginFailedException refused = assertThrows(LoginFailedException.class,
                () -> service.openShop(who, 999_999));

        assertEquals(LoginFailedException.Reason.SHOP_NOT_ALLOWED, refused.reason());
    }

    // ==================================================================
    // the licence gate - defect D12
    // ==================================================================

    @Test
    @DisplayName("the shop still trades on the licence expiry date")
    void tradesOnTheExpiryDate() throws Exception {
        AppContext context = signInOn(licenceExpiry);

        assertTrue(context.licence().valid());
        assertEquals(0, context.licence().daysLeft());
    }

    @Test
    @DisplayName("and is refused the day after")
    void refusedTheDayAfterExpiry() throws Exception {
        AuthService service = service(clockOn(licenceExpiry.plusDays(1)));
        Authentication who = service.authenticate(CASHIER_NAME, CASHIER_PASSWORD.toCharArray());

        LoginFailedException refused =
                assertThrows(LoginFailedException.class, () -> service.openShop(who, SEEDED_SHOP));

        assertEquals(LoginFailedException.Reason.LICENCE_EXPIRED, refused.reason());
        assertTrue(refused.isLicenceProblem());
    }

    // ==================================================================
    // sessions
    // ==================================================================

    @Test
    @DisplayName("signing out closes the session that was opened")
    void signOutClosesTheSession() throws Exception {
        AppContext context = signInAsCashier();
        assertEquals(1, openSessionCount(cashierId));

        service(Clock.systemDefaultZone()).signOut(context);

        assertEquals(0, openSessionCount(cashierId));
    }

    @Test
    @DisplayName("a session left open by a power cut is closed at the next login")
    void staleSessionsAreClosed() throws Exception {
        signInAsCashier();          // the till dies here, nothing closes this one
        signInAsCashier();          // and the cashier signs in again

        assertEquals(1, openSessionCount(cashierId),
                "Only the newest session should still be open.");
    }

    // ==================================================================
    // changing a password
    // ==================================================================

    @Test
    @DisplayName("changing your own password needs the current one")
    void changingOwnPasswordNeedsTheCurrentOne() throws Exception {
        AppContext context = signInAsCashier();
        AuthService service = service(Clock.systemDefaultZone());

        assertThrows(ValidationException.class, () -> service.changeOwnPassword(
                context, "wrong-current".toCharArray(), "brand-new-1".toCharArray()));

        assertTrue(canSignIn(CASHIER_PASSWORD), "The password should not have changed.");

        service.changeOwnPassword(
                context, CASHIER_PASSWORD.toCharArray(), "brand-new-1".toCharArray());

        assertTrue(canSignIn("brand-new-1"));
        assertFalse(canSignIn(CASHIER_PASSWORD));
    }

    @Test
    @DisplayName("a new password that is too short, too long or unchanged is refused")
    void newPasswordIsValidated() throws Exception {
        AppContext context = signInAsCashier();
        AuthService service = service(Clock.systemDefaultZone());

        ValidationException tooShort = assertThrows(ValidationException.class,
                () -> service.changeOwnPassword(
                        context, CASHIER_PASSWORD.toCharArray(), "abc".toCharArray()));
        assertTrue(tooShort.userMessage().contains("at least 6"), tooShort.userMessage());

        assertThrows(ValidationException.class, () -> service.changeOwnPassword(
                context,
                CASHIER_PASSWORD.toCharArray(),
                "x".repeat(Passwords.MAX_BYTES + 1).toCharArray()));

        assertThrows(ValidationException.class, () -> service.changeOwnPassword(
                context, CASHIER_PASSWORD.toCharArray(), CASHIER_PASSWORD.toCharArray()));

        assertTrue(canSignIn(CASHIER_PASSWORD), "None of those should have changed anything.");
    }

    @Test
    @DisplayName("the password is wiped from memory once it has been used")
    void passwordArrayIsCleared() throws Exception {
        char[] password = CASHIER_PASSWORD.toCharArray();

        service(Clock.systemDefaultZone()).authenticate(CASHIER_NAME, password);

        assertArrayIsBlank(password);
    }

    // ==================================================================
    // helpers
    // ==================================================================

    private static AuthService service(Clock clock) {
        return new AuthServiceImpl(
                transactions,
                new UserDaoImpl(),
                new ShopDaoImpl(),
                new PermissionDaoImpl(),
                new UserLogDaoImpl(),
                permissions,
                clock);
    }

    private static Clock clockOn(LocalDate day) {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(day.atTime(10, 0).atZone(zone).toInstant(), zone);
    }

    private AppContext signInAsCashier() throws LoginFailedException {
        return signInOn(LocalDate.now());
    }

    private AppContext signInOn(LocalDate day) throws LoginFailedException {
        AuthService service = service(clockOn(day));

        Authentication who = service.authenticate(CASHIER_NAME, CASHIER_PASSWORD.toCharArray());
        assertTrue(who.hasSingleShop(), "The test cashier should be linked to exactly one shop.");

        AppContext context = service.openShop(who, who.onlyShop().id());
        session.set(context);
        return context;
    }

    private AppContext signInAsAdministrator() throws LoginFailedException {
        AuthService service = service(Clock.systemDefaultZone());

        // admin / admin123, from the V4 bootstrap migration.
        Authentication who = service.authenticate("admin", "admin123".toCharArray());
        AppContext context = service.openShop(who, SEEDED_SHOP);
        session.set(context);
        return context;
    }

    private boolean canSignIn(String password) {
        try {
            service(Clock.systemDefaultZone()).authenticate(CASHIER_NAME, password.toCharArray());
            return true;
        } catch (LoginFailedException e) {
            return false;
        }
    }

    private static void assertArrayIsBlank(char[] password) {
        for (char c : password) {
            assertEquals('\0', c, "The password was left in memory.");
        }
    }

    private int openSessionCount(int userId) {
        return transactions.inReadOnly(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM userlog WHERE user_USID = ? AND logStat = 1")) {

                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getInt(1) : 0;
                }
            }
        });
    }

    private void setUserStat(int stat) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            execute(connection, "UPDATE user SET UserStat = ? WHERE USID = ?", stat, cashierId);
            connection.commit();
        }
    }

    private void setRoleStatus(int status) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            execute(connection, "UPDATE userroles SET ur_status = ? WHERE URID = ?", status, roleId);
            connection.commit();
        }
    }

    private static void grant(Connection connection, Feature feature, boolean view, boolean create,
                              boolean edit, boolean delete, boolean verify, boolean print)
            throws SQLException {

        execute(connection, """
                INSERT INTO userroleaccess
                  (UserRolls_URID, SysFeatures_SFID, is_view, is_create, is_edit,
                   is_delete, is_verify, is_print)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                roleId, feature.id(),
                view ? 1 : 0, create ? 1 : 0, edit ? 1 : 0,
                delete ? 1 : 0, verify ? 1 : 0, print ? 1 : 0);
    }

    /**
     * Removes everything this class creates.
     *
     * The user goes first: {@code user.UserRoles_URID} points at the role, and
     * the foreign keys added in V2 cascade {@code shopusers} and {@code userlog}
     * from the user, and {@code userroleaccess} from the role.
     */
    private static void removeTestRows() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            execute(connection, "DELETE FROM user WHERE UserName = ?", CASHIER_NAME);
            execute(connection, "DELETE FROM userroles WHERE UserRoleName = ?", ROLE_NAME);

            if (userLogWatermark != NOT_CAPTURED) {
                execute(connection, "DELETE FROM userlog WHERE ULID > ?", userLogWatermark);
            }
            connection.commit();
        }
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {

            return rows.next() ? rows.getInt(1) : 0;
        }
    }

    private static void execute(Connection connection, String sql, Object... params)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            statement.executeUpdate();
        }
    }

    private static int insertReturningKey(Connection connection, String sql, Object... params)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(statement, params);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static LocalDate queryDate(Connection connection, String sql, Object... params)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getObject(1, LocalDate.class) : null;
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
