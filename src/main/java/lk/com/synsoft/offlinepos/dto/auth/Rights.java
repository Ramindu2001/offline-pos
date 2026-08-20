package lk.com.synsoft.offlinepos.dto.auth;

/**
 * What one role may do with one feature: a single {@code userroleaccess} row.
 *
 * The columns are nullable in the schema, and a NULL there means the right was
 * never granted. {@link Rights#NONE} is what a feature with no row at all
 * resolves to, so a missing row and an explicitly denied row behave the same
 * way. Absence is never permission.
 */
public record Rights(
        boolean view,
        boolean create,
        boolean edit,
        boolean delete,
        boolean verify,
        boolean print) {

    /** No row, or a row with everything off. */
    public static final Rights NONE = new Rights(false, false, false, false, false, false);

    public boolean allows(Action action) {
        return switch (action) {
            case VIEW -> view;
            case CREATE -> create;
            case EDIT -> edit;
            case DELETE -> delete;
            case VERIFY -> verify;
            case PRINT -> print;
        };
    }

    /** True when the role holds no right at all here, so the menu entry can be dropped. */
    public boolean isEmpty() {
        return equals(NONE);
    }
}
