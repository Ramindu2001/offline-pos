package lk.com.synsoft.offlinepos.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a .sql file into individual statements.
 *
 * JDBC will not take a whole file, so the migration runner has to break it up.
 * Splitting naively on ';' would corrupt any statement containing one inside a
 * string - a city name or a column comment - so this tracks quoting and
 * comments as it goes.
 *
 * It is deliberately small: it understands the SQL our own migrations contain,
 * not every construct MySQL accepts. Anything more exotic (stored routines with
 * their own DELIMITER blocks) would need more, and we do not use them.
 */
final class SqlScript {

    private SqlScript() {
    }

    static List<String> split(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingle = false;   // '...'
        boolean inDouble = false;   // "..."
        boolean inBacktick = false; // `...`
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';

            // --- end of a comment ---
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    current.append(c);
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            // --- start of a comment, only outside quotes ---
            if (!inSingle && !inDouble && !inBacktick) {
                if (c == '-' && next == '-') {
                    inLineComment = true;
                    continue;
                }
                if (c == '#') {
                    inLineComment = true;
                    continue;
                }
                if (c == '/' && next == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
            }

            // --- quoting ---
            if (c == '\'' && !inDouble && !inBacktick) {
                // A backslash escape means the quote does not close the string.
                if (!(inSingle && isEscaped(sql, i))) {
                    // '' inside a string is a literal quote, not a close+open.
                    if (inSingle && next == '\'') {
                        current.append(c).append(next);
                        i++;
                        continue;
                    }
                    inSingle = !inSingle;
                }
            } else if (c == '"' && !inSingle && !inBacktick) {
                if (!(inDouble && isEscaped(sql, i))) {
                    inDouble = !inDouble;
                }
            } else if (c == '`' && !inSingle && !inDouble) {
                inBacktick = !inBacktick;
            }

            // --- statement break ---
            if (c == ';' && !inSingle && !inDouble && !inBacktick) {
                addIfMeaningful(statements, current);
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        addIfMeaningful(statements, current);
        return statements;
    }

    /** Counts the run of backslashes before position i; an odd count escapes. */
    private static boolean isEscaped(String sql, int i) {
        int backslashes = 0;
        for (int j = i - 1; j >= 0 && sql.charAt(j) == '\\'; j--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static void addIfMeaningful(List<String> statements, StringBuilder buffer) {
        String statement = buffer.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }
}
