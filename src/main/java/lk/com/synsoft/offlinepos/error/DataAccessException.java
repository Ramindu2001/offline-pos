package lk.com.synsoft.offlinepos.error;

/**
 * The database could not be read or written.
 *
 * Unchecked, unlike its checked siblings, because no caller between the DAO and
 * the screen can do anything useful about a dropped connection or a broken
 * constraint. Forcing it into every signature would only produce catch blocks
 * that rethrow.
 *
 * It always wraps the original {@link java.sql.SQLException}: the cause carries
 * the SQL state and vendor code into the log, while {@link #userMessage()} keeps
 * all of that off the screen.
 */
public class DataAccessException extends RuntimeException implements UserFacing {

    private static final String DEFAULT_USER_MESSAGE =
            "The system could not reach the database. Please try again, and tell your supervisor if it keeps happening.";

    private final String userMessage;

    public DataAccessException(String logMessage, Throwable cause) {
        this(DEFAULT_USER_MESSAGE, logMessage, cause);
    }

    public DataAccessException(String userMessage, String logMessage, Throwable cause) {
        super(logMessage, cause);
        this.userMessage = userMessage;
    }

    public DataAccessException(String logMessage) {
        super(logMessage);
        this.userMessage = DEFAULT_USER_MESSAGE;
    }

    @Override
    public String userMessage() {
        return userMessage;
    }
}
