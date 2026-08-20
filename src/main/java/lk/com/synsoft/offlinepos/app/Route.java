package lk.com.synsoft.offlinepos.app;

import java.util.Optional;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;

/**
 * Every screen in the application, with the conditions for reaching it.
 *
 * Ported from the reference app's {@code constants/routes.js}, with one change
 * that matters: <b>the guard is declared here and nowhere else.</b> React
 * declares it twice - once in {@code navigation.js} so the sidebar can hide the
 * link, and again in {@code routes.jsx} so the page refuses to render - and two
 * declarations of the same rule are two chances for them to disagree. Here the
 * sidebar and the router read the same three fields off the same constant, so
 * a link that is visible is a link that opens, always.
 *
 * That is the structural half of the fix for defect D15. The other half is that
 * this is data: the legacy sidebar repeated its permission check 2,701 lines
 * deep, and adding a menu item meant writing another one.
 *
 * <b>A null fxml means the screen is not built yet.</b> The route still exists,
 * still carries its guard and still appears in the sidebar for whoever may see
 * it - it simply opens a placeholder naming the phase that will fill it in.
 * Keeping the catalogue complete from the start is what lets the navigation be
 * tested against a real role now rather than after every screen lands.
 */
public enum Route {

    // ------------------------------------------------------------------
    // Auth - the only routes reachable without a session
    // ------------------------------------------------------------------

    LOGIN("Sign in", Shell.AUTH, "auth/login-view.fxml"),
    FORGOT_PASSWORD("Forgotten password", Shell.AUTH, "auth/forgot-password-view.fxml"),
    SELECT_SHOP("Choose a shop", Shell.AUTH, "auth/select-shop-view.fxml"),
    LICENCE_BLOCKED("Licence", Shell.AUTH, "auth/licence-blocked-view.fxml"),

    // ------------------------------------------------------------------
    // Always available to anyone signed in
    // ------------------------------------------------------------------

    HOME("Home", Shell.MAIN, "home-view.fxml"),
    CHANGE_PASSWORD("Change password", Shell.MAIN, "auth/change-password-view.fxml"),
    ACCESS_DENIED("Not available", Shell.MAIN, "access-denied-view.fxml"),

    /**
     * Stands in for every route a later phase builds. Never navigated to
     * directly - the router substitutes it when the requested route has no FXML.
     */
    NOT_BUILT("Coming soon", Shell.MAIN, "placeholder-view.fxml"),

    ANALYTICS("Analytics", Shell.MAIN, 13),
    REPORTS("All Reports", Shell.MAIN, 14),

    // ------------------------------------------------------------------
    // The till
    // ------------------------------------------------------------------

    POS("POS", Shell.POS, 8, Feature.RETAIL_SALES, null, false),

    // ------------------------------------------------------------------
    // Sales and orders
    // ------------------------------------------------------------------

    INVOICE_NEW("Create Invoice", Shell.MAIN, 9, Feature.RETAIL_SALES, null, false),
    INVOICES("Invoice List", Shell.MAIN, 9, Feature.INVOICE_LIST, null, false),
    WHOLESALE("Wholesale Invoice", Shell.MAIN, 9, Feature.WHOLESALE_SALES, null, false),
    SALES_ORDERS("Sales Orders", Shell.MAIN, 9, Feature.WHOLESALE_SALES, ShopFlag.QUOTATION, false),
    SALES_RETURNS("Sales Return", Shell.MAIN, 10, Feature.SALES_RETURN, null, false),
    DELIVERY_NOTES("Delivery Notes", Shell.MAIN, 9, Feature.WHOLESALE_SALES, null, false),
    PRESCRIPTIONS("Prescriptions", Shell.MAIN, 9, Feature.PRESCRIPTION, ShopFlag.PRESCRIPTION, false),

    // ------------------------------------------------------------------
    // Store
    // ------------------------------------------------------------------

