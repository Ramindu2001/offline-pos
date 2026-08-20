package lk.com.synsoft.offlinepos.app;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.AuthenticatedUser;
import lk.com.synsoft.offlinepos.dto.auth.CompanyProfile;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.LicenceStatus;
import lk.com.synsoft.offlinepos.dto.auth.Rights;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.dto.auth.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Phase 4 gate: the sidebar shows a role exactly what that role can reach,
 * and nothing else.
 *
 * The matrix below is modelled on the reference app's mock users - an
 * administrator, a cashier, a stock keeper - plus the two shop configurations
 * that switch whole sections off.
 *
 * This is defect D15 measured rather than described. The legacy
 * {@code View/sidebar.php} was 2,701 lines because every entry carried its own
 * copy of this logic; the same question is asked here once, of a list, and can
 * therefore be checked exhaustively.
 */
class NavigationTest {

    private static final Rights VIEW = new Rights(true, false, false, false, false, false);

    /** A plain retail shop, matching what the V4 bootstrap seeds. */
    private static final Set<ShopFlag> RETAIL_SHOP = EnumSet.of(
            ShopFlag.INVENTORY, ShopFlag.CATEGORY, ShopFlag.SUPPLIERS,
            ShopFlag.EXPENSES, ShopFlag.CUSTOMERS, ShopFlag.FIXED_PRICE,
            ShopFlag.CREDIT, ShopFlag.INVOICE_PRINT);

    // ==================================================================
    // building sessions
    // ==================================================================

    private static AppContext session(UserType type, Set<ShopFlag> flags, Feature... viewable) {
        Map<Integer, Rights> rights = new HashMap<>();
        for (Feature feature : viewable) {
            rights.put(feature.id(), VIEW);
        }

        AuthenticatedUser user = new AuthenticatedUser(
                2, "someone", "", "", type, 3, "Test Role", BigDecimal.ZERO);

        ShopProfile shop = new ShopProfile(
                1, "SH_000001", "Main Shop", "", "", "Colombo", "", "",
                1, true, false, true, flags);

        CompanyProfile company = new CompanyProfile(
                1, "CM_000001", "My Company", "Sri Lanka", "OFFLINE-0001", "1.0",
                LocalDate.of(2026, 1, 1), LocalDate.of(2099, 1, 1), true, false, false);

        return new AppContext(user, shop, company,
                LicenceStatus.of(company, LocalDate.of(2026, 8, 20)), rights, 1);
    }

    private static List<String> labels(AppContext context) {
        return Navigation.visibleTo(context).stream()
                .flatMap(group -> group.items().stream())
                .map(Navigation.Item::label)
                .toList();
    }

    private static List<String> groupTitles(AppContext context) {
        return Navigation.visibleTo(context).stream()
                .map(group -> group.title() == null ? "(top)" : group.title())
                .toList();
    }

    // ==================================================================
    // the matrix
    // ==================================================================

    @Test
    @DisplayName("a cashier sees the four things a cashier does, and nothing else")
    void cashier() {
        AppContext cashier = session(UserType.NORMAL, RETAIL_SHOP,
                Feature.RETAIL_SALES, Feature.INVOICE_LIST);

        assertEquals(
                List.of("Home", "Analytics", "POS",           // open to anyone signed in
                        "Create Invoice", "Invoice List",     // RETAIL_SALES, INVOICE_LIST
                        "All Reports"),                       // open to anyone signed in
                labels(cashier));

        assertEquals(List.of("(top)", "Sales & Orders", "Reports"), groupTitles(cashier));
    }

    @Test
    @DisplayName("a stock keeper sees the store, not the till")
    void stockKeeper() {
        AppContext keeper = session(UserType.NORMAL, RETAIL_SHOP,
                Feature.STORE, Feature.GOODS_RECEIVED, Feature.PRODUCTS, Feature.SUPPLIERS);

        assertEquals(
                List.of("Home", "Analytics",
                        "Store", "GRN Entry",
                        "Products",
                        "All Reports",
                        "Suppliers"),
                labels(keeper));

        assertFalse(labels(keeper).contains("POS"));
        assertFalse(labels(keeper).contains("Create Invoice"));
    }

    @Test
    @DisplayName("an administrator sees everything the shop has switched on, Control included")
    void administrator() {
        AppContext admin = session(UserType.SUPER_ADMIN, RETAIL_SHOP);

        List<String> visible = labels(admin);

        assertTrue(visible.contains("Company"), "Control is administrators only.");
        assertTrue(visible.contains("Shops"));
        assertTrue(visible.contains("Upload Products"));
        assertTrue(visible.contains("POS"));
        assertTrue(visible.contains("Users"));

        // Switched off for this shop, so not even an administrator sees them.
        assertFalse(visible.contains("Prescriptions"), "is_prescription is off.");
        assertFalse(visible.contains("Sales Orders"), "is_quotation is off.");
        assertFalse(visible.contains("Sections"), "is_racks is off.");
        assertFalse(visible.contains("SMS Promotions"), "is_promotions is off.");
    }

