package lk.com.synsoft.offlinepos.controller;

import java.util.Arrays;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;
import lk.com.synsoft.offlinepos.service.AuthService;
import lk.com.synsoft.offlinepos.ui.BackgroundTasks;

/**
 * Changing your own password.
 *
 * The current password is asked for even though the user is already signed in.
 * A till is left unlocked at a counter more often than anyone admits, and this
 * is the one action that would let a passer-by keep the account.
 */
public final class ChangePasswordController implements View {

    @FXML private PasswordField currentField;
    @FXML private PasswordField newField;
    @FXML private PasswordField repeatField;
    @FXML private Button saveButton;
    @FXML private Label messageLabel;

    private ViewContext context;

    @Override
    public void initialise(ViewContext context) {
        this.context = context;
    }

    @Override
    public void onShow() {
        clearFields();
        hideMessage();
        saveButton.setDisable(false);
        currentField.requestFocus();
    }

    @FXML
    private void onSave() {
        char[] current = currentField.getText().toCharArray();
        char[] fresh = newField.getText().toCharArray();
        char[] repeat = repeatField.getText().toCharArray();

        // Checked here because it is about two fields on this screen, not about
        // the password itself: the service has no second field to compare.
        if (!Arrays.equals(fresh, repeat)) {
            wipe(current, fresh, repeat);
            showMessage("The two new passwords do not match.", false);
            return;
        }

        if (fresh.length < AuthService.MINIMUM_PASSWORD_LENGTH) {
            wipe(current, fresh, repeat);
            showMessage("Use at least " + AuthService.MINIMUM_PASSWORD_LENGTH + " characters.", false);
            return;
        }

        hideMessage();
        saveButton.setDisable(true);

        BackgroundTasks.run(
                () -> {
                    // changeOwnPassword wipes both arrays itself, whatever happens.
                    context.services().auth()
                            .changeOwnPassword(context.requireSession(), current, fresh);
                    return null;
                },
                done -> {
                    clearFields();
                    saveButton.setDisable(false);
                    showMessage("Your password has been changed.", true);
                },
                message -> {
                    saveButton.setDisable(false);
                    showMessage(message, false);
                },
                "Changing your password");

        Arrays.fill(repeat, '\0');
    }

    @FXML
    private void onCancel() {
        clearFields();
        context.router().go(Route.HOME);
    }

    private void clearFields() {
        currentField.clear();
        newField.clear();
        repeatField.clear();
    }

    private void showMessage(String message, boolean good) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("form-error", "form-success");
        messageLabel.getStyleClass().add(good ? "form-success" : "form-error");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private static void wipe(char[]... passwords) {
        for (char[] password : passwords) {
            Arrays.fill(password, '\0');
        }
    }
}
