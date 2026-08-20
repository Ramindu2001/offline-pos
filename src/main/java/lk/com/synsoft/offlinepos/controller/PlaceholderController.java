package lk.com.synsoft.offlinepos.controller;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;
import lk.com.synsoft.offlinepos.app.ViewRouter;

/**
 * Stands in for a screen a later phase builds.
 *
 * It exists so the route catalogue can be complete from Phase 4 onwards. A
 * complete catalogue is what lets the sidebar be filtered - and tested - against
 * a real role now, instead of after all thirty-nine screens have landed.
 *
 * It also prints the guard the route carries, which turns out to be the quickest
 * way to answer "why can this role see this and not that" while the rest is
 * being built.
 */
public final class PlaceholderController implements View, ViewRouter.PlaceholderView {

    @FXML private Label titleLabel;
    @FXML private Label phaseLabel;
    @FXML private Label guardLabel;

    private Route requested = Route.NOT_BUILT;

    @Override
    public void initialise(ViewContext context) {
        // Nothing to wire: this screen reads no data and calls no service.
    }

    @Override
    public void setRequestedRoute(Route route) {
        this.requested = route;
    }

    @Override
    public void onShow() {
        titleLabel.setText(requested.title());
        phaseLabel.setText("This screen arrives in Phase " + requested.phase() + ".");
        guardLabel.setText(describeGuard(requested));
    }

    private static String describeGuard(Route route) {
        List<String> conditions = new ArrayList<>(3);

        if (route.adminOnly()) {
            conditions.add("administrators only");
        }
        route.feature().ifPresent(feature ->
                conditions.add("needs view on " + feature.label() + " (SFID " + feature.id() + ")"));
        route.flag().ifPresent(flag ->
                conditions.add("needs the shop switch " + flag.column()));

        return conditions.isEmpty()
                ? "Open to anyone signed in."
                : String.join("  ·  ", conditions);
    }
}
