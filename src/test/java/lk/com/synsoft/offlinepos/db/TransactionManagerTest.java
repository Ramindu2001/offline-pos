package lk.com.synsoft.offlinepos.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import lk.com.synsoft.offlinepos.config.AppConfig;
import lk.com.synsoft.offlinepos.config.DataSourceProvider;
import lk.com.synsoft.offlinepos.error.DataAccessException;
import lk.com.synsoft.offlinepos.error.ValidationException;
import lk.com.synsoft.offlinepos.util.Money;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phase 2 gate: a multi-table write that fails part way through must leave
 * nothing behind.
 *
 * This is the test defect D01 never had. The legacy app wrote a sale across six
 * tables with no transaction, so a failure on the fifth left an invoice with
 * stock already deducted and no payment recorded. The two probe tables below
 * stand in for a header and its lines, with the same foreign key, so the
 * property is checked against a real InnoDB server rather than a mock: rollback
 * is a database behaviour, and a mock would only be testing this test.
 *
 * The tables are created and dropped by the test and are prefixed {@code zz_}
 * so they sort last and cannot be confused with a real one. Nothing here touches
 * an application table.
 *
 * The whole class is skipped when MySQL is not reachable, so a build on a
 * machine without a server still passes rather than reporting a failure it
 * cannot do anything about.
 */
class TransactionManagerTest {

    private static final String HEADER = "zz_tx_probe_header";
    private static final String LINE = "zz_tx_probe_line";

    private static DataSource dataSource;
    private static TransactionManager transactions;
    private static ProbeDao probe;

    @BeforeAll
    static void connect() throws SQLException {
        dataSource = DataSourceProvider.get();

        try (Connection connection = dataSource.getConnection()) {
            connection.rollback();
        } catch (SQLException e) {
            org.junit.jupiter.api.Assumptions.abort(
                    "Skipped: no MySQL at " + AppConfig.get().describe() + " (" + e.getMessage() + ")");
        }

        transactions = new TransactionManager(dataSource);
        probe = new ProbeDao();

        // DDL commits itself in MySQL, so this sits outside any unit of work.
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("DROP TABLE IF EXISTS " + LINE);
            statement.execute("DROP TABLE IF EXISTS " + HEADER);

            statement.execute("""
                    CREATE TABLE zz_tx_probe_header (
                      id      int NOT NULL AUTO_INCREMENT,
                      doc_no  varchar(20)    NOT NULL,
                      total   decimal(12,2)  NOT NULL DEFAULT 0.00,
                      PRIMARY KEY (id),
                      UNIQUE KEY uq_zz_tx_probe_doc_no (doc_no)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            statement.execute("""
                    CREATE TABLE zz_tx_probe_line (
                      id         int NOT NULL AUTO_INCREMENT,
                      header_id  int            NOT NULL,
                      item       varchar(60)    NOT NULL,
                      amount     decimal(12,2)  NOT NULL DEFAULT 0.00,
                      PRIMARY KEY (id),
                      CONSTRAINT fk_zz_tx_probe_line_header
                        FOREIGN KEY (header_id) REFERENCES zz_tx_probe_header (id)
                        ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }

    @AfterAll
    static void dropProbeTables() throws SQLException {
        if (dataSource == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("DROP TABLE IF EXISTS " + LINE);
            statement.execute("DROP TABLE IF EXISTS " + HEADER);
        }
        DataSourceProvider.close();
    }

    @BeforeEach
    void emptyProbeTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("DELETE FROM " + LINE);
            statement.executeUpdate("DELETE FROM " + HEADER);
            connection.commit();
        }
    }

    // ==================================================================
    // the gate
    // ==================================================================

    @Test
    @DisplayName("GATE: a multi-table write that fails part way leaves zero rows behind")
    void aFailedMultiTableWriteLeavesNothingBehind() {
        ValidationException refused = assertThrows(ValidationException.class, () ->
                // Explicit type argument: a lambda body that only ever throws
                // gives the compiler nothing to infer the exception type from.
                transactions.<ValidationException>runInTransaction(connection -> {
                    int headerId = probe.insertHeader(connection, "SALE-0001", Money.of("250"));
                    probe.insertLine(connection, headerId, "Rice 5kg", Money.of("150"));
                    probe.insertLine(connection, headerId, "Sugar 1kg", Money.of("100"));

                    // Where a real sale discovers the payment is short: after the
                    // header, the lines and the stock have all been written.
                    throw new ValidationException("The payment does not cover the bill.");
                }));

        assertEquals("The payment does not cover the bill.", refused.userMessage());

        assertEquals(0, countHeaders(), "The header was left behind.");
        assertEquals(0, countLines(), "The lines were left behind.");
    }

