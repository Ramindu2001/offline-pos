package lk.com.synsoft.offlinepos.dto.auth;

/**
 * {@code user.UserType}.
 *
 * A super admin skips every rights check, which is how the legacy system
 * behaved and how the first account on a fresh install can set the system up
 * before any role exists. It is not a licence to skip the other two gates: a
 * super admin still needs an active shop and a valid company licence.
 */
public enum UserType {

    NORMAL(0),
    SUPER_ADMIN(1);

    private final int code;

    UserType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /**
     * Anything that is not exactly 1 is an ordinary user.
     *
     * Deliberately not an exception on an unknown value: the safe reading of a
     * number this build does not recognise is the one with fewer rights, not a
     * cashier locked out mid-shift.
     */
    public static UserType fromCode(int code) {
        return code == SUPER_ADMIN.code ? SUPER_ADMIN : NORMAL;
    }
}
