package lk.com.synsoft.offlinepos.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Turns the current row of a result set into one object.
 *
 * A mapper must read the row and nothing else: it must not call
 * {@link ResultSet#next()}, must not keep the result set, and must not go back
 * to the database. {@link BaseDao} owns the cursor.
 *
 * Read columns by name, never by index. An index silently reads the wrong
 * column the moment a SELECT list changes, and these SELECT lists will change
 * for fourteen more phases.
 *
 * @param <T> what the row becomes
 */
@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet row) throws SQLException;
}
