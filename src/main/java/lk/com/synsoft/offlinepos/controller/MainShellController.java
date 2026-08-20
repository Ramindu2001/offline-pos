package lk.com.synsoft.offlinepos.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lk.com.synsoft.offlinepos.app.Navigation;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.Services;
import lk.com.synsoft.offlinepos.app.Session;
import lk.com.synsoft.offlinepos.app.ViewRouter;
import lk.com.synsoft.offlinepos.config.AppConfig;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.LicenceStatus;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.error.ErrorHandler;
import lk.com.synsoft.offlinepos.error.LoginFailedException;
import lk.com.synsoft.offlinepos.ui.BackgroundTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The sidebar, the header and the space a screen loads into.
 *
 * The sidebar is built once per session by walking
 * {@link Navigation#visibleTo}, which filters the whole menu in one pass. That
 * one pass replaces {@code View/sidebar.php}, which was 2,701 lines of the same
 * permission check written out again for every entry (defect D15).
 *
 * Built by {@link #build} rather than by the router's normal loading path: it is
 * the frame, not a destination, so it has no route and no guard of its own.
 */
public final class MainShellController {

    private static final Logger log = LoggerFactory.getLogger(MainShellController.class);

    private static final String FXML = "/lk/com/synsoft/offlinepos/view/main-shell.fxml";
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);

    @FXML private BorderPane root;
    @FXML private ScrollPane navScroll;
    @FXML private VBox navBox;
    @FXML private Label versionLabel;
    @FXML private Label pageTitle;
    @FXML private Label dateLabel;
    @FXML private MenuButton shopMenu;
    @FXML private MenuButton userMenu;
    @FXML private ProgressBar busyBar;
    @FXML private HBox licenceStrip;
    @FXML private Label licenceMessage;
    @FXML private StackPane content;

    private Services services;
    private ViewRouter router;

    /** Every nav button, so the active one can be marked without a rebuild. */
    private final Map<Route, Button> navButtons = new EnumMap<>(Route.class);

    public static MainShellController build(Services services, ViewRouter router) {
        try {
            FXMLLoader loader = new FXMLLoader(MainShellController.class.getResource(FXML));
            loader.load();

            MainShellController controller = loader.getController();
            controller.wire(services, router);
            return controller;

        } catch (IOException e) {
            throw new IllegalStateException("Could not load the application shell.", e);
        }
    }

    public BorderPane root() {
        return root;
    }

    private void wire(Services services, ViewRouter router) {
        this.services = services;
        this.router = router;

        AppContext session = Session.require();

        buildSidebar(session);
        buildHeader(session);

        versionLabel.setText("Version " + AppConfig.get().appVersion());
        dateLabel.setText(LocalDate.now().format(DATE));

        // Shown, never enforced: the bar appears, the screen stays usable.
        busyBar.visibleProperty().bind(BackgroundTasks.busy());
        busyBar.managedProperty().bind(busyBar.visibleProperty());

        showLicenceWarning(session.licence());
    }

    // ==================================================================
    // sidebar
    // ==================================================================

    private void buildSidebar(AppContext session) {
        navBox.getChildren().clear();
        navButtons.clear();

        List<Navigation.Group> groups = Navigation.visibleTo(session);

        for (Navigation.Group group : groups) {
            if (!group.isFlat()) {
                Label heading = new Label(group.title().toUpperCase(Locale.ENGLISH));
                heading.getStyleClass().add("nav-heading");
                navBox.getChildren().add(heading);
            }

            for (Navigation.Item item : group.items()) {
                navBox.getChildren().add(navButton(item));
            }
        }

        log.info("Sidebar built for {}: {} group(s), {} item(s).",
                session.user().displayName(), groups.size(), navButtons.size());
    }

    private Button navButton(Navigation.Item item) {
        Button button = new Button(item.label());
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> router.go(item.route()));

        // A screen a later phase builds still navigates - it opens the
        // placeholder - but saying so up front beats a cashier finding out.
        if (!item.route().isBuilt()) {
            button.getStyleClass().add("nav-item-pending");
            button.setTooltip(new javafx.scene.control.Tooltip(
                    item.label() + " arrives in Phase " + item.route().phase() + "."));
        }

        navButtons.put(item.route(), button);
        return button;
    }

    // ==================================================================
    // header
    // ==================================================================

    private void buildHeader(AppContext session) {
        userMenu.setText(session.user().displayName());

        MenuItem role = new MenuItem("Role: " + session.user().roleName());
        role.setDisable(true);

        MenuItem changePassword = new MenuItem("Change password");
        changePassword.setOnAction(event -> router.go(Route.CHANGE_PASSWORD));

        MenuItem signOut = new MenuItem("Sign out");
        signOut.setOnAction(event -> router.signOut());

        userMenu.getItems().setAll(role, new SeparatorMenuItem(), changePassword, signOut);

        buildShopMenu(session);
    }

    /**
     * The shop switcher.
     *
     * The list is read in the background: a shop with several branches would
     * otherwise pause the header while it queried, on a screen that is supposed
     * to appear instantly.
     */
    private void buildShopMenu(AppContext session) {
        shopMenu.setText(session.shop().name());

        BackgroundTasks.run(
                () -> services.auth().availableShops(session),
                shops -> {
                    if (shops.size() <= 1) {
                        // Nothing to switch to. A menu that only ever shows where
                        // you already are is noise on a till.
                        shopMenu.setDisable(true);
                        return;
                    }

                    shopMenu.getItems().setAll(shops.stream().map(this::shopItem).toList());
                },
                message -> shopMenu.setDisable(true),
                "Loading the shop list");
    }

    private MenuItem shopItem(ShopProfile shop) {
        MenuItem item = new MenuItem(shop.name());

        item.setDisable(shop.id() == Session.require().shopId());
        item.setOnAction(event -> switchTo(shop));

        return item;
    }

    /**
     * Moves the session to another shop.
     *
     * The router forgets every cached view first. A screen built against the old
     * shop must not survive the switch: showing one shop's stock under another
     * shop's header is how defect D10 started.
     */
    private void switchTo(ShopProfile shop) {
        try {
            AppContext moved = services.auth().switchShop(Session.require(), shop.id());
            router.enter(moved);

        } catch (LoginFailedException e) {
            licenceMessage.setText(ErrorHandler.explain("Switching shop", e));
            licenceStrip.setVisible(true);
            licenceStrip.setManaged(true);
        }
    }

    /**
     * The expiry warning strip.
     *
     * A shop that finds out its licence has lapsed when the till stops selling
     * has found out too late, so the warning starts a month out. An expired
     * licence never reaches here - the router sends it to the licence screen.
     */
    private void showLicenceWarning(LicenceStatus licence) {
        if (!licence.expiringSoon()) {
            return;
        }

        long days = licence.daysLeft();

        licenceMessage.setText(days == 0
                ? "This licence expires today, " + licence.expiryDate() + ". Please contact your supplier."
                : "This licence expires in " + days + (days == 1 ? " day" : " days")
                  + ", on " + licence.expiryDate() + ". Please contact your supplier.");

        licenceStrip.setVisible(true);
        licenceStrip.setManaged(true);
    }

    // ==================================================================
    // content
    // ==================================================================

    /** Puts a view in the frame and marks its sidebar entry as the current one. */
    public void setContent(Route route, Node view) {
        content.getChildren().setAll(view);
        pageTitle.setText(route.title());

        navButtons.forEach((navRoute, button) ->
                button.pseudoClassStateChanged(ACTIVE, navRoute == route));

        navScroll.requestLayout();
    }

    private static final javafx.css.PseudoClass ACTIVE =
            javafx.css.PseudoClass.getPseudoClass("active");
}
