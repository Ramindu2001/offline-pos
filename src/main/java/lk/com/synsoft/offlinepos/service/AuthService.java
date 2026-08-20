package lk.com.synsoft.offlinepos.service;

import java.util.List;

import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Authentication;
import lk.com.synsoft.offlinepos.dto.auth.ShopProfile;
import lk.com.synsoft.offlinepos.error.LoginFailedException;
import lk.com.synsoft.offlinepos.error.NotFoundException;
import lk.com.synsoft.offlinepos.error.PermissionDeniedException;
import lk.com.synsoft.offlinepos.error.ValidationException;

/**
 * Signing in, choosing a shop, and changing a password.
 *
 * Signing in is two steps because it genuinely is two: proving who you are, and
 * opening a particular shop. A user linked to three shops has to pick one, and
 * the shop and licence gates belong to that choice rather than to the password.
 *
 * <pre>{@code
 * Authentication who = auth.authenticate("cashier", password);
 * AppContext session = auth.openShop(who, who.hasSingleShop()
 *         ? who.onlyShop().id()
 *         : chosenByTheUser);
 * }</pre>
 */
public interface AuthService {

    /**
     * Step one: the user name and password, and nothing else.
     *
     * Returns who they are and which shops they may open. It grants access to
     * nothing on its own - no context exists yet, so no service will serve a
     * call.
     *
     * @param password cleared by this method before it returns, however it ends
     * @throws LoginFailedException with BAD_CREDENTIALS for a wrong name, a wrong
     *                              password or an unreadable hash; ACCOUNT_DISABLED
     *                              or ROLE_DISABLED for an account that is switched
     *                              off; NO_SHOP when nothing is linked
     */
    Authentication authenticate(String userName, char[] password) throws LoginFailedException;

    /**
     * Step two: open a shop and build the session.
     *
     * Runs the remaining gates in the legacy order - shop active, company active,
     * licence in date - loads the rights matrix, and records the session in
     * {@code userlog}. All of it in one transaction, so a till that dies part way
     * through leaves no half-open session behind.
     *
     * @throws LoginFailedException SHOP_NOT_ALLOWED if the user is not linked to
     *                              this shop, SHOP_DISABLED, LICENCE_INACTIVE or
     *                              LICENCE_EXPIRED
     */
    AppContext openShop(Authentication authentication, int shopId) throws LoginFailedException;

    /**
     * Moves an open session to another shop.
     *
     * Runs the same gates as {@link #openShop} and returns a whole new context.
     * Not an edit to the existing one: a context with one shop's rights and
     * another shop's id is exactly how defect D10 moved the wrong shop's stock,
     * and an immutable record cannot be caught half way.
     *
     * The caller must also drop anything it cached under the old shop.
     */
    AppContext switchShop(AppContext current, int shopId) throws LoginFailedException;

    /**
     * The shops this session could switch to, active ones only.
     *
     * Read fresh rather than carried in the context: a shop can be closed, or
     * the user unlinked from it, while they are signed in.
     */
    List<ShopProfile> availableShops(AppContext context);

    /** Closes the session in {@code userlog}. Safe to call more than once. */
    void signOut(AppContext context);

    /**
     * Changes the signed-in user's own password.
     *
     * The current password is required even though the user is already signed
     * in: a till is often left unlocked at a counter, and this is the one action
     * that would let a passer-by keep the account.
     *
     * @throws ValidationException if the current password is wrong, the new one
     *                             is too short, too long for bcrypt to read, or
     *                             the same as the old one
     */
    void changeOwnPassword(AppContext context, char[] currentPassword, char[] newPassword)
            throws ValidationException;

    /**
     * Sets another user's password.
     *
     * This is what "forgot password" means on a standalone till. The reference
     * app emails a reset link; with no server and no mail there is nobody to
     * send it, so an administrator sets a new one instead and the login screen
     * says so.
     *
     * Takes no context: the right to do this is read from the signed-in session
     * through {@code PermissionService}, exactly as every service from Phase 6
     * onwards will read it.
     *
     * @throws PermissionDeniedException unless the caller may edit users
     * @throws NotFoundException         if there is no such user
     * @throws ValidationException       if the new password is not acceptable
     */
    void resetPassword(int targetUserId, char[] newPassword)
            throws PermissionDeniedException, NotFoundException, ValidationException;

    /** The shortest password this application will set. Matches the reference app's rule. */
    int MINIMUM_PASSWORD_LENGTH = 6;
}
