package lk.com.synsoft.offlinepos.db;

/**
 * The database could not be brought to the version this build expects.
 *
 * Always fatal at startup: running the app against a schema it does not
 * understand is how data gets written into the wrong shape.
 */
public class MigrationException extends RuntimeException {

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