    @Test
    @DisplayName("Control is hidden from an ordinary user however many rights the role holds")
    void controlIsAdminOnly() {
        // Every feature in the system, and still no Control section.
        AppContext everything = session(UserType.NORMAL, RETAIL_SHOP, Feature.values());

        List<String> visible = labels(everything);

        assertFalse(visible.contains("Company"));
        assertFalse(visible.contains("Shops"));
        assertFalse(visible.contains("System Modules"));
        assertFalse(visible.contains("Assign Users"));
        assertFalse(visible.contains("Upload Inventory"));

        // Every remaining Control entry is gated on a shop flag this shop does
        // not have, so the section has nothing left and is dropped whole.
        assertFalse(groupTitles(everything).contains("Control"));
    }

    @Test
    @DisplayName("a Control entry gated on a shop flag, not on being an administrator, follows the flag")
    void flagGatedControlEntries() {
        Set<ShopFlag> withCounters = EnumSet.copyOf(RETAIL_SHOP);
        withCounters.add(ShopFlag.COUNTER);

        AppContext cashier = session(UserType.NORMAL, withCounters, Feature.RETAIL_SALES);

        // Cash Counters carries no feature and is not administrators only: the
        // shop switch is the whole of its guard.
        assertTrue(labels(cashier).contains("Cash Counters"));
        assertFalse(labels(cashier).contains("Company"), "Company is still administrators only.");
    }

    // ==================================================================
    // shop flags switch whole sections off
    // ==================================================================

    @Test
    @DisplayName("a shop without inventory loses the Store section entirely")
    void inventoryOff() {
        Set<ShopFlag> noInventory = EnumSet.copyOf(RETAIL_SHOP);
        noInventory.remove(ShopFlag.INVENTORY);

        AppContext admin = session(UserType.SUPER_ADMIN, noInventory);

        assertFalse(groupTitles(admin).contains("Store"));
        assertFalse(labels(admin).contains("GRN Entry"));
        assertFalse(labels(admin).contains("Stock Transfer"));

        // The rest is untouched.
        assertTrue(labels(admin).contains("Products"));
    }

    @Test
    @DisplayName("a shop without credit loses the Credit & Debit section entirely")
    void creditOff() {
        Set<ShopFlag> noCredit = EnumSet.copyOf(RETAIL_SHOP);
        noCredit.remove(ShopFlag.CREDIT);

        AppContext admin = session(UserType.SUPER_ADMIN, noCredit);

        assertFalse(groupTitles(admin).contains("Credit & Debit"));
        assertFalse(labels(admin).contains("Customer Dues"));
        assertFalse(labels(admin).contains("Supplier Cheques"));
    }

    @Test
    @DisplayName("a shop with everything switched on shows every group")
    void everyFlagOn() {
        AppContext admin = session(UserType.SUPER_ADMIN, EnumSet.allOf(ShopFlag.class));

        assertEquals(
                List.of("(top)", "Sales & Orders", "Store", "Credit & Debit",
                        "Items", "Expenses", "Reports", "Master Data", "Control"),
                groupTitles(admin),
                "All nine groups from navigation.js should be present.");
    }

    // ==================================================================
    // the edges
    // ==================================================================

    @Test
    @DisplayName("a role with no rights at all still gets the screens that need none")
    void noRightsAtAll() {
        AppContext nobody = session(UserType.NORMAL, RETAIL_SHOP);

        // Home, Analytics and Reports carry no feature in navigation.js, so they
        // are open to anyone who managed to sign in.
        assertEquals(List.of("Home", "Analytics", "All Reports"), labels(nobody));
        assertEquals(List.of("(top)", "Reports"), groupTitles(nobody));
    }

    @Test
    @DisplayName("an empty group is dropped, never shown as a bare heading")
    void emptyGroupsAreDropped() {
        AppContext nobody = session(UserType.NORMAL, RETAIL_SHOP);

        for (Navigation.Group group : Navigation.visibleTo(nobody)) {
            assertFalse(group.items().isEmpty(),
                    "Group '" + group.title() + "' is showing with nothing in it.");
        }
    }

    @Test
    @DisplayName("nobody signed in sees no menu at all")
    void noSession() {
        assertEquals(List.of(), Navigation.visibleTo(null));
    }

    @Test
    @DisplayName("every visible item is a route that will actually open")
    void whatIsShownIsWhatOpens() {
        // The point of hanging the guard on the Route rather than on the menu
        // entry: there is no second rule that could disagree with this one.
        for (UserType type : UserType.values()) {
            AppContext context = session(type, EnumSet.allOf(ShopFlag.class), Feature.values());

            for (Navigation.Group group : Navigation.visibleTo(context)) {
                for (Navigation.Item item : group.items()) {
                    assertEquals(item.route(), ViewRouter.resolve(item.route(), context),
                            item.label() + " is in the menu but the router would refuse it.");
                }
            }
        }
    }
}
