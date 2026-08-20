package lk.com.synsoft.offlinepos.controller;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import lk.com.synsoft.offlinepos.app.Navigation;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;

/**
 * The landing screen.
 *
 * Phase 13 puts the real dashboard here - takings, low stock, the day's totals.
 * Until then it does the one useful thing it can: shows who is signed in, in
 * which shop, and offers the handful of screens this role actually reaches, so
 * a shortcut exists for a cashier whose whole job is two of them.
 */
public final class HomeController implements View {

    /** Enough to be useful, few enough to scan. */
    private static final int MAX_TILES = 8;

    @FXML private Label greetingLabel;
    @FXML private Label shopLabel;
    @FXML private Label footnoteLabel;
    @FXML private FlowPane tiles;

    private ViewContext context;

    @Override
    public void initialise(ViewContext context) {
        this.context = context;
    }

    @Override
    public void onShow() {
        AppContext session = context.requireSession();

        greetingLabel.setText("Welcome, " + session.user().displayName());
        shopLabel.setText(session.shop().name()
                + (session.shop().city().isBlank() ? "" : ", " + session.shop().city())
                + "  -  " + session.user().roleName());

        buildTiles(session);
    }

    private void buildTiles(AppContext session) {
        List<Route> reachable = Navigation.visibleTo(session).stream()
                .flatMap(group -> group.items().stream())
                .map(Navigation.Item::route)
                .filter(route -> route != Route.HOME)
                .limit(MAX_TILES)
                .toList();

        tiles.getChildren().setAll(reachable.stream().map(this::tile).toList());

        int total = Navigation.visibleTo(session).stream()
                .mapToInt(group -> group.items().size())
                .sum();

        footnoteLabel.setText(total == 0
                ? "Your role has not been given access to any screen yet. Please ask your administrator."
                : "Your role can reach " + total + " screen" + (total == 1 ? "" : "s")
                  + ", all listed in the menu on the left.");
    }

    private Button tile(Route route) {
        Button tile = new Button(route.title());
        tile.getStyleClass().add("tile");
        tile.setOnAction(event -> context.router().go(route));

        if (!route.isBuilt()) {
            tile.getStyleClass().add("tile-pending");
        }
        return tile;
    }
}