    INVENTORY("Store", Shell.MAIN, 7, Feature.STORE, null, false),
    GRN("GRN Entry", Shell.MAIN, 7, Feature.GOODS_RECEIVED, null, false),
    ADJUSTMENTS("Stock Adjust", Shell.MAIN, 12, Feature.ADJUSTMENT, null, false),
    TRANSFERS("Stock Transfer", Shell.MAIN, 12, Feature.TRANSFER_NOTE, null, false),
    PRICE_CHANGE("Price Change", Shell.MAIN, 7, Feature.PRICE_CHANGE, null, false),
    PRICE_CHANGE_BULK("Bulk Price Change", Shell.MAIN, 7, Feature.PRICE_CHANGE, null, false),

    // ------------------------------------------------------------------
    // Credit and debit
    // ------------------------------------------------------------------

    CREDIT_CUSTOMERS("Credit Customers", Shell.MAIN, 11, Feature.CUSTOMER_DUE_PAYMENT, null, false),
    CUSTOMER_DUES("Customer Dues", Shell.MAIN, 11, Feature.CUSTOMER_DUE_PAYMENT, null, false),
    CUSTOMER_CHEQUES("Customer Cheques", Shell.MAIN, 11, Feature.CUSTOMER_DUE_PAYMENT, null, false),
    SUPPLIER_DUES("Supplier Dues", Shell.MAIN, 11, Feature.SUPPLIER_DUE_PAYMENTS, null, false),
    SUPPLIER_PAYMENT("Supplier Payment", Shell.MAIN, 11, Feature.SUPPLIER_DUE_PAYMENTS, null, false),
    SUPPLIER_CHEQUES("Supplier Cheques", Shell.MAIN, 11, Feature.SUPPLIER_DUE_PAYMENTS, null, false),
    SUPPLIER_RETURNS("Supplier Return", Shell.MAIN, 10, Feature.SUPPLIER_RETURN, null, false),

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    PRODUCTS("Products", Shell.MAIN, 6, Feature.PRODUCTS, null, false),
    CATEGORIES("Main Categories", Shell.MAIN, 6, Feature.MAIN_CATEGORY, ShopFlag.CATEGORY, false),
    SUBCATEGORIES("Sub Categories", Shell.MAIN, 6, Feature.SUBCATEGORY, ShopFlag.CATEGORY, false),
    UNITS("Units", Shell.MAIN, 6, Feature.UNITS, null, false),
    SECTIONS("Sections", Shell.MAIN, 6, Feature.SECTION_AND_RACKS, ShopFlag.RACKS, false),
    RACKS("Racks", Shell.MAIN, 6, Feature.SECTION_AND_RACKS, ShopFlag.RACKS, false),
    BARCODES("Barcodes", Shell.MAIN, 6, Feature.LABEL_PRINT, null, false),

    // ------------------------------------------------------------------
    // Expenses
    // ------------------------------------------------------------------

    EXPENSES("Add Expense", Shell.MAIN, 13, Feature.EXPENSES, null, false),
    EXPENSE_CATEGORIES("Expense Categories", Shell.MAIN, 13, Feature.EXPENSE_CATEGORIES, null, false),
    EXPENSE_TYPES("Expense Types", Shell.MAIN, 13, Feature.EXPENSE_TYPE, null, false),

    // ------------------------------------------------------------------
    // Master data
    // ------------------------------------------------------------------

    CUSTOMERS("Customers", Shell.MAIN, 6, Feature.CUSTOMERS, ShopFlag.CUSTOMERS, false),
    SUPPLIERS("Suppliers", Shell.MAIN, 6, Feature.SUPPLIERS, ShopFlag.SUPPLIERS, false),
    SALESMEN("Salesmen", Shell.MAIN, 6, Feature.SALESMAN, ShopFlag.SALESMAN, false),
    SALES_REPS("Sales Reps", Shell.MAIN, 6, Feature.SALESMAN, ShopFlag.SALESMAN, false),
    SALES_REP_GROUPS("Sales Rep Groups", Shell.MAIN, 6, Feature.SALESMAN, ShopFlag.SALESMAN, false),
    USERS("Users", Shell.MAIN, 6, Feature.ADD_USERS, null, false),
    USER_ROLES("User Roles", Shell.MAIN, 6, Feature.ADD_USER_ROLE, null, false),

