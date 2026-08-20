package lk.com.synsoft.offlinepos.dto.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The 61 permissioned features, keyed by their real {@code sysfeatures.SFID}.
 *
 * The ids are not invented and must never be renumbered: they are the primary
 * key that every {@code userroleaccess} row points at, so a shop's existing
 * rights would silently move to different screens if one changed.
 *
 * Ported from the reference app's {@code access/featureMap.js}, cross-checked
 * against the seeded {@code sysfeatures} rows. SFIDs 23-30 and 60-61 are absent
 * on purpose - those features were deleted from the legacy system - and 70 was
 * never issued.
 *
 * A service names one of these in every guarded method:
 *
 * <pre>{@code
 * permissions.require(Feature.PRODUCTS, Action.CREATE);
 * }</pre>
 */
public enum Feature {

    // --- INVENTORY ---
    STORE(1, Module.INVENTORY, "Store"),
    GOODS_RECEIVED(2, Module.INVENTORY, "Goods Received"),
    ADJUSTMENT(3, Module.INVENTORY, "Adjustment"),
    TRANSFER_NOTE(4, Module.INVENTORY, "Transfer Note"),
    SUPPLIER_RETURN(5, Module.INVENTORY, "Supplier Return"),
    PRICE_CHANGE(6, Module.INVENTORY, "Price Change"),
    SUPPLIER_DUE_PAYMENTS(66, Module.INVENTORY, "Supplier Due Payments"),

    // --- ORDERS ---
    RETAIL_SALES(7, Module.ORDERS, "Retail Sales"),
    WHOLESALE_SALES(8, Module.ORDERS, "WholeSale Sales"),
    CUSTOMER_DUE_PAYMENT(9, Module.ORDERS, "Customer Due Payment"),
    SALES_RETURN(10, Module.ORDERS, "Sales Return"),
    INVOICE_LIST(56, Module.ORDERS, "Invoice List"),
    PRESCRIPTION(68, Module.ORDERS, "Prescription"),

    // --- ACCOUNTS ---
    EXPENSE_CATEGORIES(11, Module.ACCOUNTS, "Expense Categories"),
    EXPENSE_TYPE(12, Module.ACCOUNTS, "Expense Type"),
    EXPENSES(13, Module.ACCOUNTS, "Expenses"),
    PROMOTIONS(71, Module.ACCOUNTS, "Promotions"),

