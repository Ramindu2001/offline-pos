package lk.com.synsoft.offlinepos.service.impl;

import java.util.function.Supplier;

import lk.com.synsoft.offlinepos.dto.auth.Action;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.error.PermissionDeniedException;
import lk.com.synsoft.offlinepos.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answers the permission questions from the signed-in {@code AppContext}.
 *
 * The context is read through a {@link Supplier} rather than held, for two
 * reasons. Choosing a different shop replaces the context, and a service holding
 * the old one would keep enforcing the old shop's scope. And it keeps this class
 * out of the UI layer: {@code app.Session::current} is passed in at startup, so
 * the service layer never imports {@code app}.
 *
 * Every check is answered from memory. The matrix is loaded once at login, so a
 * permission check costs nothing - which matters, because a check that costs a
 * round trip is a check people find reasons to skip.
 */
public final class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final Supplier<AppContext> currentContext;

    public PermissionServiceImpl(Supplier<AppContext> currentContext) {
        this.currentContext = currentContext;
    }

    @Override
    public void require(Feature feature, Action action) throws PermissionDeniedException {
        AppContext context = context();

        if (context.can(feature, action)) {
            return;
        }

        // Worth a log line on its own: a refusal that a user did not expect is
        // usually a rights row that was never created, and on a standalone till
        // this file is the only way to find out.
        log.warn("Refused {} on {} for user {} (role {})",
                action, feature, context.user().displayName(), context.user().roleName());

        throw new PermissionDeniedException(feature.label(), action.verb());
    }

    @Override
    public void requireView(Feature feature) throws PermissionDeniedException {
        require(feature, Action.VIEW);
    }

    @Override
    public boolean can(Feature feature, Action action) {
        return isSignedIn() && currentContext.get().can(feature, action);
    }

    @Override
    public void requireShopFeature(ShopFlag flag) throws PermissionDeniedException {
        if (!context().has(flag)) {
            throw new PermissionDeniedException(
                    "this feature", "use " + flag.name().toLowerCase().replace('_', ' ') + " in");
        }
    }

    @Override
    public boolean shopHas(ShopFlag flag) {
        return isSignedIn() && currentContext.get().has(flag);
    }

    @Override
    public int shopId() {
        return context().shopId();
    }

    @Override
    public int userId() {
        return context().userId();
    }

    @Override
    public AppContext context() {
        AppContext context = currentContext.get();

        if (context == null) {
            // Not a PermissionDeniedException: nobody is being refused. A service
            // reached without a session is a routing bug, and the fix for D07 is
            // that it should not be constructible.
            throw new IllegalStateException(
                    "No user is signed in. A service was called outside a session.");
        }
        return context;
    }

    @Override
    public boolean isSignedIn() {
        return currentContext.get() != null;
    }
}
