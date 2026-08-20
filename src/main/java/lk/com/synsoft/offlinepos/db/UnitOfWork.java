package lk.com.synsoft.offlinepos.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A body of work that runs against one connection and returns a result.
 *
 * The second type parameter is what lets a unit of work throw a checked domain
 * exception - a validation failure has to be able to abort a transaction - while
 * a caller whose work throws nothing checked still needs no catch block. Java
 * infers it from what the lambda actually throws.
 *
 * @param <T> what the work produces
 * @param <E> the checked exception it may throw, or RuntimeException if none
 */
@FunctionalInterface
public interface UnitOfWork<T, E extends Exception> {

    T execute(Connection connection) throws SQLException, E;
}
