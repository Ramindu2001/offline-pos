package lk.com.synsoft.offlinepos.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lk.com.synsoft.offlinepos.error.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The transaction boundary. This class is the reason the DAO layer exists.
 *
 * A unit of work borrows one connection, runs whatever DAO calls it needs on
 * that same connection, and ends exactly one way: commit if it returned,
 * rollback if anything at all was thrown.
 *
 * <pre>{@code
 * public int completeSale(Invoice invoice) throws ValidationException {
 *     return transactions.inTransaction(connection -> {
 *         int billNo = docNoDao.nextInvoiceNo(connection, shopId);   // SELECT ... FOR UPDATE
 *         int id     = invoiceDao.insertHeader(connection, invoice, billNo);
 *         invoiceDao.insertLines(connection, id, invoice.lines());
 *         inventoryDao.deduct(connection, invoice.lines());
 *         paymentDao.insert(connection, id, invoice.payments());
 *         return billNo;
 *     });
 * }
 * }</pre>
 *
 * This is the fix for defect D01. In the legacy app {@code beginTransaction()}
 * existed but was called from one controller in 96,000 lines, so completing a
 * sale wrote six tables with no atomicity: a failure on the fifth left an
 * invoice with stock already deducted and no payment recorded, and nothing
 * undid it.
 *
 * <b>Nesting.</b> A unit of work started while one is already running on this
 * thread joins it rather than opening a second connection. Two connections
 * would be two transactions, the inner one committing work the outer one may
 * still roll back, which is exactly the hole this class closes. Only the
 * outermost call commits.
 */
public final class TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    /** The connection of the transaction currently running on this thread, if any. */
    private static final ThreadLocal<Connection> ACTIVE = new ThreadLocal<>();

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ------------------------------------------------------------------
    // read-write
    // ------------------------------------------------------------------

    /**
     * Runs the work in a transaction and returns its result.
     *
     * Commits when the work returns. Rolls back on any {@link Throwable} - a
     * checked domain exception, an unchecked one, or an Error - and rethrows it
     * unchanged, so the caller sees the failure it actually caused rather than a
     * wrapper.
     */
    public <T, E extends Exception> T inTransaction(UnitOfWork<T, E> work) throws E {
        Connection joined = ACTIVE.get();
        if (joined != null) {
            return join(joined, work);
        }

        try (Connection connection = open()) {
            ACTIVE.set(connection);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;

            } catch (Throwable failure) {
                rollback(connection, failure);
                throw rethrow(failure);

            } finally {
                ACTIVE.remove();
            }
        } catch (SQLException e) {
            // Thrown by open(), commit() or close(), never by the work itself,
            // which the inner catch has already dealt with.
            throw new DataAccessException("Transaction could not be completed.", e);
        }
    }

    /**
     * As {@link #inTransaction(UnitOfWork)}, for work that produces nothing.
     *
     * A separate name rather than an overload. Java cannot tell which of two
     * same-named methods a one-line lambda means when the body is a method call
     * - {@code connection -> dao.delete(connection, id)} is potentially
     * compatible with both a returning and a non-returning shape - and that is
     * the most common line a service will write.
     */
    public <E extends Exception> void runInTransaction(VoidUnitOfWork<E> work) throws E {
        this.<Void, E>inTransaction(connection -> {
            work.execute(connection);
            return null;
        });
    }

    // ------------------------------------------------------------------
    // read-only
    // ------------------------------------------------------------------

    /**
     * Runs a query that writes nothing.
     *
     * Marked read-only so the driver and server can refuse it if it turns out to
     * write after all, and released with a rollback rather than a commit: there
     * is nothing to commit, and a report must never be able to leave a change
     * behind.
     *
     * Inside an open transaction this joins it instead, so a service that reads
     * back its own uncommitted writes sees them.
     */
    public <T, E extends Exception> T inReadOnly(UnitOfWork<T, E> work) throws E {
        Connection joined = ACTIVE.get();
        if (joined != null) {
            return join(joined, work);
        }

        try (Connection connection = open()) {
            connection.setReadOnly(true);
            ACTIVE.set(connection);
            try {
                return work.execute(connection);

            } catch (Throwable failure) {
                throw rethrow(failure);

            } finally {
                ACTIVE.remove();
                quietRollback(connection);
            }
        } catch (SQLException e) {
            throw new DataAccessException("The query could not be run.", e);
        }
    }

    // ------------------------------------------------------------------

    private Connection open() throws SQLException {
        Connection connection = dataSource.getConnection();

        // The pool is configured autoCommit=false, but a connection that arrived
        // any other way must not be allowed to commit a statement on its own.
        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }
        return connection;
    }

    /**
     * An inner unit of work, on the connection the outer one already holds. It
     * neither commits nor rolls back: whatever it throws travels up to the call
     * that owns the boundary, and that call rolls the whole thing back.
     */
    private <T, E extends Exception> T join(Connection connection, UnitOfWork<T, E> work) throws E {
        try {
            return work.execute(connection);
        } catch (SQLException e) {
            throw new DataAccessException("A statement failed inside an open transaction.", e);
        }
    }

    private void rollback(Connection connection, Throwable cause) {
        try {
            connection.rollback();
            log.debug("Rolled back after {}", cause.toString());
        } catch (SQLException e) {
            // The original failure is what the caller needs to see, so a failed
            // rollback is attached to it rather than thrown over the top of it.
            log.error("Rollback failed; the transaction may be left open.", e);
            cause.addSuppressed(e);
        }
    }

    private void quietRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.warn("Could not release a read-only connection cleanly.", e);
        }
    }

    /**
     * Rethrows the failure with its own type intact.
     *
     * A SQLException from a DAO becomes a DataAccessException here. That is the
     * one place the conversion happens, so no DAO and no service needs a catch
     * block for it. Everything else passes straight through: the cast is
     * unchecked but sound, because the only checked exceptions a unit of work
     * can throw are SQLException, handled just above, and E.
     */
    @SuppressWarnings("unchecked")
    private <E extends Exception> E rethrow(Throwable failure) throws E {
        if (failure instanceof SQLException sql) {
            throw new DataAccessException("A statement failed and the work was rolled back.", sql);
        }
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw (E) failure;
    }
}
