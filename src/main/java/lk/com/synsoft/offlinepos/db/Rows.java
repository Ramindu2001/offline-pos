package lk.com.synsoft.offlinepos.db;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lk.com.synsoft.offlinepos.util.Money;

/**
 * Column readers for {@link RowMapper} implementations.
 *
 * JDBC's own getters return 0 and false for SQL NULL, which is how a missing
 * price silently becomes free and a missing flag silently becomes off. Every
 * reader here that can return null does, and the ones that cannot say so in
 * their name.
 *
 * The money readers also fix the scale on the way out. Three of the columns
 * these read were FLOAT in the live database (defect D18) and are DECIMAL here;
 * pinning the scale at the boundary means no total is ever assembled from
 * values that disagree about how many decimal places they have.
 */
public final class Rows {

    private Rows() {
    }

    // ---------- text ----------

    /** Null stays null. */
    public static String string(ResultSet row, String column) throws SQLException {
        return row.getString(column);
    }

    /** Null and blank both become "", for a field the UI will always render. */
    public static String text(ResultSet row, String column) throws SQLException {
        String value = row.getString(column);
        return value == null ? "" : value.trim();
    }

    // ---------- numbers ----------

    public static int integer(ResultSet row, String column) throws SQLException {
        return row.getInt(column);
    }

    /** For a nullable foreign key, where 0 is a real id and null means none. */
    public static Integer integerOrNull(ResultSet row, String column) throws SQLException {
        int value = row.getInt(column);
        return row.wasNull() ? null : value;
    }

    public static long longValue(ResultSet row, String column) throws SQLException {
        return row.getLong(column);
    }

    // ---------- money and quantity ----------

    /** A money column at scale 2. NULL reads as zero, which is what a total needs. */
    public static BigDecimal money(ResultSet row, String column) throws SQLException {
        BigDecimal value = row.getBigDecimal(column);
        return value == null ? Money.ZERO : Money.round(value);
    }

    /** A quantity column at scale 3. */
    public static BigDecimal quantity(ResultSet row, String column) throws SQLException {
        BigDecimal value = row.getBigDecimal(column);
        return value == null ? Money.ZERO_QTY : Money.qty(value);
    }

    /** Where the difference between "no price set" and "priced at zero" matters. */
    public static BigDecimal moneyOrNull(ResultSet row, String column) throws SQLException {
        BigDecimal value = row.getBigDecimal(column);
        return value == null ? null : Money.round(value);
    }

    // ---------- dates ----------

    public static LocalDate date(ResultSet row, String column) throws SQLException {
        return row.getObject(column, LocalDate.class);
    }

    public static LocalDateTime dateTime(ResultSet row, String column) throws SQLException {
        return row.getObject(column, LocalDateTime.class);
    }

    public static LocalTime time(ResultSet row, String column) throws SQLException {
        return row.getObject(column, LocalTime.class);
    }

    // ---------- flags ----------

    /**
     * A tinyint used as a flag.
     *
     * The schema stores these as 1 and 0, but not consistently as tinyint(1),
     * so getBoolean is not always safe. Anything other than 0 is true, and NULL
     * is false.
     */
    public static boolean flag(ResultSet row, String column) throws SQLException {
        int value = row.getInt(column);
        return !row.wasNull() && value != 0;
    }

    /** True only when the column holds exactly the value given. */
    public static boolean is(ResultSet row, String column, int expected) throws SQLException {
        int value = row.getInt(column);
        return !row.wasNull() && value == expected;
    }
}
