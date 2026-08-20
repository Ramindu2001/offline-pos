package lk.com.synsoft.offlinepos.error;

/**
 * A failure that already knows how to describe itself to the person at the till.
 *
 * The rule this exists to enforce: a message shown to a cashier is written when
 * the failure is created, by the code that knows what went wrong - not improvised
 * at the top of the stack out of whatever text the exception happened to carry.
 * {@code getMessage()} is for the log file and may contain ids, SQL state and
 * table names. {@link #userMessage()} may not.
 */
public interface UserFacing {

    /**
     * One plain sentence, safe to show on screen: no SQL, no stack, no
     * identifiers the user has never seen.
     */
    String userMessage();
}
