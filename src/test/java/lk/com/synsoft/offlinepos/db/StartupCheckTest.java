package lk.com.synsoft.offlinepos.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The check that stops a fresh install migrating the wrong server.
 *
 * Two MySQL-family servers run on a typical machine here: MySQL 8 on 3307,
 * which this application owns, and XAMPP's MariaDB 10.4 on 3306, which holds
 * the legacy reference database. One wrong digit in db.port would point a new
 * shop at the second one, and because MariaDB accepts SQL that MySQL 8 rejects
 * and has no utf8mb4_0900_ai_ci collation, the migrations would half-apply to a
 * database nobody meant to touch.
 *
 * The version strings below are the real ones both servers report.
 */
class StartupCheckTest {

    @Test
    @DisplayName("MariaDB is recognised even though it calls itself MySQL")
    void recognisesMariaDb() {
        // Connector/J reports MariaDB's product name as "MySQL"; only the
        // version string gives it away.
        assertTrue(StartupCheck.looksLikeMariaDb("MySQL", "10.4.32-MariaDB"));
        assertTrue(StartupCheck.looksLikeMariaDb("MariaDB", "10.11.6-MariaDB-log"));
        assertTrue(StartupCheck.looksLikeMariaDb("MySQL", "5.5.5-10.4.32-mariadb"));
    }

    @Test
    @DisplayName("a real MySQL 8 is not mistaken for one")
    void acceptsMySql() {
        assertFalse(StartupCheck.looksLikeMariaDb("MySQL", "8.0.46"));
        assertFalse(StartupCheck.looksLikeMariaDb("MySQL", "8.4.0"));
        assertFalse(StartupCheck.looksLikeMariaDb("MySQL", "8.0.36-log"));
    }
}
