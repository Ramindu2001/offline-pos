package lk.com.synsoft.offlinepos.util;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The money rules, checked against the reference app they were ported from.
 *
 * These are the numbers a shop reconciles its till against, so the cases below
 * are deliberately the awkward ones: the halves, the thirds, the empty field a
 * cashier tabs through, and the two values that are equal but do not look it.
 */
class MoneyTest {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    @Nested
    @DisplayName("scale and rounding")
    class Rounding {

        @Test
        @DisplayName("money settles at two decimal places")
        void moneyIsScaleTwo() {
            assertEquals(2, Money.of("10").scale());
            assertEquals("10.00", Money.of("10").toPlainString());
        }

        @Test
        @DisplayName("a half rounds up, matching the reference app")
        void halfRoundsUp() {
            // money.js: Math.round((value + EPSILON) * 100) / 100
            assertEquals(d("2.35"), Money.round(d("2.345")));
            assertEquals(d("2.35"), Money.round(d("2.346")));
            assertEquals(d("2.34"), Money.round(d("2.344")));
            assertEquals(d("0.01"), Money.round(d("0.005")));
        }

        @Test
        @DisplayName("the classic 0.1 + 0.2 comes out exactly 0.30")
        void addsWithoutBinaryDrift() {
            assertEquals(d("0.30"), Money.add(d("0.1"), d("0.2")));
        }

        @Test
        @DisplayName("a hundred one-cent lines add up to exactly one rupee")
        void centsDoNotDrift() {
            BigDecimal total = Money.ZERO;
            for (int i = 0; i < 100; i++) {
                total = Money.add(total, d("0.01"));
            }
            assertEquals(d("1.00"), total);
        }

        @Test
        @DisplayName("quantities keep three decimal places, so 0.25 kg survives")
        void quantityIsScaleThree() {
            assertEquals(d("0.250"), Money.qty(d("0.25")));
            assertEquals(3, Money.qty(d("1")).scale());
        }

        @Test
        @DisplayName("a percentage can be reported to its own scale")
        void roundsToAGivenScale() {
            // grnMath reports margin to one decimal place.
            assertEquals(d("33.3"), Money.round(d("33.333"), 1));
            assertEquals(d("34"), Money.round(d("33.5"), 0));
        }
    }

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("a null amount counts as zero rather than throwing")
        void nullIsZero() {
            assertEquals(d("5.00"), Money.add(d("5"), null));
            assertEquals(d("5.00"), Money.subtract(d("5"), null));
            assertEquals(Money.ZERO, Money.multiply(null, d("5")));
        }

        @Test
        @DisplayName("a percentage of an amount")
        void percentOf() {
            assertEquals(d("125.00"), Money.percentOf(d("1000"), d("12.5")));
            assertEquals(d("0.00"), Money.percentOf(d("1000"), Money.ZERO));
        }

        @Test
        @DisplayName("dividing by zero gives zero instead of stopping the sale")
        void divideByZeroIsZero() {
            assertEquals(Money.ZERO, Money.divide(d("100"), Money.ZERO));
            assertEquals(d("33.33"), Money.divide(d("100"), d("3")));
        }

        @Test
        @DisplayName("a total is the sum of its lines")
        void sums() {
            assertEquals(d("60.75"), Money.sum(List.of(d("10.25"), d("20.50"), d("30.00"))));
            assertEquals(Money.ZERO, Money.sum(List.of()));
        }

        @Test
        @DisplayName("change due and amount outstanding never go negative")
        void clampsAtZero() {
            assertEquals(Money.ZERO, Money.atLeastZero(d("-5")));
            assertEquals(d("5.00"), Money.atLeastZero(d("5")));
        }
    }

    @Nested
    @DisplayName("comparison")
    class Comparison {

        @Test
        @DisplayName("10.00 and 10.0 are the same amount, even though equals() says otherwise")
        void comparesByValueNotScale() {
            assertNotEquals(d("10.00"), d("10.0"));      // the trap
            assertTrue(Money.eq(d("10.00"), d("10.0"))); // the fix
        }

        @Test
        @DisplayName("zero, positive and negative")
        void signs() {
            assertTrue(Money.isZero(d("0.00")));
            assertTrue(Money.isZero(null));
            assertTrue(Money.isNegative(d("-0.01")));
            assertTrue(Money.isPositive(d("0.01")));
            assertFalse(Money.isPositive(Money.ZERO));
        }

        @Test
        @DisplayName("greater and less than")
        void ordering() {
            assertTrue(Money.gt(d("10.01"), d("10.00")));
            assertTrue(Money.gte(d("10.00"), d("10")));
            assertTrue(Money.lt(d("9.99"), d("10")));
        }
    }

    @Nested
    @DisplayName("reading what a cashier typed")
    class Parsing {

        @Test
        @DisplayName("an empty or half-typed field reads as zero, never an error")
        void unreadableInputIsZero() {
            assertEquals(Money.ZERO, Money.parse(null));
            assertEquals(Money.ZERO, Money.parse(""));
            assertEquals(Money.ZERO, Money.parse("   "));
            assertEquals(Money.ZERO, Money.parse("-"));
            assertEquals(Money.ZERO, Money.parse("12abc"));
        }

        @Test
        @DisplayName("grouping commas are accepted, because the field displays them")
        void acceptsGroupedInput() {
            assertEquals(d("1234.50"), Money.parse("1,234.50"));
            assertEquals(d("1234.50"), Money.parse(" 1234.5 "));
        }

        @Test
        @DisplayName("a quantity keeps its three decimals")
        void parsesQuantity() {
            assertEquals(d("2.500"), Money.parseQty("2.5"));
            assertEquals(Money.ZERO_QTY, Money.parseQty(""));
        }
    }

    @Nested
    @DisplayName("the shared line rule")
    class Lines {

        @Test
        @DisplayName("quantity times price, with no discount")
        void plainLine() {
            assertEquals(d("31.50"), Money.lineTotal(d("3"), d("10.50"), Money.ZERO));
        }

        @Test
        @DisplayName("a percentage discount comes off the gross")
        void discountedLine() {
            assertEquals(d("180.00"), Money.lineTotal(d("2"), d("100"), d("10")));
        }

        @Test
        @DisplayName("a fractional quantity still totals to the cent")
        void fractionalQuantity() {
            assertEquals(d("62.50"), Money.lineTotal(d("0.250"), d("250"), Money.ZERO));
        }
    }

    @Nested
    @DisplayName("display")
    class Display {

        @Test
        @DisplayName("money always shows both decimals, with grouping")
        void formatsMoney() {
            assertEquals("1,234.50", Money.format(d("1234.5")));
            assertEquals("0.00", Money.format(Money.ZERO));
            assertEquals("0.00", Money.format(null));
            assertEquals("1,000,000.00", Money.format(d("1000000")));
        }

        @Test
        @DisplayName("quantities show decimals only when they mean something")
        void formatsQuantity() {
            assertEquals("12", Money.formatQty(d("12.000")));
            assertEquals("2.5", Money.formatQty(d("2.5")));
            assertEquals("0.25", Money.formatQty(d("0.250")));
            assertEquals("1,234.5", Money.formatQty(d("1234.5")));
            assertEquals("0", Money.formatQty(Money.ZERO_QTY));
        }

        @Test
        @DisplayName("currency carries the symbol the receipt prints")
        void formatsCurrency() {
            assertEquals("Rs. 1,234.50", Money.formatCurrency(d("1234.5")));
            assertEquals("$ 1,234.50", Money.formatCurrency(d("1234.5"), "$"));
        }
    }
}
