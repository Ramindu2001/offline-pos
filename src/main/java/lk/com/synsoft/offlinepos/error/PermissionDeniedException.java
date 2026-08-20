package lk.com.synsoft.offlinepos.error;

/**
 * The signed-in user may not do this.
 *
 * Thrown from the service implementation, which is the whole point. The legacy
 * app checked permission in the page, by emitting a JavaScript redirect with no
 * exit - so the page carried on rendering, its queries still ran, and the data
 * had already been sent before the browser navigated away (defect D03).
 *
 * Phase 3 fills this in properly with PermissionService. It is declared now so
 * the layers built in this phase can already name it.
 */
public class PermissionDeniedException extends AppException {

    private final String feature;
    private final String action;

    public PermissionDeniedException(String feature, String action) {
        super("You do not have permission to " + action.toLowerCase() + " " + feature.toLowerCase() + ".",
              "Permission denied: " + action + " on " + feature);
        this.feature = feature;
        this.action = action;
    }

    public String feature() {
        return feature;
    }

    public String action() {
        return action;
    }
}
