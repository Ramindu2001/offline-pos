package lk.com.synsoft.offlinepos.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A body of work that runs against one connection and returns nothing.
 *
 * @param <E> the checked exception it may throw, or RuntimeException if none
 */
@FunctionalInterface
public interface VoidUnitOfWork<E extends Exception> {

    void execute(Connection connection) throws SQLException, E;
}
