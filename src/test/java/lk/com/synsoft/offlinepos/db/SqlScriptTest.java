package lk.com.synsoft.offlinepos.db;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The splitter is the one place a migration can be silently corrupted: a
 * mis-split statement either fails loudly or, worse, runs as two half
 * statements. The cases below are the ones our own migrations actually
 * contain - 1,846 city names among them.
 */
class SqlScriptTest {

    @Test
    @DisplayName("splits plain statements on the semicolon")
    void splitsPlainStatements() {
        List<String> statements = SqlScript.split("SELECT 1; SELECT 2; SELECT 3;");

        assertEquals(List.of("SELECT 1", "SELECT 2", "SELECT 3"), statements);
    }

    @Test
    @DisplayName("ignores a trailing semicolon and blank statements")
    void ignoresEmptyStatements() {
        assertEquals(List.of("SELECT 1"), SqlScript.split("SELECT 1;;;   ;"));
    }

    @Test
    @DisplayName("keeps a semicolon that sits inside a string literal")
    void keepsSemicolonInsideString() {
        List<String> statements =
                SqlScript.split("INSERT INTO t VALUES ('a;b'); SELECT 1;");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'a;b'"));
    }

    @Test
    @DisplayName("handles an apostrophe escaped with a backslash")
    void handlesBackslashEscapedQuote() {
        List<String> statements =
                SqlScript.split("INSERT INTO city VALUES ('Ja\\'ffna'); SELECT 1;");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("Ja\\'ffna"));
    }

    @Test
    @DisplayName("handles an apostrophe doubled up")
    void handlesDoubledQuote() {
        List<String> statements =
                SqlScript.split("INSERT INTO city VALUES ('Ja''ffna'); SELECT 1;");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("Ja''ffna"));
    }

    @Test
    @DisplayName("drops line comments but keeps the statement around them")
    void dropsLineComments() {
        String sql = """
                -- a leading comment
                SELECT 1; -- a trailing comment
                # a hash comment
                SELECT 2;
                """;

        List<String> statements = SqlScript.split(sql);

        assertEquals(2, statements.size());
        assertEquals("SELECT 1", statements.get(0).trim());
        assertEquals("SELECT 2", statements.get(1).trim());
    }

    @Test
    @DisplayName("drops block comments")
    void dropsBlockComments() {
        List<String> statements =
                SqlScript.split("/* header\n spanning lines */ SELECT 1; SELECT 2;");

        assertEquals(2, statements.size());
        assertEquals("SELECT 1", statements.get(0).trim());
    }

    @Test
    @DisplayName("does not treat a comment marker inside a string as a comment")
    void ignoresCommentMarkerInsideString() {
        List<String> statements = SqlScript.split("SELECT '-- not a comment'; SELECT 2;");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("-- not a comment"));
    }

    @Test
    @DisplayName("keeps a semicolon inside a backtick identifier")
    void keepsSemicolonInsideBacktick() {
        List<String> statements = SqlScript.split("SELECT `odd;name` FROM t; SELECT 2;");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("`odd;name`"));
    }

    @Test
    @DisplayName("keeps a multi-line CREATE TABLE in one piece")
    void keepsMultiLineStatementTogether() {
        String sql = """
                CREATE TABLE `t` (
                  `id` int NOT NULL AUTO_INCREMENT,
                  `name` varchar(45) DEFAULT NULL COMMENT '1=one; 2=two',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB;
                """;

        List<String> statements = SqlScript.split(sql);

        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("PRIMARY KEY"));
        assertTrue(statements.get(0).contains("1=one; 2=two"));
    }
}
