package lk.com.synsoft.offlinepos.controller;

import javafx.fxml.FXML;
import lk.com.synsoft.offlinepos.app.Route;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;

/**
 * What "forgotten password" means with no server behind the till.
 *
 * The reference app posts an email address and sends a reset link. There is
 * nothing here to send it from, and pretending otherwise - showing "check your
 * email" for a message that will never arrive - would be worse than saying so.
 * An administrator resets it instead, through
 * {@code AuthService.resetPassword}, which is gated on the Users feature.
 */
public final class ForgotPasswordController implements View {

    private ViewContext context;

    @Override
    public void initialise(ViewContext context) {
        this.context = context;
    }

    @FXML
    private void onBack() {
        context.router().go(Route.LOGIN);
    }
}
