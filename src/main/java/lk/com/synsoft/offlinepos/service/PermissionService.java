package lk.com.synsoft.offlinepos.service;

import lk.com.synsoft.offlinepos.dto.auth.Action;
import lk.com.synsoft.offlinepos.dto.auth.AppContext;
import lk.com.synsoft.offlinepos.dto.auth.Feature;
import lk.com.synsoft.offlinepos.dto.auth.ShopFlag;
import lk.com.synsoft.offlinepos.error.PermissionDeniedException;

/**
 * The three gates, asked one at a time.
 *
 * Every guarded service method starts with a call to {@link #require}. Not the
 * controller: the legacy app checked in the page, by writing a JavaScript
 * redirect with no {@code exit}, so the page went on rendering and its queries
 * still ran - the data was already on the wire before the browser navigated
 * away (defect D03). A check that the caller can skip by not calling it is not
 * a check.
 *
 * The same reasoning covers {@link #shopId()}. Shop scope is taken from the
 * signed-in session, never from a form field, which is what stopped defect D10
 * from being possible.
 */
public interface PermissionService {

    /**
     * Refuses unless the signed-in role holds this right.
     *
     * A super admin passes everything, as in the legacy system. Everyone else
     * needs the right explicitly: a feature with no {@code userroleaccess} row
     * is denied, because absence is not permission.
     *
     * @throws PermissionDeniedException if the right is missing
     * @throws IllegalStateException     if nobody is signed in, which is a bug
     *                                   rather than a permission problem
     */
    void require(Feature feature, Action action) throws PermissionDeniedException;

    /** As {@link #require}, for the common case of simply opening a screen. */
    void requireView(Feature feature) throws PermissionDeniedException;

    /**
     * Whether the right is held.
     *
     * For the UI to decide what to draw. Hiding a button that would be refused
     * is a courtesy to the user; it is never the protection.
     */
    boolean can(Feature feature, Action action);

    /**
     * Refuses unless this shop has the area switched on at all.
     *
     * A different question from rights: a shop with {@code is_credit} off has no
     * credit screens for anybody, however senior.
     */
    void requireShopFeature(ShopFlag flag) throws PermissionDeniedException;

    boolean shopHas(ShopFlag flag);

    /**
     * The shop every query must filter by.
     *
     * @throws IllegalStateException if nobody is signed in
     */
    int shopId();

    /** The user to stamp on a row as its author. */
    int userId();

    /** The signed-in session. */
    AppContext context();

    boolean isSignedIn();
}
