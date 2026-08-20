package lk.com.synsoft.offlinepos.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Authentication;
import lk.com.synsoft.offlinepos.ui.BackgroundTasks;

/**
 * Signing in.
 *
 * The work happens off the JavaFX thread. A bcrypt verify at cost 10 takes
 * roughly a tenth of a second by design - that is what makes it worth using -
 * and a window that stops repainting while it runs looks like a crashed till.
 */
public final class LoginController implements View {

    @FXML private TextField userNameField;
    @FXML private PasswordField passwordField;
    @FXML private Button signInButton;
    @FXML private Label errorLabel;

    private ViewContext context;

    @Override
    public void initialise(ViewContext context) {
        this.context = context;
    }

    @Override
    public void onShow() {
        // Reached again after a sign-out, so nothing is left from last time.
        passwordField.clear();
        hideError();

        signInButton.setDisable(false);
        Platform.runLater(userNameField::requestFocus);
    }

    @FXML
    private void onSignIn() {
        String userName = userNameField.getText();
        char[] password = passwordField.getText().toCharArray();

        if (userName == null || userName.isBlank()) {
            showError("Please enter your user name.");
            return;
        }
        if (password.length == 0) {
            showError("Please enter your password.");
            return;
        }

        hideError();
        signInButton.setDisable(true);

        BackgroundTasks.run(
                () -> context.services().auth().authenticate(userName, password),
                this::onAuthenticated,
                message -> {
                    signInButton.setDisable(false);
                    passwordField.clear();
                    passwordField.requestFocus();
                    showError(message);
                },
                "Signing in");
    }

    /**
     * A correct password is not yet a session.
     *
     * With one shop the till opens straight into it, which is the usual case.
     * With several the user chooses, and the shop and licence gates run against
     * whichever they pick.
     */
    private void onAuthenticated(Authentication authentication) {
        if (!authentication.hasSingleShop()) {
            signInButton.setDisable(false);
            context.router().awaitShopChoice(authentication);
            return;
        }

        BackgroundTasks.run(
                () -> context.services().auth()
                        .openShop(authentication, authentication.onlyShop().id()),
                this::onSessionOpen,
                message -> {
                    signInButton.setDisable(false);
                    showError(message);
                },
                "Opening the shop");
    }

    private void onSessionOpen(AppContext session) {
        passwordField.clear();
        context.router().enter(session);
    }

    @FXML
    private void onForgotPassword() {
        context.router().go(Route.FORGOT_PASSWORD);
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