    // --- REPORTS ---
    RPT_MASTER_UNIT_LIST(31, Module.REPORTS, "Master Unit List"),
    RPT_MASTER_SUB_CATEGORY_LIST(32, Module.REPORTS, "Master Sub Category List"),
    RPT_MASTER_CATEGORY_LIST(33, Module.REPORTS, "Master Category List"),
    RPT_MASTER_ITEM_LIST(34, Module.REPORTS, "Master Item List"),
    RPT_MASTER_SUPPLIER_LIST(35, Module.REPORTS, "Master Supplier List"),
    RPT_MASTER_CUSTOMER_LIST(36, Module.REPORTS, "Master Customer List"),
    RPT_MASTER_SALESMEN_LIST(37, Module.REPORTS, "Master Salesmen List"),
    RPT_INVENTORY_SUMMARY(38, Module.REPORTS, "Inventory Summary"),
    RPT_SALES_SUMMARY(39, Module.REPORTS, "Sales Summary"),
    RPT_SALESMAN_WISE_SALES(40, Module.REPORTS, "Salesman Wise Sales"),
    RPT_USER_WISE_SALES(41, Module.REPORTS, "User wise Sales"),
    RPT_ITEM_RETURN(42, Module.REPORTS, "Item Return"),
    RPT_CUSTOMER_PROFILES(43, Module.REPORTS, "Customer Profiles"),
    RPT_CREDIT_CUSTOMER(44, Module.REPORTS, "Credit Customer"),
    RPT_CUSTOMER_SALES(45, Module.REPORTS, "Customer Sales"),
    RPT_DUE_SALES(46, Module.REPORTS, "Due Sales"),
    RPT_TOP_SELLING_PRODUCT(47, Module.REPORTS, "Top Selling Product"),
    RPT_PRODUCT_VARIATIONS(48, Module.REPORTS, "Product Variations"),
    RPT_SUPPLIER_RETURN(49, Module.REPORTS, "Supplier Return"),
    RPT_SUPPLIER_PAYMENT(50, Module.REPORTS, "Supplier Payment"),
    RPT_CATEGORY_SELLING(51, Module.REPORTS, "Category Selling"),
    RPT_TRANSFER_NOTE(52, Module.REPORTS, "Transfer Note Report"),
    RPT_EXPENSE_SUMMARY(53, Module.REPORTS, "Expense Summary"),
    RPT_INVOICE_RETURN(55, Module.REPORTS, "Invoice Return"),
    RPT_PROFIT_AND_LOSS(57, Module.REPORTS, "Profit & Loss"),
    RPT_PAYMETHOD_SALE(58, Module.REPORTS, "Paymethod Sale"),
    RPT_EXPIRED_ITEMS(59, Module.REPORTS, "Expired Items"),
    RPT_SALE_Z_REPORT(62, Module.REPORTS, "Sale Z Report"),
    RPT_SUPPLIER_PURCHASE(63, Module.REPORTS, "Supplier Purchase"),
    RPT_MONTHLY_SALE(64, Module.REPORTS, "Monthly Sale"),
    RPT_INVENTORY_PRICE(65, Module.REPORTS, "Inventory Price"),
    RPT_SALESMAN_DETAILS(69, Module.REPORTS, "Salesman Details"),
    RPT_BATCH_WISE_SALE(72, Module.REPORTS, "Batch wise sale"),

    // --- SETTINGS ---
    MAIN_CATEGORY(14, Module.SETTINGS, "Main Category"),
    SUBCATEGORY(15, Module.SETTINGS, "Subcategory Category"),
    PRODUCTS(16, Module.SETTINGS, "Products"),
    SUPPLIERS(17, Module.SETTINGS, "Suppliers"),
    CUSTOMERS(18, Module.SETTINGS, "Customers"),
    SALESMAN(19, Module.SETTINGS, "Salesman"),
    ADD_USERS(20, Module.SETTINGS, "Add Users"),
    SECTION_AND_RACKS(21, Module.SETTINGS, "Section and Racks"),
    UNITS(22, Module.SETTINGS, "Units"),
    ADD_USER_ROLE(54, Module.SETTINGS, "Add User Role"),
    LABEL_PRINT(67, Module.SETTINGS, "Label Print");

    /** Every id, resolved once. A rights matrix is looked up by SFID on every check. */
    private static final Map<Integer, Feature> BY_ID = index();

    private final int id;
    private final Module module;
    private final String label;

    Feature(int id, Module module, String label) {
        this.id = id;
        this.module = module;
        this.label = label;
    }

    /** The {@code sysfeatures.SFID} this feature is stored under. */
    public int id() {
        return id;
    }

    public Module module() {
        return module;
    }

    /** The name as the role-permissions screen shows it. */
    public String label() {
        return label;
    }

    /**
     * The feature an id refers to, if this build still knows it.
     *
     * Empty rather than an exception: a database that has been through a newer
     * version of the program may hold rights rows for a feature this build has
     * never heard of, and the right answer is to ignore that row, not to refuse
     * to log the cashier in.
     */
    public static Optional<Feature> byId(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** The features of one module, in declaration order. Phase 4 builds the sidebar from this. */
    public static List<Feature> of(Module module) {
        return Stream.of(values()).filter(feature -> feature.module == module).toList();
    }

    private static Map<Integer, Feature> index() {
        Map<Integer, Feature> byId = new HashMap<>();

        for (Feature feature : values()) {
            Feature clash = byId.put(feature.id, feature);
            if (clash != null) {
                throw new IllegalStateException(
                        "Two features share SFID " + feature.id + ": " + clash + " and " + feature);
            }
        }
        return Collections.unmodifiableMap(byId);
    }
}
