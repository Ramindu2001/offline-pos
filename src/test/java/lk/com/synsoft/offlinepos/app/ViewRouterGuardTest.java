package lk.com.synsoft.offlinepos.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser;
import lk.com.synsoft.offlinepos.dto.auth.CompanyProfile;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.LicenceStatus;
import lk.com.synsoft.offlinepos.dto.auth.Rights;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.dto.auth.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the router decides before it opens anything.
 *
 * These are defects D07 and D03 at the routing layer. Nineteen of the legacy
 * application's 107 pages had no auth guard at all, and the ones that did
 * refused by printing a JavaScript redirect after their queries had already
 * run. Here the decision happens first, and it is the only way in.
 */
class ViewRouterGuardTest {

    private static final Rights VIEW = new Rights(true, false, false, false, false, false);

    private static AppContext session(UserType type, LocalDate today, LocalDate expiry,
                                      boolean companyActive, Set<ShopFlag> flags,
                                      Feature... viewable) {

        AuthenticatedUser user = new AuthenticatedUser(
                5, "cashier", "", "", type, 3, "Cashier", BigDecimal.ZERO);

        ShopProfile shop = new ShopProfile(
                1, "SH_000001", "Main Shop", "", "", "", "", "", 1, true, false, true, flags);

        CompanyProfile company = new CompanyProfile(
                1, "CM_000001", "My Company", "", "OFFLINE-0001", "1.0",
                LocalDate.of(2026, 1, 1), expiry, companyActive, false, false);

        Map<Integer, Rights> rights = new java.util.HashMap<>();
        for (Feature feature : viewable) {
            rights.put(feature.id(), VIEW);
        }

        return new AppContext(user, shop, company,
                LicenceStatus.of(company, today), rights, 1);
    }

    private static AppContext cashier(Feature... viewable) {
        return session(UserType.NORMAL, LocalDate.of(2026, 8, 20), LocalDate.of(2099, 1, 1),
                true, EnumSet.of(ShopFlag.INVENTORY), viewable);
    }

    // ==================================================================

    @Test
    @DisplayName("with nobody signed in, every private route becomes the login screen")
    void noSessionGoesToLogin() {
        for (Route route : Route.values()) {
            Route decided = ViewRouter.resolve(route, null);

            if (route.shell().needsSession()) {
                assertEquals(Route.LOGIN, decided,
                        route + " must not be reachable with nobody signed in.");
            } else {
                assertEquals(route, decided, route + " is an auth screen and should open.");
            }
        }
    }

    @Test
    @DisplayName("a route the role holds opens")
    void allowedRouteOpens() {
        AppContext cashier = cashier(Feature.RETAIL_SALES, Feature.INVOICE_LIST);

        assertEquals(Route.POS, ViewRouter.resolve(Route.POS, cashier));
        assertEquals(Route.INVOICES, ViewRouter.resolve(Route.INVOICES, cashier));
        assertEquals(Route.HOME, ViewRouter.resolve(Route.HOME, cashier));
    }

    @Test
    @DisplayName("a route the role lacks becomes the refusal, before anything is loaded")
    void refusedRouteIsRedirected() {
        AppContext cashier = cashier(Feature.RETAIL_SALES);

        assertEquals(Route.ACCESS_DENIED, ViewRouter.resolve(Route.PRODUCTS, cashier));
        assertEquals(Route.ACCESS_DENIED, ViewRouter.resolve(Route.USERS, cashier));
        assertEquals(Route.ACCESS_DENIED, ViewRouter.resolve(Route.COMPANY, cashier));
    }

    @Test
    @DisplayName("a shop switch that is off refuses the route even with the right held")
    void shopFlagRefusesRoute() {
        // Every right on customers, but the shop does not do customers.
        AppContext cashier = cashier(Feature.CUSTOMERS);

        assertEquals(Route.ACCESS_DENIED, ViewRouter.resolve(Route.CUSTOMERS, cashier));
    }

    @Test
    @DisplayName("an expired licence leaves exactly one reachable screen")
    void expiredLicenceBlocksEverything() {
        AppContext expired = session(UserType.SUPER_ADMIN,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 19),
                true, EnumSet.allOf(ShopFlag.class), Feature.values());

        assertTrue(expired.licence().expired());

        for (Route route : Route.values()) {
            if (route == Route.LICENCE_BLOCKED) {
                continue;
            }
            assertEquals(Route.LICENCE_BLOCKED, ViewRouter.resolve(route, expired),
                    route + " should be unreachable on an expired licence.");
        }
    }

    @Test
    @DisplayName("an inactive company blocks everything too, super admin included")
    void inactiveCompanyBlocksEverything() {
        AppContext inactive = session(UserType.SUPER_ADMIN,
                LocalDate.of(2026, 8, 20), LocalDate.of(2099, 1, 1),
                false, EnumSet.allOf(ShopFlag.class), Feature.values());

        assertEquals(Route.LICENCE_BLOCKED, ViewRouter.resolve(Route.HOME, inactive));
        assertEquals(Route.LICENCE_BLOCKED, ViewRouter.resolve(Route.POS, inactive));
    }

    @Test
    @DisplayName("a super admin reaches every route the shop has switched on")
    void superAdminReachesEverything() {
        AppContext admin = session(UserType.SUPER_ADMIN,
                LocalDate.of(2026, 8, 20), LocalDate.of(2099, 1, 1),
                true, EnumSet.allOf(ShopFlag.class));

        for (Route route : Route.values()) {
            assertEquals(route, ViewRouter.resolve(route, admin),
                    route + " should be reachable by an administrator.");
        }
    }

    // ==================================================================
    // the constructor requirement that closes D07
    // ==================================================================

    @Test
    @DisplayName("a signed-in view cannot be given a context with no session")
    void viewContextRefusesToExistWithoutASession() {
        // Not a convention a screen is trusted to follow: a controller has no
        // other way to obtain its services, and this constructor will not make
        // one for a private route with nobody signed in.
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> new ViewContext(services(), router(), Route.PRODUCTS, null));

        assertTrue(refused.getMessage().contains("PRODUCTS"), refused.getMessage());

        for (Route route : Route.values()) {
            if (route.shell().needsSession()) {
                assertThrows(IllegalStateException.class,
                        () -> new ViewContext(services(), router(), route, null),
                        route + " must not be constructible without a session.");
            }
        }
    }

    @Test
    @DisplayName("an auth view may be built without one, because there is not one yet")
    void authViewsMayHaveNoSession() {
        ViewContext context = new ViewContext(services(), router(), Route.LOGIN, null);

        assertEquals(Route.LOGIN, context.route());
        assertThrows(IllegalStateException.class, context::requireSession);
    }

    /**
     * Neither of these touches the database or a window: the constructors only
     * hold what they are given, which is what makes the guard testable here.
     */
    private static Services services() {
        return new Services(null);
    }

    private static ViewRouter router() {
        return new ViewRouter(null, null, services());
    }
}
