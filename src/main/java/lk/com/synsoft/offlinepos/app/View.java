package lk.com.synsoft.offlinepos.app;

/**
 * A controller that wants its context.
 *
 * FXML constructs controllers with a no-argument constructor, so dependencies
 * cannot arrive that way. The router calls {@link #initialise} once, after the
 * FXML fields are injected and before the view is shown.
 *
 * Implement {@link #onShow()} as well when the view holds data that can go
 * stale. Controllers are cached and reused, so a screen is initialised once but
 * shown many times.
 */
public interface View {

    /** Called once, immediately after the FXML is loaded. */
    void initialise(ViewContext context);

    /**
     * Called every time the view is brought to the front, including the first.
     *
     * Where a screen reloads whatever it lists. Doing it here rather than in
     * {@link #initialise} is what keeps a cached controller from showing a
     * cashier the stock levels from an hour ago.
     */
    default void onShow() {
    }
}
