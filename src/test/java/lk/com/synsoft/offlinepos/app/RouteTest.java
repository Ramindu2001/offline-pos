package lk.com.synsoft.offlinepos.app;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The route catalogue against the files it names.
 *
 * A mistyped FXML path is not a compile error and is not caught until somebody
 * clicks the menu entry - which, for a screen a role rarely opens, could be
 * weeks after it was broken.
 */
class RouteTest {

    @Test
    @DisplayName("every route that claims a view has one on the classpath")
    void everyDeclaredViewExists() {
        for (Route route : Route.values()) {
            route.fxml().ifPresent(path -> assertNotNull(
                    Route.class.getResource(path),
                    route + " names " + path + ", which is not in the build."));
        }
    }

    @Test
    @DisplayName("the screens Phase 4 promises are built; the rest say which phase builds them")
    void builtRoutesAreTheOnesPhaseFourPromised() {
        Set<Route> built = new HashSet<>();

        for (Route route : Route.values()) {
            if (route.isBuilt()) {
                built.add(route);
                assertEquals(4, route.phase(), route + " is built, so it belongs to Phase 4.");
            } else {
                assertTrue(route.phase() > 4,
                        route + " has no view and no later phase to build it.");
            }
        }

        assertEquals(
                Set.of(Route.LOGIN, Route.FORGOT_PASSWORD, Route.SELECT_SHOP, Route.LICENCE_BLOCKED,
                        Route.HOME, Route.CHANGE_PASSWORD, Route.ACCESS_DENIED, Route.NOT_BUILT),
                built);
    }

    @Test
    @DisplayName("only the auth shell is reachable without signing in")
    void onlyAuthIsPublic() {
        for (Route route : Route.values()) {
            boolean publicRoute = route.isReachableBy(null);

            assertEquals(route.shell() == Shell.AUTH, publicRoute,
                    route + " is in the " + route.shell() + " shell; it must "
                    + (route.shell() == Shell.AUTH ? "" : "not ") + "be reachable signed out.");
        }
    }

    @Test
    @DisplayName("every route in the menu appears exactly once")
    void theMenuHasNoDuplicates() {
        Set<Route> seen = new HashSet<>();

        for (Navigation.Group group : Navigation.all()) {
            for (Navigation.Item item : group.items()) {
                assertTrue(seen.add(item.route()),
                        item.route() + " is in the sidebar twice.");
            }
        }
    }

    @Test
    @DisplayName("the placeholder and the refusal are never put in the menu")
    void internalRoutesAreNotInTheMenu() {
        for (Navigation.Group group : Navigation.all()) {
            for (Navigation.Item item : group.items()) {
                assertFalse(item.route() == Route.NOT_BUILT || item.route() == Route.ACCESS_DENIED
                            || item.route() == Route.LICENCE_BLOCKED,
                        item.route() + " is an internal destination, not a menu entry.");
            }
        }
    }

    @Test
    @DisplayName("an administrator-only route carries no feature right, because it does not need one")
    void adminOnlyRoutesNeedNoFeature() {
        for (Route route : Route.values()) {
            if (route.adminOnly()) {
                assertTrue(route.feature().isEmpty(),
                        route + " is administrators only; a super admin holds every right anyway, "
                        + "so a feature here would be a rule that can never fire.");
            }
        }
    }

    @Test
    @DisplayName("every route has a title worth showing in a window bar")
    void everyRouteHasATitle() {
        for (Route route : Route.values()) {
            assertFalse(route.title().isBlank(), route + " has no title.");
        }
    }
}
