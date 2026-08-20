package lk.com.synsoft.offlinepos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;

/**
 * Every money and quantity calculation in the application.
 *
 * Ported from the reference app's {@code src/utils/money.js}, which the tested
 * posMath, grnMath, returnMath and creditMath modules are all built on. Matching
 * it exactly is what makes those tests usable as this application's
 * specification.
 *
 * <b>Scale and rounding.</b> Money is scale 2, quantities scale 3, both
 * HALF_UP. The scales come from the schema: 107 money columns are
 * decimal(12,2), the quantity columns are decimal(12,3). HALF_UP matches the
 * reference app's {@code Math.round((value + EPSILON) * 100) / 100}, which
 * rounds a half up, and it is what a receipt reader expects. Fix this now:
 * changing it later re-opens every total the shop has ever printed.
 *
 * <b>No doubles.</b> There is deliberately no {@code of(double)}. Offering one
 * would make {@code Money.of(0.1).add(Money.of(0.2))} look correct while
 * carrying binary error into a decimal column - and 27 of those columns really
 * were FLOAT in the live database, which is defect D18. Values arrive here from
 * a text field or a decimal column, and both come through as a String or a
 * BigDecimal already.
 */
public final class Money {

    /** Money: decimal(12,2) throughout the schema. */
    public static final int SCALE = 2;

    /** Quantity: decimal(12,3), so a shop can sell 0.25 kg. */
    public static final int QTY_SCALE = 3;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    public static final BigDecimal ZERO_QTY = BigDecimal.ZERO.setScale(QTY_SCALE);

    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final String DEFAULT_SYMBOL = "Rs.";

    private Money() {
    }

