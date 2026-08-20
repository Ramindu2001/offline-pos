package lk.com.synsoft.offlinepos.dto.auth;

/**
 * The five groups in {@code sysmodules}, which are also the five sections of
 * the sidebar.
 */
public enum Module {

    INVENTORY(1, "Inventory"),
    ORDERS(2, "Orders"),
    ACCOUNTS(3, "Accounts"),
    REPORTS(4, "Reports"),
    SETTINGS(5, "Settings");

    private final int id;
    private final String label;

    Module(int id, String label) {
        this.id = id;
        this.label = label;
    }

    /** The {@code sysmodules.SMID}. */
    public int id() {
        return id;
    }

    public String label() {
        return label;
    }
}
