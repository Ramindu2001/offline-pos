package lk.com.synsoft.offlinepos.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;

/**
 * The sidebar, as data.
 *
 * Ported group for group from the reference app's {@code constants/navigation.js}.
 * Nine groups; the first has no title and sits flat at the top.
 *
 * This is the fix for defect D15. The legacy {@code View/sidebar.php} was 2,701
 * lines of copy-pasted permission checks - every menu item repeated its own role
 * lookup, so changing a rule meant editing dozens of near-identical blocks and
 * hoping none had been missed. Here the menu is a list, the filtering happens in
 * {@link #visibleTo(AppContext)}, and it happens once.
 *
 * The items carry no guard of their own: each names a {@link Route}, and the
 * route carries the feature, the shop flag and the admin-only switch. A link is
 * therefore visible exactly when the route will open, with no second rule to
 * keep in step.
 */
public final class Navigation {

    /** One entry in the sidebar. */
    public record Item(Route route) {

        public String label() {
            return route.title();
        }
    }

    /**
     * A titled section of the sidebar.
     *
     * @param title null for the flat group at the top
     * @param flag  hides the whole group when the shop does not have the area,
     *              which is how a shop without inventory loses the Store section
     *              rather than losing its six items one at a time
     */
    public record Group(String title, ShopFlag flag, List<Item> items) {

        public Group {
            items = List.copyOf(items);
        }

        public boolean isFlat() {
            return title == null;
        }

        public Optional<ShopFlag> shopFlag() {
            return Optional.ofNullable(flag);
        }
    }

    private static final List<Group> GROUPS = List.of(

            group(null, null,
                    Route.HOME,
                    Route.ANALYTICS,
                    Route.POS),

            group("Sales & Orders", null,
                    Route.INVOICE_NEW,
                    Route.INVOICES,
                    Route.WHOLESALE,
                    Route.SALES_ORDERS,
                    Route.SALES_RETURNS,
                    Route.DELIVERY_NOTES,
                    Route.PRESCRIPTIONS),

            group("Store", ShopFlag.INVENTORY,
                    Route.INVENTORY,
                    Route.GRN,
                    Route.ADJUSTMENTS,
                    Route.TRANSFERS,
                    Route.PRICE_CHANGE,
                    Route.PRICE_CHANGE_BULK),

            group("Credit & Debit", ShopFlag.CREDIT,
                    Route.CREDIT_CUSTOMERS,
                    Route.CUSTOMER_DUES,
                    Route.CUSTOMER_CHEQUES,
                    Route.SUPPLIER_DUES,
                    Route.SUPPLIER_PAYMENT,
                    Route.SUPPLIER_CHEQUES,
                    Route.SUPPLIER_RETURNS),

            group("Items", null,
                    Route.PRODUCTS,
                    Route.CATEGORIES,
                    Route.SUBCATEGORIES,
                    Route.UNITS,
                    Route.SECTIONS,
                    Route.RACKS,
                    Route.BARCODES),

            group("Expenses", ShopFlag.EXPENSES,
                    Route.EXPENSES,
                    Route.EXPENSE_CATEGORIES,
                    Route.EXPENSE_TYPES),

            group("Reports", null,
                    Route.REPORTS),

            group("Master Data", null,
                    Route.CUSTOMERS,
                    Route.SUPPLIERS,
                    Route.SALESMEN,
                    Route.SALES_REPS,
                    Route.SALES_REP_GROUPS,
                    Route.USERS,
                    Route.USER_ROLES),

            group("Control", null,
                    Route.COMPANY,
                    Route.SHOPS,
                    Route.SYSTEM_MODULES,
                    Route.SYSTEM_FEATURES,
                    Route.SHOP_FEATURES,
                    Route.ASSIGN_USERS,
                    Route.PAYMENT_METHODS,
                    Route.SALE_SETTINGS,
                    Route.COUNTERS,
                    Route.PROMOTIONS,
                    Route.UPLOAD_PRODUCTS,
                    Route.UPLOAD_INVENTORY));

    private Navigation() {
    }

    /** The whole menu, unfiltered. Only tests and the router catalogue want this. */
    public static List<Group> all() {
        return GROUPS;
    }

    /**
     * The menu this session should see.
     *
     * A group whose shop flag is off contributes nothing at all, and a group
     * left with no visible items is dropped rather than shown empty - an empty
     * heading tells a cashier there is something there they cannot have, which
     * is worse than not mentioning it.
     */
    public static List<Group> visibleTo(AppContext context) {
        if (context == null) {
            return List.of();
        }

        List<Group> visible = new ArrayList<>(GROUPS.size());

        for (Group group : GROUPS) {
            if (group.flag() != null && !context.has(group.flag())) {
                continue;
            }

            List<Item> items = group.items().stream()
                    .filter(item -> item.route().isReachableBy(context))
                    .toList();

            if (!items.isEmpty()) {
                visible.add(new Group(group.title(), group.flag(), items));
            }
        }
        return visible;
    }

    private static Group group(String title, ShopFlag flag, Route... routes) {
        List<Item> items = new ArrayList<>(routes.length);
        for (Route route : routes) {
            items.add(new Item(route));
        }
        return new Group(title, flag, items);
    }
}
