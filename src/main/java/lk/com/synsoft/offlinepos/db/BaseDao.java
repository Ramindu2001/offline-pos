package lk.com.synsoft.offlinepos.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lk.com.synsoft.offlinepos.error.DataAccessException;

/**
 * What every DAO extends: prepared-statement plumbing, and nothing else.
 *
 * Three rules hold everywhere below this class.
 *
 * <b>The connection comes in, it is never fetched.</b> Every helper takes the
 * caller's {@link Connection}, so a DAO cannot start a transaction of its own or
 * silently escape the one it is running in. Only
 * {@link TransactionManager} opens connections.
 *
 * <b>Values are bound, never spliced.</b> Each helper takes SQL with {@code ?}
 * placeholders and the values separately. There is no overload that takes an
 * assembled string. The legacy model layer called prepare() 479 times but bound
 * parameters in only 333 of them, concatenating variables into the other 146
 * (defect D08); here there is nowhere to put a concatenated value.
 *
 * <b>The shape of a statement is written out.</b> No helper takes a table name,
 * a column list or an order-by from a parameter. Defect D14 was a generic
 * insertData() that built both from caller-supplied array keys, which is a
 * dynamic-SQL hole wearing a helper's clothes. Each DAO method spells out its
 * own SQL as a constant.
 */
public abstract class BaseDao {

    // ------------------------------------------------------------------
    // reading
    // ------------------------------------------------------------------

    /**
     * The single row a query returns, if there is one.
     *
     * Throws when a second row turns up. A query written to identify one thing
     * that matches two is a bug in the query or a missing unique constraint, and
     * quietly taking the first row is how the wrong customer gets charged.
     */
    protected <T> Optional<T> queryOne(Connection connection, String sql,
                                       RowMapper<T> mapper, Object... params) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);

            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }

                T value = mapper.map(rows);

                if (rows.next()) {
                    throw new DataAccessException(
                            "Expected at most one row but the query matched more: " + sql);
                }
                return Optional.of(value);
            }
        }
    }

    /** Every row the query returns, in the order the database returned them. */
    protected <T> List<T> queryList(Connection connection, String sql,
                                    RowMapper<T> mapper, Object... params) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);

            try (ResultSet rows = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rows.next()) {
                    results.add(mapper.map(rows));
                }
                return results;
            }
        }
    }

    /**
     * The first column of the first row, or the fallback when nothing matched.
     *
     * For a SUM that no row satisfies, MySQL returns one row holding NULL rather
     * than no rows at all, so the fallback covers both.
     */
    protected BigDecimal queryDecimal(Connection connection, String sql,
                                      BigDecimal fallback, Object... params) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);

            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return fallback;
                }
                BigDecimal value = rows.getBigDecimal(1);
                return value == null ? fallback : value;
            }
        }
    }

    protected int queryInt(Connection connection, String sql,
                           int fallback, Object... params) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);

            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return fallback;
                }
                int value = rows.getInt(1);
                return rows.wasNull() ? fallback : value;
            }
        }
    }

    /** True when the query matches anything. Give it a SELECT 1 ... LIMIT 1. */
    protected boolean exists(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);

            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    // ------------------------------------------------------------------
    // writing
    // ------------------------------------------------------------------

    /** Runs an INSERT, UPDATE or DELETE and returns how many rows it touched. */
    protected int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    /**
     * An UPDATE or DELETE that must match exactly one row.
     *
     * Zero rows means the row was deleted or its id was wrong; more than one
     * means the WHERE clause is too loose. Either way the transaction should
     * stop, because the caller has been told a write succeeded that did not do
     * what it intended.
     */
    protected void updateOne(Connection connection, String what,
                             String sql, Object... params) throws SQLException {

        int affected = update(connection, sql, params);

        if (affected != 1) {
            throw new DataAccessException(
                    what + " should have changed exactly one row but changed " + affected + ".");
        }
    }

    /** Runs an INSERT and returns the generated primary key. */
    protected int insert(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(statement, params);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("Insert returned no generated key: " + sql);
                }
                return keys.getInt(1);
            }
        }
    }

    /**
     * One statement run over many rows of values.
     *
     * Document lines go in this way. An invoice with forty lines is one round
     * trip instead of forty, and every line is inside the caller's transaction
     * either way.
     */
    protected int[] batch(Connection connection, String sql,
                          Collection<Object[]> rows) throws SQLException {

        if (rows.isEmpty()) {
            return new int[0];
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Object[] params : rows) {
                bind(statement, params);
                statement.addBatch();
            }
            return statement.executeBatch();
        }
    }

    // ------------------------------------------------------------------
    // binding
    // ------------------------------------------------------------------

    /**
     * Binds the values to the placeholders, in order.
     *
     * The types listed are the ones the DTO layer is allowed to hold: BigDecimal
     * for money, java.time for dates, and no doubles anywhere. A type that is
     * not listed is rejected rather than passed to setObject and guessed at,
     * because the guess a driver makes for an unexpected type is where a date
     * turns into a string and a decimal turns into a float.
     */
    protected static void bind(PreparedStatement statement, Object... params) throws SQLException {
        if (params == null) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            int position = i + 1;
            Object value = params[i];

            if (value == null) {
                statement.setNull(position, Types.NULL);
            } else if (value instanceof String s) {
                statement.setString(position, s);
            } else if (value instanceof Integer n) {
                statement.setInt(position, n);
            } else if (value instanceof Long n) {
                statement.setLong(position, n);
            } else if (value instanceof Short n) {
                statement.setShort(position, n);
            } else if (value instanceof BigDecimal d) {
                statement.setBigDecimal(position, d);
            } else if (value instanceof Boolean b) {
                statement.setInt(position, b ? 1 : 0);
            } else if (value instanceof LocalDate d) {
                statement.setObject(position, d);
            } else if (value instanceof LocalDateTime d) {
                statement.setObject(position, d);
            } else if (value instanceof LocalTime t) {
                statement.setObject(position, t);
            } else if (value instanceof byte[] bytes) {
                statement.setBytes(position, bytes);
            } else if (value instanceof Enum<?> e) {
                statement.setString(position, e.name());
            } else if (value instanceof Double || value instanceof Float) {
                throw new DataAccessException(
                        "Parameter " + position + " is a " + value.getClass().getSimpleName()
                        + ". Money and quantities are BigDecimal - binary floating point cannot "
                        + "hold 0.01 exactly and will not add up over a long bill (defect D18).");
            } else if (value instanceof java.util.Date) {
                throw new DataAccessException(
                        "Parameter " + position + " is a java.util.Date. Use LocalDate or LocalDateTime.");
            } else {
                throw new DataAccessException(
                        "Parameter " + position + " is a " + value.getClass().getName()
                        + ", which has no defined mapping to a column.");
            }
        }
    }
}
