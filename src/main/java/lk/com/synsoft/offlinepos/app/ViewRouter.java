package lk.com.synsoft.offlinepos.app;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lk.com.synsoft.offlinepos.controller.MainShellController;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Authentication;
import lk.com.synsoft.offlinepos.error.ErrorHandler;
import lk.com.synsoft.offlinepos.ui.Shells;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one way to get from one screen to another.
 *
 * Every navigation runs the same three guards before anything is loaded, in
 * this order:
 *
 *   1. A route that needs a session, with nobody signed in, goes to the login
 *      screen. Nothing is constructed and no query runs.
 *   2. A session whose licence has lapsed sees the licence screen and nothing
 *      else, whatever it asked for.
 *   3. A route the session may not reach shows the refusal, and is logged.
 *
 * The order matters and the position matters more. The legacy application ran
 * its check inside the page, after the queries, and refused by emitting a
 * JavaScript redirect with no {@code exit} - so the data had already been
 * assembled and sent before the browser navigated away (defect D03). Here the
 * refusal happens before the FXML is opened.
 *
 * <b>Controllers are cached, views are refreshed.</b> A route keeps its
 * controller so a half-completed screen survives a trip to another one, and
 * {@link View#onShow()} runs on every visit so what it displays does not go
 * stale. The cache is emptied on sign-out and on a shop switch: a view holding
 * one shop's rows must never be shown under another shop's header.
 */
public final class ViewRouter {

    private static final Logger log = LoggerFactory.getLogger(ViewRouter.class);

    private record Loaded(Parent root, Object controller) {
    }

    private final Stage stage;
    private final Scene scene;
    private final Services services;

    private final Map<Route, Loaded> cache = new EnumMap<>(Route.class);

    private MainShellController mainShell;
    private Route current;

    /**
     * Carries a proven identity from the login screen to the shop chooser.
     *
     * A correct password does not open anything on its own - the shop and
     * licence gates have not run yet - so this is not a session and is
     * deliberately not held in {@link Session}. It is dropped the moment a shop
     * is opened or the login screen is shown again.
     */
    private Authentication pending;

    public ViewRouter(Stage stage, Scene scene, Services services) {
        this.stage = stage;
        this.scene = scene;
        this.services = services;
    }

    public Route current() {
        return current;
    }

    // ==================================================================
    // navigating
    // ==================================================================

    /** Opens a route, or whatever the guards say should be opened instead. */
    public void go(Route route) {
        Route target = allowed(route);

        try {
            show(target);
            current = target;

        } catch (RuntimeException e) {
            // A screen that will not build must not take the application with
            // it: the cashier is left where they were, with a sentence.
            String message = ErrorHandler.explain("Opening " + target.title(), e);
            log.error("Could not open {}.", target);

            if (current == null) {
                throw new IllegalStateException(message, e);
            }
        }
    }

    Route allowed(Route requested) {
        return resolve(requested, Session.current());
    }

    /**
     * Which route may actually be opened.
     *
     * Static and free of side effects on purpose: the decision that closes
     * defects D03 and D07 should be testable on its own, without a window, a
     * database or a signed-in application to run it in.
     */
    public static Route resolve(Route requested, AppContext session) {
        if (requested.shell().needsSession() && session == null) {
            log.warn("{} needs a session; sending to the login screen.", requested);
            return Route.LOGIN;
        }

        if (session != null && !session.licence().valid()) {
            log.warn("Licence is not valid; nothing but the licence screen is reachable.");
            return Route.LICENCE_BLOCKED;
        }

        if (!requested.isReachableBy(session)) {
            log.warn("{} refused for {} (role {}).", requested,
                    session.user().displayName(), session.user().roleName());
            return Route.ACCESS_DENIED;
        }

        return requested;
    }

    // ==================================================================
    // sessions
    // ==================================================================

    /**
     * Ends the session and returns to the login screen.
     *
     * The cache goes first. A cached controller holds rows, totals and a shop
     * id; leaving them in memory for the next person to sign in at the same till
     * would be a data leak between two cashiers on one machine.
     */
    public void signOut() {
        AppContext session = Session.current();

        if (session != null) {
            services.auth().signOut(session);
        }

        Session.end();
        pending = null;
        forget();
        go(Route.LOGIN);
    }

    /** Called after a successful sign-in, and after switching shop. */
    public void enter(AppContext session) {
        pending = null;

        Session.begin(session);
        forget();
        go(Route.HOME);
    }

    /** Hands a proven identity to the shop chooser. */
    public void awaitShopChoice(Authentication authentication) {
        this.pending = authentication;
        go(Route.SELECT_SHOP);
    }

    /** What the shop chooser is choosing for, or empty if it was reached directly. */
    public Optional<Authentication> pendingAuthentication() {
        return Optional.ofNullable(pending);
    }

    /** Drops every cached view. */
    public void forget() {
        cache.clear();
        mainShell = null;
        current = null;
    }

    // ==================================================================
    // building
    // ==================================================================

    private void show(Route route) {
        // A route whose screen a later phase builds still navigates, still keeps
        // its guard and still highlights its own entry in the sidebar. What it
        // opens is the placeholder, which says which phase fills it in.
        Route viewRoute = route.isBuilt() ? route : Route.NOT_BUILT;

        Loaded loaded = cache.computeIfAbsent(viewRoute, this::load);

        if (loaded.controller() instanceof PlaceholderView placeholder) {
            placeholder.setRequestedRoute(route);
        }

        switch (route.shell()) {
            case MAIN -> {
                MainShellController shell = mainShell();
                shell.setContent(route, loaded.root());
                scene.setRoot(shell.root());
            }
            case AUTH -> scene.setRoot(Shells.auth(loaded.root()));
            case POS -> scene.setRoot(Shells.pos(loaded.root()));
            case PRINT -> scene.setRoot(Shells.print(loaded.root()));
        }

        if (loaded.controller() instanceof View view) {
            view.onShow();
        }

        stage.setTitle(title(route));
        log.info("Showing {}.", route);
    }

    /** Implemented by the one controller that stands in for several routes. */
    public interface PlaceholderView {

        void setRequestedRoute(Route route);
    }

    private Loaded load(Route route) {
        String fxml = route.fxml().orElseThrow(() -> new IllegalStateException(
                route + " has no FXML; the router should have sent it to the placeholder."));

        try {
            FXMLLoader loader = new FXMLLoader(ViewRouter.class.getResource(fxml));

            if (loader.getLocation() == null) {
                throw new IllegalStateException("Missing view file: " + fxml);
            }

            Parent root = loader.load();
            Object controller = loader.getController();

            if (controller instanceof View view) {
                // The context refuses to exist for a signed-in shell with no
                // session, so this line is what makes an unguarded screen
                // impossible rather than merely discouraged.
                view.initialise(new ViewContext(services, this, route, Session.current()));
            }

            return new Loaded(root, controller);

        } catch (IOException e) {
            throw new IllegalStateException("Could not load " + fxml, e);
        }
    }

    /**
     * The sidebar and header, built once per session.
     *
     * Once, not per navigation: rebuilding it would reset which sidebar groups
     * the user had collapsed on every single click. {@link #forget()} drops it,
     * so a shop switch does rebuild it against the new shop's flags.
     */
    private MainShellController mainShell() {
        if (mainShell == null) {
            mainShell = MainShellController.build(services, this);
        }
        return mainShell;
    }

    private String title(Route route) {
        AppContext session = Session.current();

        if (session == null) {
            return "OfflinePOS";
        }
        return route.title() + " - " + session.shop().name() + " - OfflinePOS";
    }
}
