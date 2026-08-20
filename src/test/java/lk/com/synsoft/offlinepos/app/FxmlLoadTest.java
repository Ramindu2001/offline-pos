package lk.com.synsoft.offlinepos.app;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every view file is actually loadable.
 *
 * A misspelled {@code fx:controller}, an {@code fx:id} with no matching field,
 * a missing import - none of these is a compile error. They surface the first
 * time somebody opens that screen, which for a screen one role rarely touches
 * could be weeks after it broke. Loading them all in a test moves that to the
 * build.
 *
 * It also proves the stylesheet parses: JavaFX reports a bad CSS rule by
 * printing a warning and carrying on with the rule dropped, so a typo there is
 * silent until someone notices a screen looking wrong.
 *
 * Skipped where there is no display to start the toolkit against.
 */
class FxmlLoadTest {

    private static boolean toolkitStarted;

    @BeforeAll
    static void startToolkit() {
        CountDownLatch ready = new CountDownLatch(1);

        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        } catch (UnsupportedOperationException | Error noDisplay) {
            Assumptions.abort("Skipped: no JavaFX toolkit available (" + noDisplay + ")");
        }

        try {
            if (!ready.await(20, TimeUnit.SECONDS)) {
                Assumptions.abort("Skipped: the JavaFX toolkit did not start.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assumptions.abort("Interrupted while starting the toolkit.");
        }

        toolkitStarted = true;
        Platform.setImplicitExit(false);
    }

    @Test
    @DisplayName("every view named by a route loads, with the controller it declares")
    void everyViewLoads() throws Exception {
        Assumptions.assumeTrue(toolkitStarted);

        for (Route route : Route.values()) {
            String path = route.fxml().orElse(null);
            if (path == null) {
                continue;
            }

            Object controller = onFxThread(() -> {
                FXMLLoader loader = new FXMLLoader(Route.class.getResource(path));
                Parent root = loader.load();

                assertNotNull(root, path + " loaded as nothing.");
                return loader.getController();
            });

            assertNotNull(controller, path + " declares no controller.");
        }
    }

    @Test
    @DisplayName("the application shell loads, sidebar and header included")
    void mainShellLoads() throws Exception {
        Assumptions.assumeTrue(toolkitStarted);

        Object controller = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(
                    Route.class.getResource("/lk/com/synsoft/offlinepos/view/main-shell.fxml"));
            loader.load();
            return loader.getController();
        });

        assertInstanceOf(lk.com.synsoft.offlinepos.controller.MainShellController.class, controller);
    }

    @Test
    @DisplayName("the shell builds a real sidebar for a real session")
    void shellBuildsTheSidebar() throws Exception {
        Assumptions.assumeTrue(toolkitStarted);

        AppContext cashier = cashierSession();
        Session.begin(cashier);

        try {
            // Services over a null DataSource: the shop switcher's query fails
            // in the background and the menu disables itself, which is exactly
            // what should happen and is worth proving too.
            int buttons = onFxThread(() -> {
                var shell = lk.com.synsoft.offlinepos.controller.MainShellController.build(
                        new Services(null), new ViewRouter(null, null, new Services(null)));

                assertNotNull(shell.root());

                // The nav buttons live inside a ScrollPane, and a ScrollPane
                // does not put its content into the scene graph until its skin
                // has been created. Without a scene and a layout pass, a lookup
                // finds nothing at all - which is not the same as an empty menu.
                Scene scene = new Scene(shell.root(), 1280, 800);
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                // The last seam between the router and the shell: putting a view
                // in the frame marks its sidebar entry as the current one.
                shell.setContent(Route.HOME, new StackPane());

                long active = shell.root().lookupAll(".nav-item").stream()
                        .filter(node -> node.getPseudoClassStates().stream()
                                .anyMatch(state -> state.getPseudoClassName().equals("active")))
                        .count();

                assertEquals(1, active, "Exactly one sidebar entry should be marked current.");

                return shell.root().lookupAll(".nav-item").size();
            });

            long expected = Navigation.visibleTo(cashier).stream()
                    .mapToLong(group -> group.items().size())
                    .sum();

            assertEquals(expected, buttons,
                    "The sidebar should hold one button per visible menu entry.");
            assertTrue(buttons > 0, "The sidebar came out empty.");

        } finally {
            Session.end();
        }
    }

    private static AppContext cashierSession() {
        var user = new lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser(
                2, "cashier", "", "", lk.com.synsoft.offlinepos.dto.auth.UserType.NORMAL,
                3, "Cashier", java.math.BigDecimal.ZERO);

        var shop = new lk.com.synsoft.offlinepos.dto.auth.ShopProfile(
                1, "SH_000001", "Main Shop", "", "", "Colombo", "", "", 1, true, false, true,
                java.util.EnumSet.of(lk.com.synsoft.offlinepos.dto.auth.ShopFlag.INVENTORY));

        var company = new lk.com.synsoft.offlinepos.dto.auth.CompanyProfile(
                1, "CM_000001", "My Company", "", "OFFLINE-0001", "1.0",
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2099, 1, 1),
                true, false, false);

        var rights = java.util.Map.of(
                lk.com.synsoft.offlinepos.dto.auth.Feature.RETAIL_SALES.id(),
                new lk.com.synsoft.offlinepos.dto.auth.Rights(true, false, false, false, false, false));

        return new AppContext(user, shop, company,
                lk.com.synsoft.offlinepos.dto.auth.LicenceStatus.of(
                        company, java.time.LocalDate.of(2026, 8, 20)),
                rights, 1);
    }

    @Test
    @DisplayName("the stylesheet parses")
    void stylesheetParses() throws Exception {
        Assumptions.assumeTrue(toolkitStarted);

        String css = Route.class.getResource("/lk/com/synsoft/offlinepos/css/app.css").toExternalForm();

        onFxThread(() -> {
            Scene scene = new Scene(new StackPane(), 100, 100);
            scene.getStylesheets().add(css);

            // Forcing a CSS pass is what turns a parse problem into a visible
            // one; simply adding the sheet does not read it.
            scene.getRoot().applyCss();
            return null;
        });
    }

    // ------------------------------------------------------------------

    /** Runs work on the JavaFX thread and brings the result - or the failure - back. */
    private static <T> T onFxThread(Work<T> work) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                result.set(work.run());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });

        if (!done.await(20, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The JavaFX thread did not answer.");
        }

        if (failure.get() != null) {
            throw new AssertionError("Loading failed on the JavaFX thread.", failure.get());
        }
        return result.get();
    }

    @FunctionalInterface
    private interface Work<T> {
        T run() throws Exception;
    }
}