    @Test
    @DisplayName("a database failure part way rolls back the rows already written")
    void aDatabaseFailureRollsBackWhatCameBefore() {
        assertThrows(DataAccessException.class, () ->
                transactions.runInTransaction(connection -> {
                    int headerId = probe.insertHeader(connection, "SALE-0002", Money.of("100"));
                    probe.insertLine(connection, headerId, "Tea 400g", Money.of("100"));

                    // A line pointing at a header that does not exist. The foreign
                    // key added in V2 is what turns this into a failure instead of
                    // an orphan row of the kind the live database still holds.
                    probe.insertLine(connection, 999_999, "Orphan", Money.of("1"));
                }));

        assertEquals(0, countHeaders());
        assertEquals(0, countLines());
    }

    @Test
    @DisplayName("an unchecked failure rolls back too")
    void anUncheckedFailureRollsBack() {
        assertThrows(IllegalStateException.class, () ->
                transactions.<RuntimeException>runInTransaction(connection -> {
                    int headerId = probe.insertHeader(connection, "SALE-0003", Money.of("50"));
                    probe.insertLine(connection, headerId, "Salt", Money.of("50"));

                    throw new IllegalStateException("A bug, not a rule.");
                }));

        assertEquals(0, countHeaders());
        assertEquals(0, countLines());
    }

    @Test
    @DisplayName("work that returns is committed, all of it")
    void successfulWorkIsCommitted() {
        int headerId = transactions.inTransaction(connection -> {
            int id = probe.insertHeader(connection, "SALE-0004", Money.of("250"));
            probe.insertLine(connection, id, "Rice 5kg", Money.of("150"));
            probe.insertLine(connection, id, "Sugar 1kg", Money.of("100"));
            return id;
        });

        assertTrue(headerId > 0, "The generated key should have come back.");
        assertEquals(1, countHeaders());
        assertEquals(2, countLines());

        // And the money survived the round trip at the right scale.
        BigDecimal total = transactions.inReadOnly(connection ->
                probe.lineTotal(connection, headerId));

        assertEquals(new BigDecimal("250.00"), total);
    }

    // ==================================================================
    // nesting
    // ==================================================================

    @Test
    @DisplayName("an inner unit of work joins the outer one instead of committing on its own")
    void nestedWorkJoinsTheOuterTransaction() {
        assertThrows(ValidationException.class, () ->
                transactions.<ValidationException>runInTransaction(outer -> {
                    int headerId = probe.insertHeader(outer, "SALE-0005", Money.of("10"));

                    // A service implementation calling another one. If this opened
                    // a second connection it would commit independently, and the
                    // outer rollback below would not reach it.
                    transactions.runInTransaction(inner -> {
                        probe.insertLine(inner, headerId, "Nested line", Money.of("10"));
                    });

                    throw new ValidationException("Refused after the nested call.");
                }));

        assertEquals(0, countHeaders(), "The nested call committed the header independently.");
        assertEquals(0, countLines(), "The nested call committed its line independently.");
    }

    @Test
    @DisplayName("a read inside an open transaction sees that transaction's own writes")
    void readOnlyWorkJoinsAnOpenTransaction() {
        long linesSeenInside = transactions.inTransaction(connection -> {
            int headerId = probe.insertHeader(connection, "SALE-0006", Money.of("10"));
            probe.insertLine(connection, headerId, "Uncommitted", Money.of("10"));

            return transactions.inReadOnly(probe::countLines);
        });

        assertEquals(1, linesSeenInside);
    }

    @Test
    @DisplayName("a standalone read cannot write")
    void readOnlyWorkCannotWrite() {
        assertThrows(DataAccessException.class, () ->
                transactions.inReadOnly(connection ->
                        probe.insertHeader(connection, "SALE-0007", Money.of("10"))));

        assertEquals(0, countHeaders());
    }

    // ==================================================================
    // BaseDao
    // ==================================================================

    @Test
    @DisplayName("queryOne refuses to pick a winner when two rows match")
    void queryOneRefusesAmbiguity() {
        transactions.runInTransaction(connection -> {
            int first = probe.insertHeader(connection, "SALE-0008", Money.of("10"));
            probe.insertLine(connection, first, "Same name", Money.of("5"));
            probe.insertLine(connection, first, "Same name", Money.of("5"));
        });

        assertThrows(DataAccessException.class, () ->
                transactions.inReadOnly(connection -> probe.findLineByItem(connection, "Same name")));
    }

