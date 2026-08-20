package lk.com.synsoft.offlinepos.dto.auth;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * The 25 {@code is_*} switches on the {@code shop} row.
 *
 * These decide whether a whole area of the program exists for this shop at all,
 * and they are a different question from what the signed-in role may do: a shop
 * with {@code is_credit} off has no credit screens for anyone, however senior.
 * Rights answer "may you"; these answer "is there".
 *
 * Ported from the reference app's {@code constants/shopFlags.js}. The column
 * names are carried here verbatim, including the two that break the naming
 * convention - {@code is_BatchNo} and {@code invoice_print} - because they are
 * what the column is actually called.
 */
public enum ShopFlag {

    INVENTORY("is_inventory"),
    MINUS_STOCK("is_minus"),
    CATEGORY("is_category"),
    EXPIRY("is_expire"),
    VARIATION("is_variation"),
    SUPPLIERS("is_suppliers"),
    SERVICE("is_service"),
    SALESMAN("is_salesman"),
    EXPENSES("is_expenses"),
    CUSTOMERS("is_customers"),
    FIXED_PRICE("is_fixedprice"),
    CARTON("is_carton"),
    WARRANTY("is_warranty"),
    PROMOTIONS("is_promotions"),
    SECOND_LANGUAGE("is_secondlan"),
    LABEL_PRICE("is_labelprice"),
    QUOTATION("is_quotation"),
    RACKS("is_racks"),
    CREDIT("is_credit"),
    PRESCRIPTION("is_prescription"),
    COUNTER("is_counter"),
    EXCESS_AMOUNT("is_excessAmount"),
    BATCH_NO("is_BatchNo"),
    UNDER_COST("is_under_cost"),
    INVOICE_PRINT("invoice_print");

    private final String column;

    ShopFlag(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }

    public static Optional<ShopFlag> byColumn(String column) {
        return Stream.of(values()).filter(flag -> flag.column.equals(column)).findFirst();
    }
}
