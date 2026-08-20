package lk.com.synsoft.offlinepos.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.Session;
import lk.com.synsoft.offlinepos.app.ViewRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The keys that work anywhere in the application.
 *
 * Kept to the few that are safe from any screen. A till is used by people
 * looking at the customer rather than the keyboard, so a global key that could
 * discard a half-typed bill would be a liability; those belong to the till
 * screen itself, which Phase 8 wires from the {@code shortcutkeys} table.
 *
 * Registered on the scene, so they survive every root swap the router makes.
 *
 * Each one checks that somebody is signed in. An accelerator that fired on the
 * login screen would be a way into the application without a session, which is
 * exactly the class of hole defect D07 was.
 */
public final class Shortcuts {

    private static final Logger log = LoggerFactory.getLogger(Shortcuts.class);

    private Shortcuts() {
    }

    public static void install(Scene scene, ViewRouter router) {
        on(scene, KeyCode.F1, router, Route.HOME);
        on(scene, KeyCode.F2, router, Route.POS);
        on(scene, KeyCode.F3, router, Route.INVOICES);
        on(scene, KeyCode.F4, router, Route.PRODUCTS);

        // Ctrl rather than a bare key: signing out by mistake mid-sale would
        // cost a cashier the whole cart.
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                () -> {
                    if (Session.isSignedIn()) {
                        log.info("Signing out on Ctrl+L.");
                        router.signOut();
                    }
                });

        log.info("Global shortcuts installed: F1 Home, F2 POS, F3 Invoices, F4 Products, Ctrl+L sign out.");
    }

    /**
     * A jump to one route.
     *
     * The router applies the same guards as a click on the sidebar would, so a
     * key for a screen this role may not open shows the refusal rather than the
     * screen. There is no separate path here to keep in step.
     */
    private static void on(Scene scene, KeyCode key, ViewRouter router, Route route) {
        scene.getAccelerators().put(new KeyCodeCombination(key), () -> {
            if (Session.isSignedIn()) {
                router.go(route);
            }
        });
    }
}
