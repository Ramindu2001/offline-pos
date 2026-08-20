package lk.com.synsoft.offlinepos.error;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The single place a failure becomes a sentence on screen.
 *
 * Every layer funnels through {@link #explain(String, Throwable)}: the full
 * cause and stack go to the log file, and the caller gets back one line that is
 * safe to show. Nothing below the controller may write to the screen, and
 * nothing above the service may read a stack trace (defects D09 and D13).
 *
 * Unexpected failures also get a short reference code, printed in the log line
 * and included in the message. With no cloud and no support console, that code
 * is how a shop's "it said something went wrong" becomes a findable line in
 * %APPDATA%\OfflinePOS\logs.
 */
public final class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private ErrorHandler() {
    }

    /**
     * Logs a failure and returns the sentence to show.
     *
     * @param context what was being attempted, for the log line - "Completing sale"
     * @param error   the failure
     */
    public static String explain(String context, Throwable error) {
        if (error instanceof UserFacing known) {
            // Expected: a rule refused, or a row was missing. The stack is noise,
            // but the message and cause chain still belong in the log.
            log.warn("{} failed: {}", context, error.getMessage(), error);
            return known.userMessage();
        }

        String reference = reference();
        log.error("{} failed unexpectedly [ref {}]", context, reference, error);

        return "Something went wrong and the action was not completed. "
                + "Nothing was saved. Reference " + reference + ".";
    }

    /**
     * Installs the last line of defence.
     *
     * Without it, anything thrown off the JavaFX thread or a background task
     * disappears into the default handler's console output - which, in a
     * packaged desktop app with no console attached, means nowhere at all.
     */
    public static void installUncaughtHandler() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> explain("Thread " + thread.getName(), error));
    }

    /** Six characters, no vowels and no look-alikes, so it can be read aloud. */
    private static String reference() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
