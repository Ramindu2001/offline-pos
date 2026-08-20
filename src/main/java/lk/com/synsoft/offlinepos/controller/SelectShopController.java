package lk.com.synsoft.offlinepos.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;
import lk.com.synsoft.offlinepos.dto.auth.Authentication;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.ui.BackgroundTasks;

/**
 * Which shop to open, for a user linked to more than one.
 *
 * Nothing is granted here. The identity was proved on the login screen; the
 * shop and licence gates run when a shop is actually opened, which is why this
 * screen still has to go through {@code AuthService.openShop} rather than
 * assembling a session itself.
 */
public final class SelectShopController implements View {

    @FXML private ListView<ShopProfile> shopList;
    @FXML private Button openButton;
    @FXML private Label errorLabel;
    @FXML private Label emptyLabel;

    private ViewContext context;

    @Override
    public void initialise(ViewContext context) {
        this.context = context;

        shopList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ShopProfile shop, boolean empty) {
                super.updateItem(shop, empty);

                if (empty || shop == null) {
                    setText(null);
                    return;
                }

                String where = shop.city().isBlank() ? "" : "  -  " + shop.city();
                setText(shop.name() + where);
            }
        });

        shopList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOpen();
            }
        });
    }

    @Override
    public void onShow() {
        hideError();

        Authentication authentication = context.router().pendingAuthentication().orElse(null);

        if (authentication == null) {
            // Reached without signing in - a stale window, or a bug. Back to the
            // start rather than showing an empty chooser.
            context.router().go(Route.LOGIN);
            return;
        }

        shopList.setItems(FXCollections.observableArrayList(authentication.shops()));

        boolean any = !authentication.shops().isEmpty();

        emptyLabel.setVisible(!any);
        emptyLabel.setManaged(!any);
        shopList.setVisible(any);
        shopList.setManaged(any);
        openButton.setDisable(!any);

        if (any) {
            shopList.getSelectionModel().selectFirst();
            shopList.requestFocus();
        }
    }

    @FXML
    private void onOpen() {
        ShopProfile chosen = shopList.getSelectionModel().getSelectedItem();

        if (chosen == null) {
            showError("Please choose a shop.");
            return;
        }

        Authentication authentication = context.router().pendingAuthentication().orElse(null);
        if (authentication == null) {
            context.router().go(Route.LOGIN);
            return;
        }

        hideError();
        openButton.setDisable(true);

        BackgroundTasks.run(
                () -> context.services().auth().openShop(authentication, chosen.id()),
                session -> context.router().enter(session),
                message -> {
                    openButton.setDisable(false);
                    showError(message);
                },
                "Opening " + chosen.name());
    }

    @FXML
    private void onBack() {
        context.router().signOut();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
