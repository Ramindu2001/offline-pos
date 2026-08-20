package lk.com.synsoft.offlinepos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;

import lk.com.synsoft.offlinepos.dto.auth.Action;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser;
import lk.com.synsoft.offlinepos.dto.auth.CompanyProfile;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.LicenceStatus;
import lk.com.synsoft.offlinepos.dto.auth.Rights;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.dto.auth.UserType;
import lk.com.synsoft.offlinepos.error.PermissionDeniedException;
import lk.com.synsoft.offlinepos.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules the whole permission model rests on, without a database in the way.
 */
class PermissionServiceImplTest {

    private static final Rights VIEW_ONLY = new Rights(true, false, false, false, false, false);
    private static final Rights VIEW_AND_PRINT = new Rights(true, false, false, false, false, true);

    private static AppContext context(UserType type, Map<Integer, Rights> rights, ShopFlag... flags) {
        AuthenticatedUser user = new AuthenticatedUser(
                7, "cashier", "cashier@shop", "", type, 4, "Cashier", BigDecimal.ZERO);

        ShopProfile shop = new ShopProfile(
                1, "SH_000001", "Main Shop", "1 High Street", "", "Colombo", "", "shop@localhost",
                1, true, false, true,
                flags.length == 0 ? EnumSet.noneOf(ShopFlag.class) : EnumSet.copyOf(java.util.List.of(flags)));

        CompanyProfile company = new CompanyProfile(
                1, "CM_000001", "My Company", "Sri Lanka", "OFFLINE-0001", "1.0",
                LocalDate.of(2026, 1, 1), LocalDate.of(2099, 1, 1), true, false, false);

        return new AppContext(user, shop, company,
                LicenceStatus.of(company, LocalDate.of(2026, 8, 20)), rights, 1);
    }

    private static PermissionService serviceFor(AppContext context) {
        return new PermissionServiceImpl(() -> context);
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("a right the role holds is allowed")
    void allowsWhatTheRoleHolds() throws Exception {
        PermissionService permissions = serviceFor(
                context(UserType.NORMAL, Map.of(Feature.PRODUCTS.id(), VIEW_ONLY)));

        assertDoesNotThrow(() -> permissions.require(Feature.PRODUCTS, Action.VIEW));
        assertTrue(permissions.can(Feature.PRODUCTS, Action.VIEW));
    }

    @Test
    @DisplayName("a right the role does not hold is refused at the service")
    void refusesWhatTheRoleLacks() {
        PermissionService permissions = serviceFor(
                context(UserType.NORMAL, Map.of(Feature.PRODUCTS.id(), VIEW_ONLY)));

        PermissionDeniedException refused = assertThrows(PermissionDeniedException.class,
                () -> permissions.require(Feature.PRODUCTS, Action.CREATE));

        assertEquals("Products", refused.feature());
        assertEquals("add", refused.action());
        assertEquals("You do not have permission to add products.", refused.userMessage());

        assertFalse(permissions.can(Feature.PRODUCTS, Action.CREATE));
    }

    @Test
    @DisplayName("a feature with no rights row at all is refused - absence is not permission")
    void refusesFeatureWithNoRow() {
        PermissionService permissions = serviceFor(
                context(UserType.NORMAL, Map.of(Feature.PRODUCTS.id(), VIEW_ONLY)));

        assertThrows(PermissionDeniedException.class,
                () -> permissions.require(Feature.RETAIL_SALES, Action.VIEW));

        assertFalse(permissions.can(Feature.RETAIL_SALES, Action.VIEW));
    }

    @Test
    @DisplayName("a super admin passes everything, with no rights rows at all")
    void superAdminPassesEverything() {
        PermissionService permissions = serviceFor(context(UserType.SUPER_ADMIN, Map.of()));

        for (Feature feature : Feature.values()) {
            for (Action action : Action.values()) {
                assertTrue(permissions.can(feature, action),
                        feature + "/" + action + " should be allowed for a super admin.");
            }
        }
    }

    @Test
    @DisplayName("the six rights are read independently of each other")
    void rightsAreIndependent() {
        PermissionService permissions = serviceFor(
                context(UserType.NORMAL, Map.of(Feature.INVOICE_LIST.id(), VIEW_AND_PRINT)));

        assertTrue(permissions.can(Feature.INVOICE_LIST, Action.VIEW));
        assertTrue(permissions.can(Feature.INVOICE_LIST, Action.PRINT));
        assertFalse(permissions.can(Feature.INVOICE_LIST, Action.EDIT));
        assertFalse(permissions.can(Feature.INVOICE_LIST, Action.DELETE));
        assertFalse(permissions.can(Feature.INVOICE_LIST, Action.VERIFY));
    }

    @Test
    @DisplayName("a shop flag is a separate question from a right")
    void shopFlagsGateSeparately() {
        // Every right in the world, but the shop does not do credit.
        Rights everything = new Rights(true, true, true, true, true, true);

        PermissionService permissions = serviceFor(context(
                UserType.NORMAL,
                Map.of(Feature.CUSTOMER_DUE_PAYMENT.id(), everything),
                ShopFlag.INVENTORY));

        assertTrue(permissions.can(Feature.CUSTOMER_DUE_PAYMENT, Action.CREATE));
        assertTrue(permissions.shopHas(ShopFlag.INVENTORY));
        assertFalse(permissions.shopHas(ShopFlag.CREDIT));

        assertThrows(PermissionDeniedException.class,
                () -> permissions.requireShopFeature(ShopFlag.CREDIT));
    }

    @Test
    @DisplayName("a super admin does not conjure a shop flag that is switched off")
    void superAdminDoesNotOverrideShopFlags() {
        PermissionService permissions = serviceFor(context(UserType.SUPER_ADMIN, Map.of()));

        assertFalse(permissions.shopHas(ShopFlag.PRESCRIPTION));
        assertThrows(PermissionDeniedException.class,
                () -> permissions.requireShopFeature(ShopFlag.PRESCRIPTION));
    }

    @Test
    @DisplayName("calling a service with nobody signed in is a bug, not a refusal")
    void noSessionIsAnIllegalState() {
        PermissionService permissions = new PermissionServiceImpl(() -> null);

        assertFalse(permissions.isSignedIn());
        assertFalse(permissions.can(Feature.PRODUCTS, Action.VIEW));

        assertThrows(IllegalStateException.class,
                () -> permissions.require(Feature.PRODUCTS, Action.VIEW));
        assertThrows(IllegalStateException.class, permissions::shopId);
    }

    @Test
    @DisplayName("shop scope comes from the session")
    void shopScopeComesFromTheSession() {
        PermissionService permissions = serviceFor(context(UserType.NORMAL, Map.of()));

        assertEquals(1, permissions.shopId());
        assertEquals(7, permissions.userId());
    }
}
