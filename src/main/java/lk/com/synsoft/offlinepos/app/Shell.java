package lk.com.synsoft.offlinepos.app;

/**
 * The four frames a view can be loaded into, mirroring the reference app's
 * layouts.
 *
 * A route names its shell, so the router knows what to build around a view
 * without the view knowing anything about its surroundings.
 */
public enum Shell {

    /** Centred card on the canvas. Login, forgot password, choose a shop. */
    AUTH,

    /** Sidebar, header and content. Everything the signed-in application does. */
    MAIN,

    /**
     * Full screen, no sidebar, no scrolling.
     *
     * The till needs every pixel, and a cashier must never be able to scroll the
     * frame away from the total.
     */
    POS,

    /** White, no chrome. What actually goes on paper. */
    PRINT;

    /** Whether a signed-in session is required to build a view in this shell. */
    public boolean needsSession() {
        return this != AUTH;
    }
}
