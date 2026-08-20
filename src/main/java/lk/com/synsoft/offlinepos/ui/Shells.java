package lk.com.synsoft.offlinepos.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The three simple frames. The fourth, the main shell, has enough behaviour of
 * its own to be a controller.
 *
 * Ported from the reference app's AuthLayout, PosLayout and PrintLayout. They
 * are built in code rather than FXML because each is a handful of nodes with no
 * fields to inject, and an FXML file per wrapper would be three more files to
 * keep in step with app.css.
 */
public final class Shells {

    private Shells() {
    }

    /**
     * A centred card under the product name. Login, forgotten password, choosing
     * a shop, and the licence refusal.
     */
    public static Parent auth(Parent view) {
        Label brand = new Label("OfflinePOS");
        brand.getStyleClass().add("auth-brand");

        Label tagline = new Label("Point of sale & inventory");
        tagline.getStyleClass().add("auth-tagline");

        VBox heading = new VBox(2, brand, tagline);
        heading.setAlignment(Pos.CENTER);

        VBox card = new VBox(view);
        card.getStyleClass().add("auth-card");

        VBox column = new VBox(22, heading, card);
        column.setAlignment(Pos.CENTER);
        column.setMaxWidth(400);
        column.setMaxHeight(VBox.USE_PREF_SIZE);

        StackPane root = new StackPane(column);
        root.getStyleClass().add("auth-shell");
        root.setPadding(new Insets(24));

        return root;
    }

    /**
     * Full screen, no sidebar, nothing that scrolls.
     *
     * The till fills the display and stays put: a cashier who can scroll the
     * frame can scroll the total off the screen mid-transaction.
     */
    public static Parent pos(Parent view) {
        VBox root = new VBox(view);
        root.getStyleClass().add("pos-shell");
        VBox.setVgrow(view, Priority.ALWAYS);

        return root;
    }

    /** White, no chrome. Only what goes on paper. */
    public static Parent print(Parent view) {
        StackPane root = new StackPane(view);
        root.getStyleClass().add("print-shell");

        return root;
    }
}