    // ------------------------------------------------------------------
    // Control - administrator only
    // ------------------------------------------------------------------

    COMPANY("Company", Shell.MAIN, 6, null, null, true),
    SHOPS("Shops", Shell.MAIN, 6, null, null, true),
    SYSTEM_MODULES("System Modules", Shell.MAIN, 6, null, null, true),
    SYSTEM_FEATURES("System Features", Shell.MAIN, 6, null, null, true),
    SHOP_FEATURES("Shop Features", Shell.MAIN, 6, null, null, true),
    ASSIGN_USERS("Assign Users", Shell.MAIN, 6, null, null, true),
    PAYMENT_METHODS("Payment Methods", Shell.MAIN, 6, null, null, true),
    SALE_SETTINGS("Sale Settings", Shell.MAIN, 6, null, null, true),
    COUNTERS("Cash Counters", Shell.MAIN, 6, null, ShopFlag.COUNTER, false),
    PROMOTIONS("SMS Promotions", Shell.MAIN, 13, Feature.PROMOTIONS, ShopFlag.PROMOTIONS, false),
    UPLOAD_PRODUCTS("Upload Products", Shell.MAIN, 16, null, null, true),
    UPLOAD_INVENTORY("Upload Inventory", Shell.MAIN, 16, null, null, true);

    /** Where the FXML files live, relative to the classpath. */
    static final String VIEW_ROOT = "/lk/com/synsoft/offlinepos/view/";

    private final String title;
    private final Shell shell;
    private final String fxml;
    private final int phase;
    private final Feature feature;
    private final ShopFlag flag;
    private final boolean adminOnly;

    /** A screen that exists. */
    Route(String title, Shell shell, String fxml) {
        this(title, shell, fxml, 4, null, null, false);
    }

    /** A screen a later phase builds, open to anyone signed in. */
    Route(String title, Shell shell, int phase) {
        this(title, shell, null, phase, null, null, false);
    }

    /** A screen a later phase builds, behind a guard. */
    Route(String title, Shell shell, int phase, Feature feature, ShopFlag flag, boolean adminOnly) {
        this(title, shell, null, phase, feature, flag, adminOnly);
    }

    Route(String title, Shell shell, String fxml, int phase,
          Feature feature, ShopFlag flag, boolean adminOnly) {

        this.title = title;
        this.shell = shell;
        this.fxml = fxml;
        this.phase = phase;
        this.feature = feature;
        this.flag = flag;
        this.adminOnly = adminOnly;
    }

    public String title() {
        return title;
    }

    public Shell shell() {
        return shell;
    }

    /** Which phase builds this screen. Shown on the placeholder until it does. */
    public int phase() {
        return phase;
    }

    public boolean isBuilt() {
        return fxml != null;
    }

    /** The classpath location of the FXML, if the screen exists yet. */
    public Optional<String> fxml() {
        return Optional.ofNullable(fxml).map(name -> VIEW_ROOT + name);
    }

    public Optional<Feature> feature() {
        return Optional.ofNullable(feature);
    }

    public Optional<ShopFlag> flag() {
        return Optional.ofNullable(flag);
    }

    public boolean adminOnly() {
        return adminOnly;
    }

    /**
     * Whether this session may reach this route.
     *
     * The three questions in the order they get cheaper to answer wrong: is this
     * an administrator screen, does the shop have the area at all, and does the
     * role hold the right. The sidebar asks it to decide whether to draw a link;
     * the router asks it before it loads anything at all.
     *
     * This is not the protection. The service refuses the call regardless - see
     * {@code PermissionService.require}, and defect D03 for what happens when a
     * check like this one is the only one there is.
     */
    public boolean isReachableBy(AppContext context) {
        if (context == null) {
            return !shell.needsSession();
        }
        if (adminOnly && !context.isSuperAdmin()) {
            return false;
        }
        if (flag != null && !context.has(flag)) {
            return false;
        }
        return feature == null || context.canSee(feature);
    }
}
