package lk.com.synsoft.offlinepos.error;

/**
 * The user could not be signed in, or the shop could not be opened.
 *
 * One exception with a reason rather than seven classes: the login screen
 * handles them all the same way - show the sentence, keep the user on the
 * screen - and the reason is what decides which sentence and what gets logged.
 *
 * <b>A wrong password and an unknown user name give the same message.</b>
 * Different wording would let anyone with the login screen in front of them
 * discover which user names exist, and a till sits on a shop counter.
 */
public class LoginFailedException extends AppException {

    public enum Reason {

        /** Wrong name, wrong password, or a hash that cannot be read. */
        BAD_CREDENTIALS("The user name or password is not correct."),

        /** The account exists but has been switched off. */
        ACCOUNT_DISABLED("This account has been disabled. Ask your administrator to switch it back on."),

        /** The role the account belongs to has been switched off or deleted. */
        ROLE_DISABLED("The role for this account is no longer active. Ask your administrator."),

        /** Nobody linked this user to a shop, so there is nothing to open. */
        NO_SHOP("This account is not linked to any shop yet. Ask your administrator."),

        /** The chosen shop is not one this user may open - or does not exist. */
        SHOP_NOT_ALLOWED("You do not have access to that shop."),

        /** Gate 2: the shop itself is closed. */
        SHOP_DISABLED("This shop has been closed. Ask your administrator."),

        /** Gate 1: the company record is switched off. */
        LICENCE_INACTIVE("This company is not active. Please contact your supplier."),

        /** Gate 1: the licence ran out. The expiry date is the last valid day. */
        LICENCE_EXPIRED("The licence has expired. Please contact your supplier to renew it.");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    private final Reason reason;

    public LoginFailedException(Reason reason, String logMessage) {
        super(reason.message(), logMessage);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    /** True when the failure is about the licence, which Phase 4 shows on its own screen. */
    public boolean isLicenceProblem() {
        return reason == Reason.LICENCE_EXPIRED || reason == Reason.LICENCE_INACTIVE;
    }
}
