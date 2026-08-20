package lk.com.synsoft.offlinepos.controller;

import lk.com.synsoft.offlinepos.app.View;
import lk.com.synsoft.offlinepos.app.ViewContext;

/**
 * What a refused route shows.
 *
 * Deliberately vague about which of the reasons applies. A cashier who is told
 * "this is switched off for your shop" against "your role may not" learns the
 * shape of the permission model from the login screen outwards, and neither
 * answer helps them do their job. The precise reason is in the log.
 */
public final class AccessDeniedController implements View {

    @Override
    public void initialise(ViewContext context) {
        // Static text. It reads nothing and calls nothing, which is the point.
    }
}
