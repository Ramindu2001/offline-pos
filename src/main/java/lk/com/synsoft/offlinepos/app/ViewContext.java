package lk.com.synsoft.offlinepos.app;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.service.PermissionService;

/**
 * What a controller is handed when its view is built.
 *
 * <b>This is where defect D07 is closed.</b> The legacy application had 107
 * pages in {@code Public/} and only 88 of them included an auth check, so
 * nineteen screens rendered and queried for anybody who knew the URL. Here a
 * controller cannot reach a service without one of these, and the constructor
 * below refuses to make one for a signed-in shell with no session. It is not a
 * convention that each screen is trusted to follow - there is no other way to
 * get the objects.
 *
 * @param session the signed-in session; null only for the auth shell, where by
 *                definition there is not one yet
 */
public record ViewContext(
        Services services,
        ViewRouter router,
        Route route,
        AppContext session) {

    public ViewContext {
        if (services == null || router == null || route == null) {
            throw new IllegalArgumentException("A view needs its services, its router and its route.");
        }

        if (route.shell().needsSession() && session == null) {
            throw new IllegalStateException(
                    "Refusing to build " + route + " with nobody signed in.");
        }
    }

    public PermissionService permissions() {
        return services.permissions();
    }

    /** The session, or a failure. For the views that cannot exist without one. */
    public AppContext requireSession() {
        if (session == null) {
            throw new IllegalStateException(route + " was built without a session.");
        }
        return session;
    }
}