    // ------------------------------------------------------------------
    // building
    // ------------------------------------------------------------------

    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value.trim()));
    }

    /**
     * What a text field holds, as money.
     *
     * Anything unreadable - empty, half-typed, a stray letter - becomes zero
     * rather than throwing. That is the reference app's {@code toNumber}
     * behaviour, and it is what keeps a cashier mid-keystroke from being shown
     * an error dialog. Validation of what the field should contain happens in
     * the service, on the finished value.
     */
    public static BigDecimal parse(String value) {
        if (value == null) {
            return ZERO;
        }

        String cleaned = value.trim().replace(",", "");
        if (cleaned.isEmpty()) {
            return ZERO;
        }

        try {
            return round(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            return ZERO;
        }
    }

    /** As {@link #parse(String)}, at quantity scale. */
    public static BigDecimal parseQty(String value) {
        if (value == null) {
            return ZERO_QTY;
        }

        String cleaned = value.trim().replace(",", "");
        if (cleaned.isEmpty()) {
            return ZERO_QTY;
        }

        try {
            return qty(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            return ZERO_QTY;
        }
    }

    /** Null becomes zero. Every money column is read through this or {@link #round}. */
    public static BigDecimal orZero(BigDecimal value) {
        return value == null ? ZERO : round(value);
    }

    // ------------------------------------------------------------------
    // scale
    // ------------------------------------------------------------------

    /** To money scale. */
    public static BigDecimal round(BigDecimal value) {
        return value == null ? ZERO : value.setScale(SCALE, ROUNDING);
    }

    /** To quantity scale. */
    public static BigDecimal qty(BigDecimal value) {
        return value == null ? ZERO_QTY : value.setScale(QTY_SCALE, ROUNDING);
    }

    /**
     * To a scale of its own, for the few figures that are not money.
     *
     * grnMath reports a profit margin to one decimal place, and a report may
     * want none at all. Same rounding rule, so a percentage never disagrees with
     * the amount it was worked out from.
     */
    public static BigDecimal round(BigDecimal value, int scale) {
        return value == null
                ? BigDecimal.ZERO.setScale(scale)
                : value.setScale(scale, ROUNDING);
    }

    // ------------------------------------------------------------------
    // arithmetic
    //
    // Every result is rounded, so a total can never carry more precision than
    // the column it is going into. Rounding once at the end instead would let a
    // sum of forty lines disagree with the sum of the same forty lines as the
    // customer sees them printed.
    // ------------------------------------------------------------------

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(orZero(a).add(orZero(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(orZero(a).subtract(orZero(b)));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return ZERO;
        }
        return round(a.multiply(b));
    }

    /**
     * Divides, or returns zero when the divisor is zero.
     *
     * A till must not stop mid-sale over a division by zero, and the reference
     * app's toNumber() already turns a non-finite result into 0. An average over
     * no rows is zero; anywhere the divisor being zero is a real error, the
     * service checks it before dividing.
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || b.signum() == 0) {
            return ZERO;
        }
        return a.divide(b, SCALE, ROUNDING);
    }

    /** A percentage of an amount: {@code percentOf(1000, 12.5)} is 125.00. */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) {
            return ZERO;
        }
        return amount.multiply(percent).divide(HUNDRED, SCALE, ROUNDING);
    }

    public static BigDecimal sum(Collection<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                total = total.add(value);
            }
        }
        return round(total);
    }

    public static BigDecimal negate(BigDecimal value) {
        return round(orZero(value).negate());
    }

    /** Clamps at zero. Change due and amount outstanding are never negative. */
    public static BigDecimal atLeastZero(BigDecimal value) {
        BigDecimal rounded = orZero(value);
        return rounded.signum() < 0 ? ZERO : rounded;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return orZero(a).min(orZero(b));
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return orZero(a).max(orZero(b));
    }

    // ------------------------------------------------------------------
    // comparison
    //
    // Always by value. equals() on BigDecimal compares the scale as well, so
    // 10.00 is not equal to 10.0 - a distinction no cashier has ever wanted.
    // ------------------------------------------------------------------

    public static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }

    public static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean eq(BigDecimal a, BigDecimal b) {
        return orZero(a).compareTo(orZero(b)) == 0;
    }

    public static boolean gt(BigDecimal a, BigDecimal b) {
        return orZero(a).compareTo(orZero(b)) > 0;
    }

    public static boolean gte(BigDecimal a, BigDecimal b) {
        return orZero(a).compareTo(orZero(b)) >= 0;
    }

    public static boolean lt(BigDecimal a, BigDecimal b) {
        return orZero(a).compareTo(orZero(b)) < 0;
    }

    // ------------------------------------------------------------------
    // the one shared rule
    // ------------------------------------------------------------------

    /**
     * A line total with a percentage discount, exactly as money.js computes it:
     * gross, then the discount off the gross.
     *
     * Phase 8 builds the rest of the bill on top of this in posMath's order -
     * line discounts first, then the bill discount on what is left, so nothing
     * is discounted twice.
     */
    public static BigDecimal lineTotal(BigDecimal quantity, BigDecimal unitPrice,
                                       BigDecimal discountPercent) {

        BigDecimal gross = multiply(orZero(quantity), orZero(unitPrice));
        return subtract(gross, percentOf(gross, orZero(discountPercent)));
    }

    // ------------------------------------------------------------------
    // display
    //
    // en-US grouping, matching the reference app's toLocaleString('en-US').
    // The shop is in Sri Lanka, which uses the same 1,234.50 convention.
    // ------------------------------------------------------------------

    /** 1234.5 becomes "1,234.50". */
    public static String format(BigDecimal value) {
        return formatter("#,##0.00").format(orZero(value));
    }

    /** 1234.5 becomes "Rs. 1,234.50". */
    public static String formatCurrency(BigDecimal value) {
        return formatCurrency(value, DEFAULT_SYMBOL);
    }

    public static String formatCurrency(BigDecimal value, String symbol) {
        return symbol + " " + format(value);
    }

    /**
     * Quantities show their decimals only when they mean something:
     * 12 stays "12", 2.5 stays "2.5".
     *
     * Most quantities are whole numbers, and a receipt column of "12.000" reads
     * as a price to anyone glancing at it.
     */
    public static String formatQty(BigDecimal value) {
        BigDecimal stripped = qty(orZeroQty(value)).stripTrailingZeros();

        if (stripped.scale() <= 0) {
            return formatter("#,##0").format(stripped);
        }
        return formatter("#,##0.###").format(stripped);
    }

    private static BigDecimal orZeroQty(BigDecimal value) {
        return value == null ? ZERO_QTY : value;
    }

    /**
     * A fresh formatter per call. DecimalFormat is not thread-safe, and reports
     * on a background thread will be formatting while the till is.
     */
    private static DecimalFormat formatter(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
    }
}
