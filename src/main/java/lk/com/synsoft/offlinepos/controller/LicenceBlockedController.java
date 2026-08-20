package lk.com.synsoft.offlinepos.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lk.com.synsoft.offlinepos.app.Session;
import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.LicenceStatus;

/**
 * The only screen a lapsed licence can reach.
 *
 * It says which of the two problems it is and, if the licence expired, on what
 * date - because the date is the shop's evidence when they ring their supplier.
 * The old system said neither, and got the date wrong by a day into the bargain
 * (defect D12).
 */
public final class LicenceBlockedController implements View {

    @FXML private Label headingLabel;
    @FXML private Label detailLabel;

    private ViewContext context;

    @Override
    public void initialise(ViewContext context) {
        this.context = context;
    }

    @Override
    public void onShow() {
        AppContext session = Session.current();

        if (session == null) {
            headingLabel.setText("Licence");
            detailLabel.setText("Please sign in again.");
            return;
        }

        LicenceStatus licence = session.licence();

        if (licence.expired()) {
            headingLabel.setText("This licence has expired");
            detailLabel.setText("The licence for " + session.company().name()
                    + " ended on " + licence.expiryDate()
                    + ". Please contact your supplier to renew it.");
        } else {
            headingLabel.setText("This company is not active");
            detailLabel.setText("Please contact your supplier to activate "
                    + session.company().name() + ".");
        }
    }

    @FXML
    private void onSignOut() {
        context.router().signOut();
    }
}
