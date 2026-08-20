package lk.com.synsoft.offlinepos.dto.auth;

import java.util.List;

/**
 * The result of a correct user name and password: who they are, and which shops
 * they are allowed to open.
 *
 * Deliberately not an {@link AppContext} yet. Nothing here grants access to
 * anything - the shop and licence gates run when a shop is actually opened, and
 * only that produces a context. Splitting the two is what makes the multi-shop
 * flow honest: the reference app's SelectShopPage sits exactly here.
 *
 * @param shops the shops linked to this user through {@code shopusers}, active
 *              ones only, in name order. May be empty, which is a refusal the
 *              caller has to handle.
 */
public record Authentication(AuthenticatedUser user, List<ShopProfile> shops) {

    public Authentication {
        shops = shops == null ? List.of() : List.copyOf(shops);
    }

    /** The common case: one shop, so the till opens straight into it. */
    public boolean hasSingleShop() {
        return shops.size() == 1;
    }

    public ShopProfile onlyShop() {
        if (!hasSingleShop()) {
            throw new IllegalStateException(
                    "This user has " + shops.size() + " shops; one has to be chosen.");
        }
        return shops.get(0);
    }
}
