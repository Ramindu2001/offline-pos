package lk.com.synsoft.offlinepos.error;

/**
 * Base class for failures a caller is expected to handle.
 *
 * Checked on purpose. A service that can refuse a request says so in its
 * signature, which is what stops a controller from quietly ignoring the
 * possibility and showing the cashier a half-finished screen.
 */
public abstract class AppException extends Exception implements UserFacing {

    private final String userMessage;

    /**
     * @param userMessage what the cashier sees
     * @param logMessage  what goes in the log - may name ids, tables and columns
     */
    protected AppException(String userMessage, String logMessage) {
        super(logMessage);
        this.userMessage = userMessage;
    }

    protected AppException(String userMessage, String logMessage, Throwable cause) {
        super(logMessage, cause);
        this.userMessage = userMessage;
    }

    @Override
    public String userMessage() {
        return userMessage;
    }
}
