package lk.com.synsoft.offlinepos.dto.auth;

import java.util.Map;

/**
 * Everything the application knows about the current session: who is signed in,
 * which shop the till is open in, the company behind it, and what this user's
 * role may do.
 *
 * Built once, at login, and never changed afterwards. Choosing a different shop
 * builds a new one rather than mutating this - a half-switched context, with one
 * shop's rights and another shop's stock, is exactly how defect D10 moved the
 * wrong shop's stock.
 *
 * <b>Shop scope comes from here and from nowhere else.</b> Every query that
 * touches shop data filters on {@link #shopId()}. A shop id arriving from a form
 * field, a combo box or a request parameter is not to be trusted for this, ever.
 *
 * It lives in {@code dto} rather than {@code app} on purpose: every service
 * implementation needs it, and putting it in {@code app} would mean the service
 * layer depending on the UI layer above it. {@code app.Session} holds the
 * current one; this is only the value.
 *
 * @param sessionId the {@code userlog.ULID} opened at login, so signing out
 *                  closes this session and not another one on the same account
 *                  at a different till
 */
public record AppContext(
        AuthenticatedUser user,
        ShopProfile shop,
        CompanyProfile company,
        LicenceStatus licence,
        Map<Integer, Rights> rights,
        int sessionId) {

    public AppContext {
        rights = rights == null ? Map.of() : Map.copyOf(rights);
    }

    public int userId() {
        return user.id();
    }

    public int shopId() {
        return shop.id();
    }

    public int companyId() {
        return company.id();
    }

    public boolean isSuperAdmin() {
        return user.isSuperAdmin();
    }

    /** Whether this shop has the area at all, regardless of who is signed in. */
    public boolean has(ShopFlag flag) {
        return shop.has(flag);
    }

    /**
     * What this user may do with a feature.
     *
     * A super admin holds everything. For everyone else a missing row reads as
     * {@link Rights#NONE}: absence is never permission.
     */
    public Rights rightsFor(Feature feature) {
        if (isSuperAdmin()) {
            return new Rights(true, true, true, true, true, true);
        }
        return rights.getOrDefault(feature.id(), Rights.NONE);
    }

    /**
     * The question the UI asks to decide whether to draw a button.
     *
     * Answering it is presentation only. The service is what refuses the call -
     * see {@code PermissionService.require}. Hiding a control is a courtesy, not
     * a control (defect D03).
     */
    public boolean can(Feature feature, Action action) {
        return rightsFor(feature).allows(action);
    }

    /** True when the user can reach the feature at all, so the sidebar can drop it. */
    public boolean canSee(Feature feature) {
        return can(feature, Action.VIEW);
    }
}