    @Test
    @DisplayName("queryOne comes back empty rather than null when nothing matches")
    void queryOneIsEmptyWhenNothingMatches() {
        Optional<String> found = transactions.inReadOnly(connection ->
                probe.findLineByItem(connection, "Nothing like this"));

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("updateOne fails when the WHERE clause matched nothing")
    void updateOneFailsWhenNothingMatched() {
        assertThrows(DataAccessException.class, () ->
                transactions.runInTransaction(connection ->
                        probe.renameHeader(connection, 999_999, "SALE-NEW")));
    }

    @Test
    @DisplayName("lines go in as one batch")
    void batchInsertsEveryRow() {
        int headerId = transactions.inTransaction(connection -> {
            int id = probe.insertHeader(connection, "SALE-0009", Money.of("30"));
            probe.insertLines(connection, id, List.of("One", "Two", "Three"), Money.of("10"));
            return id;
        });

        assertEquals(3, countLines());
        assertEquals(new BigDecimal("30.00"),
                transactions.inReadOnly(connection -> probe.lineTotal(connection, headerId)));
    }

    @Test
    @DisplayName("binding a double is refused before it can reach a decimal column")
    void bindingADoubleIsRefused() {
        DataAccessException refused = assertThrows(DataAccessException.class, () ->
                transactions.inTransaction(connection ->
                        probe.insertHeaderUnsafely(connection, "SALE-0010", 12.34)));

        assertTrue(refused.getMessage().contains("BigDecimal"), refused.getMessage());
        assertEquals(0, countHeaders());
    }

    // ==================================================================

    private long countHeaders() {
        return transactions.inReadOnly(probe::countHeaders);
    }

    private long countLines() {
        return transactions.inReadOnly(probe::countLines);
    }

    /**
     * A DAO shaped exactly like the real ones: explicit SQL per method, the
     * connection first, and every value bound.
     */
    private static final class ProbeDao extends BaseDao {

        int insertHeader(Connection connection, String docNo, BigDecimal total) throws SQLException {
            return insert(connection,
                    "INSERT INTO zz_tx_probe_header (doc_no, total) VALUES (?, ?)",
                    docNo, total);
        }

        int insertLine(Connection connection, int headerId, String item, BigDecimal amount)
                throws SQLException {

            return insert(connection,
                    "INSERT INTO zz_tx_probe_line (header_id, item, amount) VALUES (?, ?, ?)",
                    headerId, item, amount);
        }

        void insertLines(Connection connection, int headerId, List<String> items, BigDecimal each)
                throws SQLException {

            batch(connection,
                    "INSERT INTO zz_tx_probe_line (header_id, item, amount) VALUES (?, ?, ?)",
                    items.stream().map(item -> new Object[] {headerId, item, each}).toList());
        }

        void renameHeader(Connection connection, int id, String docNo) throws SQLException {
            updateOne(connection, "Renaming a probe header",
                    "UPDATE zz_tx_probe_header SET doc_no = ? WHERE id = ?",
                    docNo, id);
        }

        Optional<String> findLineByItem(Connection connection, String item) throws SQLException {
            return queryOne(connection,
                    "SELECT item FROM zz_tx_probe_line WHERE item = ?",
                    row -> Rows.text(row, "item"),
                    item);
        }

        BigDecimal lineTotal(Connection connection, int headerId) throws SQLException {
            return queryDecimal(connection,
                    "SELECT SUM(amount) FROM zz_tx_probe_line WHERE header_id = ?",
                    Money.ZERO, headerId);
        }

        long countHeaders(Connection connection) throws SQLException {
            return queryInt(connection, "SELECT COUNT(*) FROM zz_tx_probe_header", 0);
        }

        long countLines(Connection connection) throws SQLException {
            return queryInt(connection, "SELECT COUNT(*) FROM zz_tx_probe_line", 0);
        }

        /** Deliberately wrong: what the binder exists to catch. */
        int insertHeaderUnsafely(Connection connection, String docNo, double total)
                throws SQLException {

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO zz_tx_probe_header (doc_no, total) VALUES (?, ?)")) {

                bind(statement, docNo, total);
                return statement.executeUpdate();
            }
        }
    }
}
