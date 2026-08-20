package lk.com.synsoft.offlinepos.dto.auth;

/**
 * The six rights a role holds per feature, one to each flag column on
 * {@code userroleaccess}.
 *
 * The column name travels with the constant so the DAO reads the row without a
 * second mapping to keep in step, and so an error message can name the right
 * the way the permissions screen shows it.
 */
public enum Action {

    VIEW("is_view", "view"),
    CREATE("is_create", "add"),
    EDIT("is_edit", "change"),
    DELETE("is_delete", "delete"),
    VERIFY("is_verify", "verify"),
    PRINT("is_print", "print");

    private final String column;
    private final String verb;

    Action(String column, String verb) {
        this.column = column;
        this.verb = verb;
    }

    /** The {@code userroleaccess} column this right is stored in. */
    public String column() {
        return column;
    }

    /** How the right reads in a sentence shown to the user: "You may not add products." */
    public String verb() {
        return verb;
    }
}
