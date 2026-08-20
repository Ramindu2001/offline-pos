package lk.com.synsoft.offlinepos.app;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Who is signed in right now.
 *
 * One process, one till, one signed-in user - so this is a single holder rather
 * than a map. It is the only mutable piece of the security model: everything
 * else is an immutable {@link AppContext} that is replaced wholesale, never
 * edited.
 *
 * The services do not read this class. They are handed {@code Session::current}
 * as a supplier at startup, which keeps the service layer from importing the
 * layer above it and keeps them testable without a running application.
 *
 * <b>What this is not.</b> Holding a context here is not what makes a screen
 * safe. Defect D07 was 19 of 107 pages with no auth guard at all, and the answer
 * to that is Phase 4's router refusing to build a view without a context - not a
 * flag that each screen is trusted to check.
 */
public final class Session {

    private static final Logger log = LoggerFactory.getLogger(Session.class);

    /**
     * Volatile because a background task - a report, a backup - may read it
     * while the JavaFX thread is replacing it at a shop switch.
     */
    private static volatile AppContext current;

    private Session() {
    }

    /** The signed-in session, or null when nobody is signed in. */
    public static AppContext current() {
        return current;
    }

    public static boolean isSignedIn() {
        return current != null;
    }

    public static void begin(AppContext context) {
        if (context == null) {
            throw new IllegalArgumentException("A session needs a context.");
        }
        current = context;

        log.info("Session open: {} in {} ({}).",
                context.user().displayName(), context.shop().name(), context.shop().shopNo());
    }

    public static void end() {
        if (current != null) {
            log.info("Session closed: {}.", current.user().displayName());
        }
        current = null;
    }

    /**
     * The signed-in session, or a failure if there is none.
     *
     * For code that has no sensible behaviour without a user - which is most of
     * the application after Phase 4.
     */
    public static AppContext require() {
        AppContext context = current;

        if (context == null) {
            throw new IllegalStateException("No user is signed in.");
        }
        return context;
    }
}
